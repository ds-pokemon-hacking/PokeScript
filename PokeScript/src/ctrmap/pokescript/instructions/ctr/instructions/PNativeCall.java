package ctrmap.pokescript.instructions.ctr.instructions;

import ctrmap.scriptformats.gen6.PawnInstruction;
import ctrmap.pokescript.InboundDefinition;
import ctrmap.pokescript.OutboundDefinition;
import ctrmap.pokescript.instructions.abstractcommands.ACompiledInstruction;
import ctrmap.pokescript.types.DataType;
import java.util.ArrayList;
import java.util.List;
import static ctrmap.pokescript.instructions.ctr.instructions.PLocalCall.getFloatConversionInstructions;
import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.instructions.abstractcommands.ANativeCall;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.stdlib.util.ArraysEx;

public class PNativeCall extends ANativeCall {

	public PNativeCall(OutboundDefinition call) {
		super(call);
	}

	protected int getArgArrayBytes() {
		return getArgCount() * 4;
	}

	@Override
	public List<PawnInstruction> compile(NCompileGraph g) {
		List<PawnInstruction> r = new ArrayList<>();
		//stack is backwards, so we can conveniently insert everything at index 0
		InboundDefinition n = g.findMethod(call);
		if (n == null) {
			System.err.println("fault resolving method " + call);
		}
		int idx = g.getNativeIdx(call.name);
		PawnInstruction sysreq_n = new PawnInstruction(PawnInstruction.Commands.SYSREQ_N, idx, getArgArrayBytes());
		r.add(sysreq_n);

		for (int i = 0; i < getArgCount(); i++) {
			r.add(0, new PawnInstruction(PawnInstruction.Commands.PUSH_PRI));
			if (n.args[i].typeDef.baseType == DataType.FLOAT && InboundDefinition.possiblyDowncast(call.args[i].type) == DataType.INT) {
				if (!call.args[i].isImmediate() || call.args[i].getImmediateValue() != 0) {//don't need to cast 0 to a float
					r.addAll(0, getFloatConversionInstructions(g));
				}
			}

			r.addAll(0, CTRInstruction.compileIL(call.args[i].getCode(DataType.ANY), g));
		}

		return r;
	}

	@Override
	public List<AInstruction> getAllInstructions() {
		List<AInstruction> l = ArraysEx.asList(this);
		for (int j = 0; j < call.args.length; j++) {
			for (AInstruction i : call.args[j].getCode(DataType.ANY)) {
				l.addAll(i.getAllInstructions());
			}
		}
		return l;
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
}
