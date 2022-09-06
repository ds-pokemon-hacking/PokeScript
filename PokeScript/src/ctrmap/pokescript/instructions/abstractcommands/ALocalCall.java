package ctrmap.pokescript.instructions.abstractcommands;

import ctrmap.pokescript.InboundDefinition;
import ctrmap.pokescript.OutboundDefinition;
import ctrmap.pokescript.stage0.Modifier;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.pokescript.types.DataType;
import xstandard.util.ArraysEx;
import java.util.List;

public class ALocalCall extends AInstruction {

	public OutboundDefinition call;

	public int localsSize;

	public ALocalCall(OutboundDefinition out) {
		call = out;
	}

	public void init(NCompileGraph cg) {
		localsSize = cg.getCurrentMethod().locals.variables.size();

		InboundDefinition def = cg.findMethod(call);
		if (def != null) {
			for (int i = 0; i < call.args.length; i++) {
				if (def.args[i].requestedModifiers.contains(Modifier.FINAL)) {
					if (!call.args[i].isImmediate()) {
						cg.currentCompiledLine.throwException("Constant argument required.");
					}
				}
			}
		}
	}

	public int getArgCount() {
		return call.args.length;
	}

	@Override
	public List<AInstruction> getAllInstructions() {
		List<AInstruction> l = ArraysEx.asList(this);
		for (int j = 0; j < call.args.length; j++) {
			for (AInstruction i : call.args[j].getCode(DataType.ANY.typeDef())) {
				l.addAll(i.getAllInstructions());
			}
		}
		return l;
	}

	@Override
	public AInstructionType getType() {
		return AInstructionType.CALL_LOCAL;
	}
	
	@Override
	public String toString() {
		return "CallLocal:" + call.name;
	}
}
