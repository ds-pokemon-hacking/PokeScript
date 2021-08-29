package ctrmap.pokescript.instructions.ctr.instructions;

import ctrmap.pokescript.InboundDefinition;
import ctrmap.pokescript.OutboundDefinition;
import ctrmap.pokescript.instructions.abstractcommands.ACompiledInstruction;
import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.types.DataType;
import java.util.ArrayList;
import java.util.List;
import static ctrmap.pokescript.instructions.ctr.instructions.PLocalCall.getFloatConversionInstructions;
import ctrmap.pokescript.instructions.abstractcommands.ANativeCall;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.scriptformats.gen6.PawnInstruction;
import ctrmap.scriptformats.gen6.PawnOpCode;

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
			throw new RuntimeException("fault resolving method " + call + ", callhash " + System.identityHashCode(call));
		}
		int idx = g.getNativeIdx(call.name);
		PawnInstruction sysreq_n = new PawnInstruction(PawnOpCode.SYSREQ_N, idx, getArgArrayBytes());
		
		int ptr = pointer;

		for (int i = getArgCount() - 1; i >= 0; i--) {
			if (n.args[i].typeDef.baseType == DataType.FLOAT && InboundDefinition.possiblyDowncast(call.args[i].type.baseType) == DataType.INT) {
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
		r.add(sysreq_n);

		return r;
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
