package ctrmap.pokescript.stage2;

import ctrmap.pokescript.InboundDefinition;
import ctrmap.pokescript.OutboundDefinition;
import ctrmap.pokescript.data.DataGraph;
import ctrmap.pokescript.data.Variable;
import ctrmap.pokescript.expr.Throughput;
import ctrmap.pokescript.instructions.abstractcommands.AAccessVariable;
import ctrmap.pokescript.instructions.abstractcommands.ACaseTable;
import ctrmap.pokescript.instructions.abstractcommands.AConditionJump;
import ctrmap.pokescript.instructions.abstractcommands.AFloatInstruction;
import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.instructions.abstractcommands.ALocalCall;
import ctrmap.pokescript.instructions.abstractcommands.ANativeCall;
import ctrmap.pokescript.instructions.abstractcommands.APlainInstruction;
import ctrmap.pokescript.instructions.abstractcommands.APlainOpCode;
import ctrmap.pokescript.instructions.gen5.VCmpResultRequest;
import ctrmap.pokescript.instructions.gen5.VOpCode;
import ctrmap.pokescript.instructions.ntr.NTRInstructionCall;
import ctrmap.pokescript.stage1.NCompileGraph;
import static ctrmap.pokescript.instructions.gen5.VConstants.*;
import static ctrmap.pokescript.instructions.gen5.VOpCode.*;
import ctrmap.pokescript.instructions.gen5.VStackCmpOpRequest;
import ctrmap.pokescript.instructions.ntr.NTRInstructionConstructor;
import ctrmap.pokescript.stage0.Modifier;
import ctrmap.pokescript.stage1.NCompilableMethod;
import ctrmap.pokescript.types.DataType;
import java.util.List;
import java.util.Map;

public class VCodeFactory extends AbstractCodeFactory<NTRInstructionCall> {

	public VCodeFactory(NCompileGraph cg) {
		super(cg);
	}

	@Override
	public void addNative(InboundDefinition def) {
		//Nothing. Natives have fixed opcodes.
	}

	public NTRInstructionCall addInstruction(VOpCode opcode, int... args) {
		return addInstruction(opcode.createCall(args));
	}

	@Override
	public int getVariablePointer(Variable var) {
		switch (var.getLocation()) {
			case DATA:
				return VAR_TOP_GLOBAL - usedGlobals.indexOf(var) - 1;
			case STACK:
				return VAR_START_LOCAL + countUnderStackVarCount(var.parent) + var.index;
			case STACK_UNDER:
				return VAR_START_LOCAL + var.index;
		}
		return 0;
	}

	private int countUnderStackVarCount(DataGraph data) {
		int count = 0;
		for (Variable var : data) {
			if (var.getLocation() == Variable.VarLoc.STACK_UNDER) {
				count++;
			}
		}
		return count;
	}

	@Override
	public void assembleVarSet(AAccessVariable.AWriteVariable instruction) {
		addInstruction(VarUpdateVar, getVariablePointer(instruction.var), GP_REG_PRI);
	}

	@Override
	public void assembleVarGet(AAccessVariable.AReadVariable instruction) {
		addInstruction(VarUpdateVar, GP_REG_PRI, getVariablePointer(instruction.var));
	}

	private void assembleVarArithmetic(VOpCode opcode) {
		addInstruction(opcode, GP_REG_PRI, GP_REG_ALT);
	}

	private void assembleCmp(int cmpCond) {
		addInstruction(PushVar, GP_REG_PRI);
		addInstruction(PushVar, GP_REG_ALT);
		addInstruction(CmpPriAlt, cmpCond);
		addInstruction(PopToVar, GP_REG_PRI);
	}

