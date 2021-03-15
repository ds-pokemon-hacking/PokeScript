package ctrmap.pokescript.expr;

import ctrmap.pokescript.types.DataType;
import ctrmap.scriptformats.gen6.PawnInstruction;
import ctrmap.pokescript.instructions.abstractcommands.APlainOpCode;

public class Equals extends CmnCompOp{
	@Override
	public DataType getInputTypeLHS() {
		return DataType.ANY; //accepts any types
	}
	
	@Override
	public DataType getInputTypeRHS() {
		return DataType.ANY; //accepts any types
	}

	@Override
	public DataType getOutputType() {
		return DataType.BOOLEAN;
	}	

	@Override
	public APlainOpCode getSimpleComparisonCommand() {
		return APlainOpCode.EQUAL;
	}
}
