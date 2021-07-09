package ctrmap.pokescript.expr;

import ctrmap.pokescript.types.DataType;

public class And extends CmnMathOp.BitAnd {
	//the method is the same as regular and, we just restrict it to booleans for cleanliness
	public And() {
		super(null);
	}
	
	@Override
	public DataType getInputTypeRHS() {
		return DataType.BOOLEAN;
	}
	
	@Override
	public DataType getInputTypeLHS() {
		return DataType.BOOLEAN;
	}

	@Override
	public DataType getOutputType() {
		return DataType.BOOLEAN;
	}
}
