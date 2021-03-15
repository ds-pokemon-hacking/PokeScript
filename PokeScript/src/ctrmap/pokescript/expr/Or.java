package ctrmap.pokescript.expr;

import ctrmap.pokescript.types.DataType;

public class Or extends CmnMathOp.BitOr{
	//the method is the same as regular or, we just restrict it to booleans for cleanliness
	public Or() {
		super(null);
	}
	
	@Override
	public DataType getInputTypeLHS() {
		return DataType.BOOLEAN;
	}
	
	@Override
	public DataType getInputTypeRHS() {
		return DataType.BOOLEAN;
	}

	@Override
	public DataType getOutputType() {
		return DataType.BOOLEAN;
	}
}
