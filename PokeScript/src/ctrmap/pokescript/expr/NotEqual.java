package ctrmap.pokescript.expr;

import ctrmap.scriptformats.gen6.PawnInstruction;
import ctrmap.pokescript.instructions.abstractcommands.APlainOpCode;

public class NotEqual extends Equals{
	@Override
	public APlainOpCode getSimpleComparisonCommand() {
		return APlainOpCode.NEQUAL;
	}
}
