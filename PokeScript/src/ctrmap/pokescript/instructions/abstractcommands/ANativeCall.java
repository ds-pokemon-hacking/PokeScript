package ctrmap.pokescript.instructions.abstractcommands;

import ctrmap.pokescript.OutboundDefinition;

/**
 *
 */
public class ANativeCall extends ALocalCall {

	public ANativeCall(OutboundDefinition call) {
		super(call);
	}

	@Override
	public AInstructionType getType() {
		return AInstructionType.CALL_NATIVE;
	}
	
	@Override
	public String toString() {
		return "CallNative:" + call.name;
	}
}
