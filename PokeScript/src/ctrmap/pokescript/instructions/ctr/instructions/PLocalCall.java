package ctrmap.pokescript.instructions.ctr.instructions;

import ctrmap.pokescript.instructions.providers.floatlib.PawnFloatLib;
import ctrmap.pokescript.OutboundDefinition;
import ctrmap.pokescript.instructions.abstractcommands.ACompiledInstruction;
import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.types.DataType;
import java.util.ArrayList;
import java.util.List;
import ctrmap.pokescript.instructions.abstractcommands.ALocalCall;
import ctrmap.pokescript.stage1.NCompilableMethod;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.scriptformats.gen6.PawnInstruction;
import ctrmap.scriptformats.gen6.PawnOpCode;

public class PLocalCall extends ALocalCall {

	public PLocalCall(OutboundDefinition def) {
		super(def);
	}

	@Override
	public int getAllocatedPointerSpace(NCompileGraph cg) {
		//call = 8
		//args = nArgs * 4
		int ptr = 0;
		List<? extends ACompiledInstruction> precomp = compile(cg);
		for (ACompiledInstruction i : precomp) {
			ptr += i.getSize();
		}
		return ptr;
	}

	protected int getArgArrayBytes() {
		return getArgCount() * 4;
	}

	@Override
	public List<PawnInstruction> compile(NCompileGraph g) {
		List<PawnInstruction> r = new ArrayList<>();

		NCompilableMethod m = g.getMethodByDef(call);

		int ptr = pointer;

		for (int i = getArgCount() - 1; i >= 0; i--) {
			//in case of floating point requirement, convert integers to floats
			if (m.def.args[i].typeDef.baseType == DataType.FLOAT && call.args[i].type.baseType == DataType.INT) {
				if (!call.args[i].isImmediate() || call.args[i].getImmediateValue() != 0) {//don't need to cast 0 to a float
					List<PawnInstruction> fconv = getFloatConversionInstructions(g);
					for (PawnInstruction ins : fconv) {
						ins.pointer = ptr;
						ptr += ins.getSize();
					}
					r.addAll(fconv);
				}
			}

			List<AInstruction> toCompile = call.args[i].getCode(DataType.ANY.typeDef());
			for (AInstruction tc : toCompile) {
				tc.pointer = ptr;
				ptr += tc.getAllocatedPointerSpace(g);
			}

			r.addAll(CTRInstruction.compileIL(toCompile, g));
			r.add(new PawnInstruction(PawnOpCode.PUSH_PRI));
			ptr += 4;
		}

		//push argument count so that the AM knows what to subtract from the stack afterwards
		PawnInstruction argCount = new PawnInstruction(PawnOpCode.PUSH_C, getArgArrayBytes());
		r.add(argCount);
		ptr += 4;

		PawnInstruction callIns = new PawnInstruction(PawnOpCode.CALL, m.getPointer() - ptr);
		r.add(callIns);

		return r;
	}

	protected static List<PawnInstruction> getFloatConversionInstructions(NCompileGraph g) {
		List<PawnInstruction> r = new ArrayList<>();
		g.addNative(PawnFloatLib._float);
		g.addLibrary(PawnFloatLib.LIBRARY_NAME);
		r.add(new PawnInstruction(PawnOpCode.PUSH_PRI)); //pushes the integer
		r.add(new PawnInstruction(PawnOpCode.SYSREQ_N, g.getNativeIdx(PawnFloatLib._float.name), 4)); //calls the sysreq with the argument pushed here vvv
		//the float itself gets pushed by the push_pri at the end of the method
		return r;
	}
}
