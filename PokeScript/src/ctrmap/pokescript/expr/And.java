package ctrmap.pokescript.expr;

import ctrmap.pokescript.types.DataType;
import ctrmap.pokescript.types.TypeDef;

public class And extends CmnMathOp.BitAnd {

	public And(boolean isDoubleOp, boolean isSetTo) {
		super(isDoubleOp, isSetTo);
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

	@Override
	public OperatorOperation getOperationType() {
		return OperatorOperation.BOOL_AND;
	}
}
