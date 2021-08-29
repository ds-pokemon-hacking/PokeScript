package ctrmap.pokescript.expr;

import ctrmap.pokescript.types.DataType;
import ctrmap.pokescript.types.TypeDef;

public class Or extends CmnMathOp.BitOr{
	//the method is the same as regular or, we just restrict it to booleans for cleanliness
	public Or() {
		super(null);
	}
	
	@Override
	public TypeDef getInputTypeLHS() {
		return DataType.BOOLEAN.typeDef();
	}
	
	@Override
	public TypeDef getInputTypeRHS() {
		return DataType.BOOLEAN.typeDef();
	}

	@Override
	public TypeDef getOutputType() {
		return DataType.BOOLEAN.typeDef();
	}
}
