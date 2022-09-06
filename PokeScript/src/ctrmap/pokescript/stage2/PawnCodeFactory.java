package ctrmap.pokescript.stage2;

import ctrmap.pokescript.InboundDefinition;
import ctrmap.pokescript.data.Variable;
import ctrmap.pokescript.instructions.abstractcommands.*;
import ctrmap.pokescript.instructions.ctr.PokeScriptToPawnOpCode;
import ctrmap.pokescript.instructions.providers.floatlib.PawnFloatLib;
import ctrmap.pokescript.stage1.NCompilableMethod;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.pokescript.types.DataType;
import ctrmap.pokescript.types.TypeDef;
import ctrmap.scriptformats.gen6.PawnInstruction;
import ctrmap.scriptformats.gen6.PawnOpCode;
import xstandard.util.ArraysEx;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PawnCodeFactory extends AbstractCodeFactory<PawnInstruction> {

	protected final int cellSize;

	public PawnCodeFactory(NCompileGraph cg, int cellSize) {
		super(cg);
		this.cellSize = cellSize;
		addInstruction(PawnOpCode.HALT);
	}

	@Override
	public void addNative(InboundDefinition def) {
		String name = def.extendsBase != null ? def.extendsBase : def.getNameWithoutNamespace();
		ArraysEx.addIfNotNullOrContains(natives, name);
	}

	public void addInstruction(PawnOpCode op) {
		addInstruction(new PawnInstruction(op));
	}

	public void addInstruction(PawnOpCode op, long... args) {
		addInstruction(new PawnInstruction(op, args));
	}

	public void addLibrary(String name) {
		ArraysEx.addIfNotNullOrContains(libraries, name);
	}

	@Override
	public int getVariablePointer(Variable variable) {
		switch (variable.getLocation()) {
			case DATA:
				return usedGlobals.indexOf(variable) * cellSize; //globals can be stripped - use real index
			case STACK:
				return (variable.index + 1) * -cellSize;
			case STACK_UNDER:
				return -4 * cellSize - variable.index;
		}
		throw new RuntimeException();
	}

	@Override
	public void assembleVarSet(AAccessVariable.AWriteVariable instruction) {
		addInstruction(instruction.var.getLocation() == Variable.VarLoc.DATA ? PawnOpCode.STOR_PRI : PawnOpCode.STOR_S_PRI, getVariablePointer(instruction.var));
	}

	@Override
	public void assembleVarGet(AAccessVariable.AReadVariable instruction) {
		addInstruction(instruction.var.getLocation() == Variable.VarLoc.DATA ? PawnOpCode.LOAD_PRI : PawnOpCode.LOAD_S_PRI, getVariablePointer(instruction.var));
	}

	@Override
	public void assemblePlain(APlainInstruction instruction) {
		switch (instruction.opCode) {
			case RESIZE_STACK:
				addInstruction(PawnOpCode.STACK, instruction.args[0] * -cellSize);
				break;
			default:
				PawnOpCode simple = PokeScriptToPawnOpCode.getDirectConvOpCode(instruction.opCode);
				if (simple != null) {
					addInstruction(simple, longarr(instruction.args));
				}
				break;
		}
	}

	@Override
	public PawnInstruction assembleJump(AConditionJump instruction) {
		PawnInstruction jump = new PawnInstruction(PokeScriptToPawnOpCode.getDirectConvOpCode(instruction.getOpCode()), 0);
		addInstruction(jump);
		return jump;
	}

	@Override
	public void assembleVFP(AFloatInstruction instruction) {
		switch (instruction.opCode) {
			case VCONST_PRI:
				addInstruction(PawnOpCode.CONST_PRI, Float.floatToIntBits(instruction.args[0]));
				break;
			case VCVT_TOFLOAT:
				addVFPCallPri(PawnFloatLib._float);
				break;
			case VCVT_FROMFLOAT:
				addVFPCallPriConst(PawnFloatLib.floatround, PawnFloatLib.PawnFloatRoundMethod.TO_ZERO.ordinal()); //java rounding method
				break;
			case VADD:
				addVFPCallPriAlt(PawnFloatLib.floatadd);
				break;
			case VSUB:
				addVFPCallPriAlt(PawnFloatLib.floatsub);
				break;
			case VMULTIPLY:
				addVFPCallPriAlt(PawnFloatLib.floatmul);
				break;
			case VDIVIDE:
				addVFPCallPriAlt(PawnFloatLib.floatdiv);
				break;
			case VNEGATE:
				addInstruction(PawnOpCode.XOR, 0x80000000);
				break;
			case VMODULO:
				//floatsub(num, floatmul(float(floatround(floatdiv(num, denom), TO_ZERO)), denom))
				//The float conversions are faster than floatfract subtraction
				addInstruction(PawnOpCode.PUSH_PRI); //push for subtraction
				addVFPCallPriAlt(PawnFloatLib.floatdiv);
				addVFPCallPriConst(PawnFloatLib.floatround, PawnFloatLib.PawnFloatRoundMethod.TO_ZERO.ordinal());
				addVFPCallPri(PawnFloatLib._float);
				addVFPCallPriAlt(PawnFloatLib.floatmul); //denominator stays in alt register since start of routine
				addInstruction(PawnOpCode.MOVE_ALT); //move multiplication result to alt register
				addInstruction(PawnOpCode.PPRI); //pop number to pri register
				addVFPCallPriAlt(PawnFloatLib.floatsub); //num - mult result
				break;
			case VEQUAL:
				addVFPValueEqCmp(0);
				break;
			case VGREATER:
				addVFPValueEqCmp(1);
				break;
			case VLESS:
				addVFPValueEqCmp(-1);
				break;
			case VNEQUAL:
				addVFPZeroCmp(PawnOpCode.NEQ);
				break;
			case VGEQUAL:
				addVFPZeroCmp(PawnOpCode.GEQ);
				break;
			case VLEQUAL:
				addVFPZeroCmp(PawnOpCode.LEQ);
				break;
		}
	}

	private void addVFPValueEqCmp(int value) {
		addVFPCmp();
		addInstruction(PawnOpCode.EQ_C_PRI, value);
	}

	private void addVFPZeroCmp(PawnOpCode cmpOp) {
		addVFPCmp();
		addInstruction(PawnOpCode.ZERO_ALT);
		addInstruction(cmpOp);
	}

	private void addSysreq(InboundDefinition target, int argCount) {
		PawnInstruction sysreq_n = new PawnInstruction(PawnOpCode.SYSREQ_N, 0, cellSize * argCount);
		pendingNativeCalls.put(sysreq_n, target);
		addInstruction(sysreq_n);
	}

	private void ensureVFPFunc(InboundDefinition def) {
		addNative(def);
		addLibrary(PawnFloatLib.LIBRARY_NAME);
	}

	private void addVFPCmp() {
		addVFPCallPriAlt(PawnFloatLib.floatcmp);
	}

	private void addVFPCallPri(InboundDefinition def) {
		ensureVFPFunc(def);
		addInstruction(PawnOpCode.PUSH_PRI);
		addSysreq(def, 1);
	}

	private void addVFPCallPriConst(InboundDefinition def, int constVal) {
		ensureVFPFunc(def);
		addInstruction(PawnOpCode.PUSH_C, constVal);
		addInstruction(PawnOpCode.PUSH_PRI);
		addSysreq(def, 2);
	}

	private void addVFPCallPriAlt(InboundDefinition def) {
		ensureVFPFunc(def);
		addInstruction(PawnOpCode.PUSH_ALT);
		addInstruction(PawnOpCode.PUSH_PRI);
		addSysreq(def, 2);
	}

	@Override
	public PawnInstruction assembleLocalCall(ALocalCall instruction) {
		NCompilableMethod m = cg.getMethodByDef(instruction.call);

		for (int i = instruction.getArgCount() - 1; i >= 0; i--) {
			List<AInstruction> toCompile = instruction.call.args[i].getCode(DataType.ANY.typeDef());
			assemble(toCompile);

			//in case of floating point requirement, convert integers to floats
			if (m.def.args[i].typeDef.baseType == DataType.FLOAT && TypeDef.possiblyDowncast(instruction.call.args[i].type.baseType) == DataType.INT) {
				if (!instruction.call.args[i].isImmediate() || instruction.call.args[i].getImmediateValue() != 0) {//don't need to cast 0 to a float
					assembleVFP(cg.getPlainFloat(AFloatOpCode.VCVT_TOFLOAT));
				}
			}

			addInstruction(PawnOpCode.PUSH_PRI);
		}

		//push argument count so that the AM knows what to subtract from the stack afterwards
		PawnInstruction argCount = new PawnInstruction(PawnOpCode.PUSH_C, instruction.getArgCount() * cellSize);
		addInstruction(argCount);
		PawnInstruction callIns = new PawnInstruction(PawnOpCode.CALL, 0);
		addInstruction(callIns);
		return callIns;
	}

	@Override
	public PawnInstruction assembleNativeCall(ANativeCall instruction) {
		InboundDefinition n = cg.findMethod(instruction.call);
		if (n == null) {
			throw new RuntimeException("fault resolving method " + instruction.call + ", callhash " + System.identityHashCode(instruction.call));
		}

		PawnInstruction sysreq_n = new PawnInstruction(PawnOpCode.SYSREQ_N, 0, instruction.getArgCount() * cellSize);

		for (int i = instruction.getArgCount() - 1; i >= 0; i--) {
			if (n.args[i].typeDef.baseType == DataType.FLOAT && TypeDef.possiblyDowncast(instruction.call.args[i].type.baseType) == DataType.INT) {
				if (!instruction.call.args[i].isImmediate() || instruction.call.args[i].getImmediateValue() != 0) {//don't need to cast 0 to a float
					assembleVFP(cg.getPlainFloat(AFloatOpCode.VCVT_TOFLOAT));
				}
			}

			List<AInstruction> toCompile = instruction.call.args[i].getCode(DataType.ANY.typeDef());
			assemble(toCompile);

			addInstruction(PawnOpCode.PUSH_PRI);
		}
		addInstruction(sysreq_n);

		return sysreq_n;
	}

	@Override
	public void assembleCaseTable(ACaseTable instruction) {
		List<Map.Entry<Integer, String>> l = new ArrayList<>(instruction.targets.entrySet());
		Collections.sort(l, (Map.Entry<Integer, String> o1, Map.Entry<Integer, String> o2) -> o1.getKey() - o2.getKey());

		PawnInstruction ins = new PawnInstruction(PawnOpCode.CASETBL, new long[instruction.targets.size() * 2 + 2]);
		TempJumptable jumptable = new TempJumptable();
		jumptable.defaultCase = instruction.defaultCase;
		jumptable.cases.putAll(instruction.targets);
		pendingJumptables.put(ins, jumptable);
		addInstruction(ins);
	}

	private static long[] longarr(int[] intarr) {
		long[] out = new long[intarr.length];
		for (int i = 0; i < intarr.length; i++) {
			out[i] = intarr[i];
		}
		return out;
	}
}
