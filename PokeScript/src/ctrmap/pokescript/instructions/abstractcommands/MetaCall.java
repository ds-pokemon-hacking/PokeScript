package ctrmap.pokescript.instructions.abstractcommands;

import ctrmap.pokescript.OutboundDefinition;

public class MetaCall extends ALocalCall {

	public MetaCall(OutboundDefinition call) {
		super(call);
	}

	@Override
	public AInstructionType getType() {
		return AInstructionType.CALL_META;
	}
	
	@Override
	public String toString() {
		return "CallMeta:" + call.name;
	}
}