	@Override
	public void assemblePlain(APlainInstruction instruction) {
		switch (instruction.opCode) {
			case ABORT_EXECUTION:
				addInstruction(Halt);
				break;
			case ADD:
				assembleVarArithmetic(VarUpdateAdd);
				break;
			case SUBTRACT:
				assembleVarArithmetic(VarUpdateSub);
				break;
			case AND:
				assembleVarArithmetic(VarUpdateAND);
				break;
			case OR:
				assembleVarArithmetic(VarUpdateOR);
				break;
			case DIVIDE:
				assembleVarArithmetic(VarUpdateDiv);
				break;
			case MULTIPLY:
				assembleVarArithmetic(VarUpdateMul);
				break;
			case MODULO:
				assembleVarArithmetic(VarUpdateMod);
				//PokeScript is based on Pawn which takes advantage of many architectures simultaneous divmod operation, meaning the result is expected in the alt register
				//This will move the result into ALT, but the subsequent movement back into PRI should be optimized out along with it
				addInstruction(VarUpdateVar, GP_REG_PRI, GP_REG_ALT); //mov r0, r1
				break;
			case BEGIN_METHOD:
				//Not needed
				break;
			case RESIZE_STACK:
				//Stack is fixed size in Gen V
				break;
			case RETURN:
				addInstruction(Return);
				break;
			case CONST_ALT:
				addInstruction(VarUpdateConst, GP_REG_ALT, instruction.args[0]);
				break;
			case CONST_PRI:
				addInstruction(VarUpdateConst, GP_REG_PRI, instruction.args[0]);
				break;
			case ZERO_ALT:
				addInstruction(VarUpdateConst, GP_REG_ALT, 0);
				break;
			case ZERO_PRI:
				addInstruction(VarUpdateConst, GP_REG_PRI, 0);
				break;
			case PUSH_PRI:
				addInstruction(PushVar, GP_REG_PRI);
				break;
			case PUSH_ALT:
				addInstruction(PushVar, GP_REG_ALT);
				break;
			case POP_PRI:
				addInstruction(PopToVar, GP_REG_PRI);
				break;
			case POP_ALT:
				addInstruction(PopToVar, GP_REG_ALT);
				break;
			case NEGATE:
				//All operations are unsigned, so we have to do this manually
				addInstruction(VarUpdateVar, GP_REG_ALT, GP_REG_PRI); //ALT = PRI
				addInstruction(VarUpdateConst, GP_REG_PRI, 0);		  //PRI = 0
				addInstruction(VarUpdateSub, GP_REG_PRI, GP_REG_ALT); //PRI = PRI - ALT
				//Be aware though that the result will still be mostly treated as unsigned
				break;
			case MOVE_ALT_TO_PRI:
				addInstruction(VarUpdateVar, GP_REG_PRI, GP_REG_ALT);
				break;
			case MOVE_PRI_TO_ALT:
				addInstruction(VarUpdateVar, GP_REG_ALT, GP_REG_PRI);
				break;
			case NOT:
				addNot();
				break;
			case JUMP:
			case SWITCH:
				addInstruction(Jump, 0);
				break;
			case JUMP_IF_ZERO:
				addInstruction(CmpVarConst, GP_REG_PRI, 0);
				addInstruction(JumpIf, VCmpResultRequest.EQUAL, 0);
				break;
			case EQUAL:
				//For whatever reason, the value that the stack is set to on success is 0, not 1. For most ops, we can just invert the condition.
				assembleCmp(VStackCmpOpRequest.NEQUAL);
				break;
			case GEQUAL:
				assembleCmp(VStackCmpOpRequest.LESS);
				break;
			case GREATER:
				assembleCmp(VStackCmpOpRequest.LEQUAL);
				break;
			case LEQUAL:
				assembleCmp(VStackCmpOpRequest.GREATER);
				break;
			case NEQUAL:
				assembleCmp(VStackCmpOpRequest.EQUAL);
				break;
			case LESS:
				assembleCmp(VStackCmpOpRequest.GEQUAL);
				break;
			case XOR:
				//Only boolean XOR is supported
				// -> ALT=0 : return PRI
				// -> ALT=1 : return !PRI
				String xorKeepLabel = System.identityHashCode(instruction) + "_XOR_KEEP";
				addInstruction(CmpVarConst, GP_REG_ALT, 0);
				addJumpInstruction(JumpIf.createCall(VCmpResultRequest.EQUAL, 0), xorKeepLabel); //If ALT == 0, skip to xorKeepLabel
				addNot();	//If skipping failed, invert PRI
				addLabel(xorKeepLabel);	//Prepare xorKeepLabel for skip
				break;
			case SHL:
				add2PowAltToReg3(instruction);
				addInstruction(VarUpdateMul, GP_REG_PRI, GP_REG_3);
				break;
			case SHR:
				add2PowAltToReg3(instruction);
				addInstruction(VarUpdateDiv, GP_REG_PRI, GP_REG_3);
				break;
			case SHL_C:
				addInstruction(VarUpdateMul, GP_REG_PRI, (1 << instruction.getArgument(0)));
				break;
			case SHR_C:
				addInstruction(VarUpdateDiv, GP_REG_PRI, (1 << instruction.getArgument(0)));
				break;
		}
	}

