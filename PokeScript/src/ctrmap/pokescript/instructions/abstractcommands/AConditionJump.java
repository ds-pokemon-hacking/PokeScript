package ctrmap.pokescript.instructions.abstractcommands;

import ctrmap.stdlib.util.ArraysEx;
import java.util.List;

/**
 *
 */
public abstract class AConditionJump extends AInstruction {

	protected APlainOpCode cmd;
	public String targetLabel;

	public AConditionJump(APlainOpCode cmd, String targetLabel) {
		this.cmd = cmd;
		this.targetLabel = targetLabel;
	}

	@Override
	public List<AInstruction> getAllInstructions() {
		return ArraysEx.asList(this);
	}
}
