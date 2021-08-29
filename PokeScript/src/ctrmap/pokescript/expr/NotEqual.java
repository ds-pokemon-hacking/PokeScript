package ctrmap.pokescript.expr;

import ctrmap.pokescript.instructions.abstractcommands.APlainOpCode;

public class NotEqual extends Equals{
	@Override
	public APlainOpCode getSimpleComparisonCommand() {
		return APlainOpCode.NEQUAL;
	}
}
