package ctrmap.pokescript.expr;

import ctrmap.pokescript.types.DataType;
import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.instructions.abstractcommands.APlainOpCode;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.pokescript.types.TypeDef;
import java.util.ArrayList;
import java.util.List;

public abstract class CmnCompOp extends Operator {
	
	public abstract APlainOpCode getSimpleComparisonCommand();
	
	@Override
	public TypeDef getInputTypeLHS() {
		return DataType.INT.typeDef();
	}
	
	@Override
	public TypeDef getInputTypeRHS() {
		return DataType.INT.typeDef();
	}

	@Override
	public TypeDef getOutputType() {
		return DataType.BOOLEAN.typeDef();
	}
	
	@Override
	public Priority getPriority(){
		return Priority.COMPARE;
	}
	
	@Override
	public List<AInstruction> createOperation(NCompileGraph cg){
		List<AInstruction> l = new ArrayList<>();
		
		//just compare pri to alt, really
		l.add(cg.getPlain(getSimpleComparisonCommand()));
		//aaaand result is in PRI. that's really all there is to it.
		return l;
	}

	public static class Greater extends CmnCompOp {

		@Override
		public APlainOpCode getSimpleComparisonCommand() {
			return APlainOpCode.GREATER;
		}
	}

	public static class GreaterOrEqual extends CmnCompOp {

		@Override
		public APlainOpCode getSimpleComparisonCommand() {
			return APlainOpCode.GEQUAL;
		}
	}

	public static class Less extends CmnCompOp {

		@Override
		public APlainOpCode getSimpleComparisonCommand() {
			return APlainOpCode.LESS;
		}
	}

	public static class LessOrEqual extends CmnCompOp {

		@Override
		public APlainOpCode getSimpleComparisonCommand() {
			return APlainOpCode.LEQUAL;
		}
	}
}
