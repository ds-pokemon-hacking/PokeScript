package ctrmap.pokescript.instructions.abstractcommands;

import xstandard.util.ArraysEx;
import java.util.List;

/**
 *
 */
public class AConditionJump extends AInstruction {

	public APlainOpCode cmd;
	public String targetLabel;

	public AConditionJump(APlainOpCode cmd, String targetLabel) {
		this.cmd = cmd;
		this.targetLabel = targetLabel;
	}

	public APlainOpCode getOpCode() {
		return cmd;
	}

	@Override
	public List<AInstruction> getAllInstructions() {
		return ArraysEx.asList(this);
	}

	@Override
	public AInstructionType getType() {
		return AInstructionType.JUMP;
	}
	
	@Override
	public String toString() {
		return cmd + " => " + targetLabel;
	}
}
