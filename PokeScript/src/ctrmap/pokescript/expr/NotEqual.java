package ctrmap.pokescript.expr;

import ctrmap.pokescript.instructions.abstractcommands.AFloatOpCode;
import ctrmap.pokescript.instructions.abstractcommands.APlainOpCode;

public class NotEqual extends Equals {

	@Override
	public APlainOpCode getIntegerComparisonOpcode() {
		return APlainOpCode.NEQUAL;
	}

	@Override
	public AFloatOpCode getFloatComparisonOpcode() {
		return AFloatOpCode.VNEQUAL;
	}
}
