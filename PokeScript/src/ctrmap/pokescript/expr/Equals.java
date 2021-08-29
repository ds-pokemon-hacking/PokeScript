package ctrmap.pokescript.expr;

import ctrmap.pokescript.types.DataType;
import ctrmap.pokescript.instructions.abstractcommands.APlainOpCode;
import ctrmap.pokescript.types.TypeDef;

public class Equals extends CmnCompOp{
	@Override
	public TypeDef getInputTypeLHS() {
		return DataType.ANY.typeDef(); //accepts any types
	}
	
	@Override
	public TypeDef getInputTypeRHS() {
		return DataType.ANY.typeDef(); //accepts any types
	}

	@Override
	public TypeDef getOutputType() {
		return DataType.BOOLEAN.typeDef();
	}	

	@Override
	public APlainOpCode getSimpleComparisonCommand() {
		return APlainOpCode.EQUAL;
	}
}