	private void add2PowAltToReg3(APlainInstruction instruction) {
		//Doing exponentiation by squaring here would not improve performance
		//much as it has a lot of division stuff in it and the maximum power here is only 15.
		String shlBreakLabel = System.identityHashCode(instruction) + "_POW2_BREAK";
		String shlLoopLabel = System.identityHashCode(instruction) + "_POW2_LOOP";
		addInstruction(VarUpdateConst, GP_REG_3, 1);
		addInstruction(VarUpdateConst, GP_REG_4, 0);
		addLabel(shlLoopLabel);
		addInstruction(CmpVarVar, GP_REG_4, GP_REG_ALT);
		addJumpInstruction(JumpIf.createCall(VCmpResultRequest.GEQUAL), shlLoopLabel);
		addInstruction(VarUpdateMul, GP_REG_3, 2);
		addInstruction(VarUpdateAdd, GP_REG_4, 1);
		addLabel(shlBreakLabel);
	}

	private void addNot() {
		String ihc = String.valueOf(System.identityHashCode(addInstruction(CmpVarConst, GP_REG_PRI, 0)));
		addJumpInstruction(JumpIf.createCall(VCmpResultRequest.EQUAL, 0), ihc + "_NOT_SETONE");
		addInstruction(VarUpdateConst, GP_REG_PRI, 0);
		addJumpInstruction(Jump.createCall(0), ihc + "_NOT_END");
		addLabel(ihc + "_NOT_SETONE");
		addInstruction(VarUpdateConst, GP_REG_PRI, 1);
		addLabel(ihc + "_NOT_END");
	}

	@Override
	public NTRInstructionCall assembleJump(AConditionJump instruction) {
		NTRInstructionCall jump = null;

		switch (instruction.getOpCode()) {
			case JUMP:
			case SWITCH:
				jump = Jump.createCall(0);
				break;
			case JUMP_IF_ZERO:
				addInstruction(CmpVarConst, GP_REG_PRI, 0);
				jump = JumpIf.createCall(VCmpResultRequest.EQUAL, 0);
				break;
		}
		if (jump != null) {
			addInstruction(jump);
		}

		return jump;
	}

	private static final float FXVFP_SCALE = 4096f;
	private static final int FXVFP_SCALE_I = 4096;

	@Override
	public void assembleVFP(AFloatInstruction instruction) {
		switch (instruction.opCode) {
			case VCONST_PRI:
				assemblePlain(cg.getPlain(APlainOpCode.CONST_PRI, (int) (instruction.args[0] * FXVFP_SCALE)));
				break;
			case VCVT_TOFLOAT:
				addInstruction(VarUpdateMul, GP_REG_PRI, FXVFP_SCALE_I);
				break;
			case VCVT_FROMFLOAT:
				addInstruction(VarUpdateDiv, GP_REG_PRI, FXVFP_SCALE_I);
				break;
			default:
				//FX operations are fixed points, so their integer equivalents apply
				//However, the unsigned operations can and WILL mess up negative comparisons and operations, so keep that in mind
				assemblePlain(cg.getPlain(instruction.opCode.integerEquivalent));
				break;
		}
	}

	@Override
	public NTRInstructionCall assembleLocalCall(ALocalCall instruction) {
		compilePreCall(this, instruction, cg, VAR_START_LOCAL);
		NTRInstructionCall call = addInstruction(Call, 0);
		compilePostCall(this, instruction);
		return call;
	}

