package ctrmap.pokescript.expr;

import ctrmap.pokescript.types.DataType;
import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.instructions.abstractcommands.APlainOpCode;
import ctrmap.pokescript.stage0.EffectiveLine;
import ctrmap.pokescript.stage1.NCompileGraph;
import java.util.ArrayList;
import java.util.List;

public abstract class CmnMathOp extends Operator {

	private boolean isSetTo = false;
	private boolean isDoubleOp = false;

	public abstract APlainOpCode getSimpleOperationCommand();

	public CmnMathOp(String source) {
		if (source != null && source.endsWith("=")) {
			isSetTo = true;
		}
		isDoubleOp = (source != null && source.length() > 1) ? source.charAt(0) == source.charAt(1) : false;
		isSetTo |= isDoubleOp;
	}

	@Override
	public DataType getInputTypeLHS() {
		return isSetTo ? DataType.VAR_INT : DataType.INT;
	}

	@Override
	public DataType getInputTypeRHS() {
		return isDoubleOp ? DataType.VOID : DataType.INT;
	}

	@Override
	public DataType getOutputType() {
		return DataType.INT;
	}

	@Override
	protected List<AInstruction> createOperation(NCompileGraph cg) {
		throw new UnsupportedOperationException("This method should never have been called on this class.");
	}

	@Override
	public Priority getPriority() {
		return Priority.NORMAL;
	}

	public boolean isResultInAlt() {
		return false;
	}

	@Override
	public List<AInstruction> getOperation(Throughput left, Throughput right, EffectiveLine line, NCompileGraph cg) {
		if (isDoubleOp) {
			if (!getSupportsDoubleOp()) {
				if (line != null) {
					line.throwException("Double operator not supported here.");
					return new ArrayList<>();
				} else {
					throw new UnsupportedOperationException("Double operator not supported here.");
				}
			}
		}

		List<AInstruction> l = createCommonMathOpSequence(left == null ? null : left.getCode(getInputTypeLHS()), right == null ? null : right.getCode(getInputTypeRHS()), isDoubleOp, cg);

		l.add(cg.getPlain(getSimpleOperationCommand()));

		if (isResultInAlt()) {
			l.add(cg.getPlain(APlainOpCode.MOVE_ALT_TO_PRI));
		}

		if (isSetTo) {
			if (left instanceof VariableThroughput) {
				l.add(((VariableThroughput) left).getWriteIns(cg)); //result is now stored
			} else {
				if (line != null) {
					line.throwException("The left hand side of a set operator must be a variable.");
					return new ArrayList<>();
				} else {
					throw new UnsupportedOperationException("Not a variable.");
				}
			}
		}

		return l;
	}

	public static List<AInstruction> createCommonMathOpSequence(List<AInstruction> leftSource, List<AInstruction> rightSource, boolean doubleOp, NCompileGraph cg) {
		List<AInstruction> l = new ArrayList<>();
		if (leftSource != null) {
			l.addAll(leftSource);
		}
		l.add(cg.getPlain(APlainOpCode.PUSH_PRI));
		if (doubleOp) {
			l.add(cg.getPlain(APlainOpCode.CONST_ALT, 1)); //will get optimized to INC/DEC in the assembler
		} else {
			if (rightSource != null) {
				l.addAll(rightSource);
			}
			l.add(cg.getPlain(APlainOpCode.MOVE_PRI_TO_ALT));
		}
		l.add(cg.getPlain(APlainOpCode.POP_PRI));
		return l;
	}

	public boolean getSupportsDoubleOp() {
		return false;
	}

	public static class Add extends CmnMathOp {

		public Add(String source) {
			super(source);
		}

		@Override
		public APlainOpCode getSimpleOperationCommand() {
			return APlainOpCode.ADD;
		}

		@Override
		public boolean getSupportsDoubleOp() {
			return true;
		}
	}

	public static class Sub extends CmnMathOp {

		public Sub(String source) {
			super(source);
		}

		@Override
		public APlainOpCode getSimpleOperationCommand() {
			return APlainOpCode.SUBTRACT;
		}

		@Override
		public boolean getSupportsDoubleOp() {
			return true;
		}
	}

	public static class Mul extends CmnMathOp {

		public Mul(String source) {
			super(source);
		}

		@Override
		public APlainOpCode getSimpleOperationCommand() {
			return APlainOpCode.MULTIPLY;
		}

		@Override
		public Priority getPriority() {
			return Priority.ALG_MULT;
		}
	}

	public static class Div extends Mul {

		public Div(String source) {
			super(source);
		}

		@Override
		public APlainOpCode getSimpleOperationCommand() {
			return APlainOpCode.DIVIDE;
		}
	}

	public static class Mod extends Div {

		public Mod(String source) {
			super(source);
		}

		@Override
		public APlainOpCode getSimpleOperationCommand() {
			return APlainOpCode.MODULO;
		}

		@Override
		public boolean isResultInAlt() {
			return true;
		}
	}

	//bitwise ops
	public static class BitAnd extends Mul {

		public BitAnd(String source) {
			super(source);
		}

		@Override
		public APlainOpCode getSimpleOperationCommand() {
			return APlainOpCode.AND;
		}

		@Override
		public Priority getPriority() {
			return Priority.BOOLOPS;
		}
	}

	public static class BitOr extends Mul {

		public BitOr(String source) {
			super(source);
		}

		@Override
		public APlainOpCode getSimpleOperationCommand() {
			return APlainOpCode.OR;
		}

		@Override
		public Priority getPriority() {
			return Priority.BOOLOPS;
		}
	}

	public static class Xor extends Mul {

		public Xor(String source) {
			super(source);
		}

		@Override
		public APlainOpCode getSimpleOperationCommand() {
			return APlainOpCode.XOR;
		}

		@Override
		public Priority getPriority() {
			return Priority.BOOLOPS;
		}
	}
}
