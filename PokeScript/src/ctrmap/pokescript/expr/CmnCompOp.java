package ctrmap.pokescript.expr;

import ctrmap.pokescript.instructions.abstractcommands.AFloatOpCode;
import ctrmap.pokescript.types.DataType;
import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.instructions.abstractcommands.APlainOpCode;
import ctrmap.pokescript.stage0.EffectiveLine;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.pokescript.types.TypeDef;
import java.util.ArrayList;
import java.util.List;

public abstract class CmnCompOp extends NumerableOperator {

	public abstract APlainOpCode getIntegerComparisonOpcode();
	public abstract AFloatOpCode getFloatComparisonOpcode();

	@Override
	public TypeDef getInputTypeLHS() {
		return DataType.FLOAT.typeDef();
	}

	@Override
	public TypeDef getInputTypeRHS() {
		return DataType.FLOAT.typeDef();
	}

	@Override
	public TypeDef getOutputType() {
		return DataType.BOOLEAN.typeDef();
	}

	@Override
	public Priority getPriority() {
		return Priority.COMPARE;
	}

	@Override
	public List<AInstruction> getOperation(EffectiveLine line, NCompileGraph cg, Throughput... inputs) {
		List<AInstruction> l = new ArrayList<>();
		createCommonCmpOpSequence(
			l,
			(inputs.length < 1 || inputs[0] == null) ? null : inputs[0].getCode(getInputTypeLHS()),
			(inputs.length < 2 || inputs[1] == null) ? null : inputs[1].getCode(getInputTypeRHS()),
			cg
		);
		l.add(useFloatingOps() ? cg.getPlainFloat(getFloatComparisonOpcode()) : cg.getPlain(getIntegerComparisonOpcode()));
		return l;
	}

	public void createCommonCmpOpSequence(List<AInstruction> l, List<AInstruction> leftSource, List<AInstruction> rightSource, NCompileGraph cg) {
		if (leftSource != null) {
			l.addAll(leftSource);
			addFloatCvtIfNeeded(l, cg);
		}
		//left rsl now in PRI
		l.add(cg.getPlain(APlainOpCode.PUSH_PRI));
		if (rightSource != null) {
			l.addAll(rightSource);
			addFloatCvtIfNeeded(l, cg);
		}
		//right rsl now in PRI
		l.add(cg.getPlain(APlainOpCode.MOVE_PRI_TO_ALT));
		//right rsl now in ALT
		l.add(cg.getPlain(APlainOpCode.POP_PRI));
		//left rsl, popped from stack, now in PRI
		//PRI = Lrsl, ALT = Rrsl
	}

	public static class Greater extends CmnCompOp {

		@Override
		public APlainOpCode getIntegerComparisonOpcode() {
			return APlainOpCode.GREATER;
		}
		
		@Override
		public AFloatOpCode getFloatComparisonOpcode() {
			return AFloatOpCode.VGREATER;
		}

		@Override
		public OperatorOperation getOperationType() {
			return OperatorOperation.GREATER;
		}
	}

	public static class GreaterOrEqual extends CmnCompOp {

		@Override
		public APlainOpCode getIntegerComparisonOpcode() {
			return APlainOpCode.GEQUAL;
		}
		
		@Override
		public AFloatOpCode getFloatComparisonOpcode() {
			return AFloatOpCode.VGEQUAL;
		}

		@Override
		public OperatorOperation getOperationType() {
			return OperatorOperation.GEQUAL;
		}
	}

	public static class Less extends CmnCompOp {

		@Override
		public APlainOpCode getIntegerComparisonOpcode() {
			return APlainOpCode.LESS;
		}
		
		@Override
		public AFloatOpCode getFloatComparisonOpcode() {
			return AFloatOpCode.VLESS;
		}

		@Override
		public OperatorOperation getOperationType() {
			return OperatorOperation.LESS;
		}
	}

	public static class LessOrEqual extends CmnCompOp {

		@Override
		public APlainOpCode getIntegerComparisonOpcode() {
			return APlainOpCode.LEQUAL;
		}
		
		@Override
		public AFloatOpCode getFloatComparisonOpcode() {
			return AFloatOpCode.VLEQUAL;
		}

		@Override
		public OperatorOperation getOperationType() {
			return OperatorOperation.LEQUAL;
		}
	}
}