	public static void compilePreCall(AbstractCodeFactory<NTRInstructionCall> factory, ALocalCall call, NCompileGraph g, int argVarStart) {
		//First, push the currently used variables to the stack
		//This could potentially result in bugs if any of the vars were changed in the args creation. Let's just hope noone does that (using things like i++ etc.)

		//Push all locals
		for (int i = 0; i < call.localsSize; i++) {
			factory.addInstruction(PushVar.createCall(VAR_START_LOCAL + i));
		}

		//Compile the input and store it onto the stack
		for (int i = 0; i < call.getArgCount(); i++) {
			List<AInstruction> toCompile = call.call.args[i].getCode(DataType.ANY.typeDef());
			factory.assemble(toCompile);

			//the result is now in the primary GPR - set it to a temp local
			//The params HAVE to be set to new variables to avoid scenarios like MOV R0, R1; MOV R1, R0 where R0 would get changed sooner than R1 is set
			//Since variables start at 0 in each method whereas the stack fills up with call depth, it's wiser to use variables instead of the stack
			//as it's improbable that one method will have enough vars to overflow
			factory.addInstruction(VarUpdateVar.createCall(VAR_START_LOCAL + call.localsSize + i, GP_REG_PRI));
		}
		for (int i = 0; i < call.getArgCount(); i++) {
			factory.addInstruction(VarUpdateVar.createCall(argVarStart + i, VAR_START_LOCAL + call.localsSize + i));
		}
	}

	public static void compilePostCall(AbstractCodeFactory<NTRInstructionCall> factory, ALocalCall call) {
		//Pop back the variables' original values
		for (int i = call.localsSize - 1; i >= 0; i--) {
			factory.addInstruction(PopToVar.createCall(VAR_START_LOCAL + i));
		}
	}

	private static int getImmediateValue(Throughput tp) {
		int value = tp.getImmediateValue();
		if (tp.type.baseType == DataType.FLOAT) {
			value = (int) (Float.intBitsToFloat(value) * FXVFP_SCALE);
		}
		return value;
	}

	@Override
	public NTRInstructionCall assembleNativeCall(ANativeCall instruction) {
		//This essentially compiles into a NTRInstructionCall with the given opCode
		//I think it's better to forbid calling the internal functions defined in VOpCode and rather use ones provided by the CG
		int localsCount = instruction.localsSize;
		int usedExtraLocalsNum = 0;

		OutboundDefinition call = instruction.call;
		NCompilableMethod def = cg.getMethodByDef(call);

		int[] nativeArgs = new int[instruction.getArgCount()];

		//Compile the input and store it onto the stack
		for (int i = 0; i < nativeArgs.length; i++) {
			int arg;

			if (instruction.call.args[i].isImmediate()) {
				arg = getImmediateValue(call.args[i]);

				if (isHighWk(arg) && !def.def.args[i].requestedModifiers.contains(Modifier.FINAL)) {
					//if the value is a constant that is within a variable range and the method accepts variables,
					//it has to be copied into a temporary variable so that it does not get dereferenced and is used as a proper constant
					int destVar = VAR_START_LOCAL + localsCount + usedExtraLocalsNum;
					addInstruction(VarUpdateConst, destVar, arg);
					arg = destVar;
					usedExtraLocalsNum++;
				}
			} else if (call.args[i].isVariable()) {
				Variable var = call.args[i].getVariable();
				arg = getVariablePointer(var);
			} else {
				List<AInstruction> toCompile = call.args[i].getCode(DataType.ANY.typeDef());
				assemble(toCompile);

				//the result is now in the primary GPR
				int destVar = VAR_START_LOCAL + localsCount + usedExtraLocalsNum;
				addInstruction(VarUpdateVar, destVar, GP_REG_PRI);
				arg = destVar;
				usedExtraLocalsNum++;
			}

			nativeArgs[i] = arg;
		}

		NTRInstructionCall natvCall = new NTRInstructionCall(NTRInstructionConstructor.constructFromMethodHeader(cg, def.def), nativeArgs);
		addInstruction(natvCall);
		return natvCall;
	}

	@Override
	public void assembleCaseTable(ACaseTable instruction
	) {
		/*
		Since Gen V does not have proper switch/case function, it can be emulated with if/else if...
		This is very inefficient, but a sufficient fallback for this functionality.
		The structure is as follows:

		for each case in the table, insert:
		CmpVarConst(GP_REG_PRI, caseValue);
		JumpIf(VCmpResultRequest.EQUAL, caseJumpLabel);

		then the default case
		Jump(defaultJumpLabel)
	
		 */
		int caseIdx = 0;
		for (Map.Entry<Integer, String> caseEntry : instruction.targets.entrySet()) {
			addInstruction(CmpVarConst, GP_REG_PRI, caseEntry.getKey());
			addJumpInstruction(JumpIf.createCall(VCmpResultRequest.EQUAL, 0), caseEntry.getValue());
			caseIdx++;
		}
		addJumpInstruction(Jump.createCall(0), instruction.defaultCase);
	}
}
