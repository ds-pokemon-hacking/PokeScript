package ctrmap.pokescript.expr;

import ctrmap.pokescript.types.DataType;
import ctrmap.pokescript.types.TypeDef;

public class And extends CmnMathOp.BitAnd {
	//the method is the same as regular and, we just restrict it to booleans for cleanliness
	public And() {
		super(null);
	}
	
	@Override
	public TypeDef getInputTypeRHS() {
		return DataType.BOOLEAN.typeDef();
	}
	
	@Override
	public TypeDef getInputTypeLHS() {
		return DataType.BOOLEAN.typeDef();
	}

	@Override
	public TypeDef getOutputType() {
		return DataType.BOOLEAN.typeDef();
	}
}
