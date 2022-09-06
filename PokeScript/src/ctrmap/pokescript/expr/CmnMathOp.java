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

public abstract class CmnMathOp extends NumerableOperator {

	public final boolean isSetTo;
	public final boolean isDoubleOp;
	
	public abstract APlainOpCode getIntegerOperationOpcode();
	public abstract AFloatOpCode getFloatOperationOpcode();
	
	public CmnMathOp(boolean isDoubleOp, boolean isSetTo) {
		this.isDoubleOp = isDoubleOp;
		this.isSetTo = isSetTo | isDoubleOp;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "; SetTo=" + isSetTo + "; Double=" + isDoubleOp;
	}

	@Override
	public TypeDef getInputTypeLHS() {
		return isSetTo ? DataType.VAR_INT.typeDef() : DataType.INT.typeDef();
	}

	@Override
	public TypeDef getInputTypeRHS() {
		return isDoubleOp ? DataType.VOID.typeDef() : DataType.INT.typeDef();
	}

	@Override
	public TypeDef getOutputType() {
		return highestNumberType;
	}

	@Override
	public Priority getPriority() {
		return Priority.NORMAL;
	}

	public boolean isResultInAlt() {
		return false;
	}

	@Override
	public List<AInstruction> getOperation(EffectiveLine line, NCompileGraph cg, Throughput... inputs) {
		if (isDoubleOp) {
			if (!getSupportsDoubleOp()) {
				if (line != null) {
					line.throwException("Double operator not supported here. (" + getClass().getSimpleName() + ")");
					return new ArrayList<>();
				} else {
					throw new UnsupportedOperationException("Double operator not supported here.");
				}
			}
		}

		List<AInstruction> l = createCommonMathOpSequence(
			(inputs.length < 1 || inputs[0] == null) ? null : inputs[0].getCode(getInputTypeLHS()), 
			(inputs.length < 2 || inputs[1] == null) ? null : inputs[1].getCode(getInputTypeRHS()), 
			isDoubleOp, 
			cg
		);

		if (useFloatingOps()) {
			AFloatOpCode opcode = getFloatOperationOpcode();
			if (opcode != null) {
				l.add(cg.getPlainFloat(opcode));
			}
			else {
				l.add(cg.getPlain(getIntegerOperationOpcode()));
			}
		}
		else {
			l.add(cg.getPlain(getIntegerOperationOpcode()));
		}

		if (isResultInAlt()) {
			l.add(cg.getPlain(APlainOpCode.MOVE_ALT_TO_PRI));
		}

		if (isSetTo) {
			if (inputs[0] instanceof VariableThroughput) {
				l.add(((VariableThroughput) inputs[0]).getWriteIns(cg)); //result is now stored
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

	public List<AInstruction> createCommonMathOpSequence(List<AInstruction> leftSource, List<AInstruction> rightSource, boolean doubleOp, NCompileGraph cg) {
		List<AInstruction> l = new ArrayList<>();
		if (leftSource != null) {
			l.addAll(leftSource);
			addFloatCvtIfNeeded(l, cg);
		}
		l.add(cg.getPlain(APlainOpCode.PUSH_PRI));
		if (doubleOp) {
			l.add(cg.getPlain(APlainOpCode.CONST_ALT, 1)); //will get optimized to INC/DEC in the assembler
		} else {
			if (rightSource != null) {
				l.addAll(rightSource);
				addFloatCvtIfNeeded(l, cg);
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
		
		public Add(boolean isDoubleOp, boolean isSetTo) {
			super(isDoubleOp, isSetTo);
		}

		@Override
		public APlainOpCode getIntegerOperationOpcode() {
			return APlainOpCode.ADD;
		}

		@Override
		public boolean getSupportsDoubleOp() {
			return true;
		}

		@Override
		public OperatorOperation getOperationType() {
			return OperatorOperation.ADD;
		}

		@Override
		public AFloatOpCode getFloatOperationOpcode() {
			return AFloatOpCode.VADD;
		}
	}

	public static class Sub extends CmnMathOp {

		public Sub(boolean isDoubleOp, boolean isSetTo) {
			super(isDoubleOp, isSetTo);
		}

		@Override
		public APlainOpCode getIntegerOperationOpcode() {
			return APlainOpCode.SUBTRACT;
		}

		@Override
		public boolean getSupportsDoubleOp() {
			return true;
		}

		@Override
		public OperatorOperation getOperationType() {
			return OperatorOperation.SUB;
		}

		@Override
		public AFloatOpCode getFloatOperationOpcode() {
			return AFloatOpCode.VSUB;
		}
	}

	public static class Mul extends CmnMathOp {

		public Mul(boolean isDoubleOp, boolean isSetTo) {
			super(isDoubleOp, isSetTo);
		}
		
		@Override
		public APlainOpCode getIntegerOperationOpcode() {
			return APlainOpCode.MULTIPLY;
		}

		@Override
		public Priority getPriority() {
			return Priority.ALG_MULT;
		}

		@Override
		public OperatorOperation getOperationType() {
			return OperatorOperation.MUL;
		}

		@Override
		public AFloatOpCode getFloatOperationOpcode() {
			return AFloatOpCode.VMULTIPLY;
		}
	}

	public static class Div extends Mul {
		
		public Div(boolean isDoubleOp, boolean isSetTo) {
			super(isDoubleOp, isSetTo);
		}
		
		@Override
		public APlainOpCode getIntegerOperationOpcode() {
			return APlainOpCode.DIVIDE;
		}
		
		@Override
		public OperatorOperation getOperationType() {
			return OperatorOperation.DIV;
		}
		
		@Override
		public AFloatOpCode getFloatOperationOpcode() {
			return AFloatOpCode.VDIVIDE;
		}
	}

	public static class Mod extends Div {

		public Mod(boolean isDoubleOp, boolean isSetTo) {
			super(isDoubleOp, isSetTo);
		}

		@Override
		public APlainOpCode getIntegerOperationOpcode() {
			return APlainOpCode.MODULO;
		}
		
		@Override
		public OperatorOperation getOperationType() {
			return OperatorOperation.MOD;
		}
		
		@Override
		public AFloatOpCode getFloatOperationOpcode() {
			return AFloatOpCode.VMODULO;
		}

		@Override
		public boolean isResultInAlt() {
			return true;
		}
	}

	//bitwise ops
	public static class BitAnd extends Mul {

		public BitAnd(boolean isDoubleOp, boolean isSetTo) {
			super(isDoubleOp, isSetTo);
		}

		@Override
		public APlainOpCode getIntegerOperationOpcode() {
			return APlainOpCode.AND;
		}

		@Override
		public Priority getPriority() {
			return Priority.BOOLOPS;
		}
		
		@Override
		public OperatorOperation getOperationType() {
			return OperatorOperation.BIT_AND;
		}
		
		@Override
		public AFloatOpCode getFloatOperationOpcode() {
			return null;
		}
	}

	public static class BitOr extends Mul {

		public BitOr(boolean isDoubleOp, boolean isSetTo) {
			super(isDoubleOp, isSetTo);
		}

		@Override
		public APlainOpCode getIntegerOperationOpcode() {
			return APlainOpCode.OR;
		}

		@Override
		public Priority getPriority() {
			return Priority.BOOLOPS;
		}
		
		@Override
		public OperatorOperation getOperationType() {
			return OperatorOperation.BIT_OR;
		}
		
		@Override
		public AFloatOpCode getFloatOperationOpcode() {
			return null;
		}
	}

	public static class Xor extends Mul {
		
		public Xor(boolean isDoubleOp, boolean isSetTo) {
			super(isDoubleOp, isSetTo);
		}

		@Override
		public APlainOpCode getIntegerOperationOpcode() {
			return APlainOpCode.XOR;
		}

		@Override
		public Priority getPriority() {
			return Priority.BOOLOPS;
		}
		
		@Override
		public OperatorOperation getOperationType() {
			return OperatorOperation.BIT_XOR;
		}
		
		@Override
		public AFloatOpCode getFloatOperationOpcode() {
			return null;
		}
	}
}
