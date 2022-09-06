package ctrmap.pokescript.expr.ast;

import ctrmap.pokescript.expr.CmnMathOp;
import ctrmap.pokescript.expr.Operator;
import ctrmap.pokescript.expr.OperatorOperation;
import ctrmap.pokescript.expr.OperatorType;
import ctrmap.pokescript.expr.Throughput;
import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.stage1.NCompileGraph;
import xstandard.util.ArraysEx;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ASTOperatorNode extends ASTNode {

	public Operator operator;

	public ASTOperatorNode(Operator oper) {
		super(ASTContentType.OP_CHAR);
		this.operator = oper;
	}

	@Override
	public String toString() {
		return "(" + Objects.toString(operator) + ")";
	}

	@Override
	public ASTNode simplifyThis(AST ast) {
		ASTNode lhs = getChild(0);
		ASTNode rhs = getChild(1);
		if ((lhs == null || !lhs.isOperatorNode()) && (rhs == null || !rhs.isOperatorNode())) {
			ASTOperandNode lhsOperand = (ASTOperandNode) lhs;
			ASTOperandNode rhsOperand = (ASTOperandNode) rhs;
			boolean allow = false;
			switch (operator.getType()) {
				case BINARY:
					allow = lhsOperand != null && rhsOperand != null && lhsOperand.isImmediate() && rhsOperand.isImmediate();
					break;
				case UNARY:
					rhsOperand = lhsOperand;
					allow = lhsOperand != null && lhsOperand.isImmediate();
					break;
			}
			if (allow) {
				ASTOperandType typeL = lhsOperand == null ? null : lhsOperand.getImmValueType();
				ASTOperandType typeR = rhsOperand == null ? null : rhsOperand.getImmValueType();
				if (operator.getType() == OperatorType.UNARY && typeR != null) {
					switch (typeR) {
						case BOOLEAN:
							return simplifyBoolUnary(rhsOperand);
						case FLOATING_POINT:
							return simplifyFloatUnary(rhsOperand);
						case INTEGER:
							return simplifyIntUnary(rhsOperand);
					}
				} else if (operator.getType() == OperatorType.BINARY && typeL != null && typeR != null) {
					if (typeL == ASTOperandType.BOOLEAN && typeR == ASTOperandType.BOOLEAN) {
						return simplifyBoolBinary(lhsOperand, rhsOperand);
					} else if (typeL == ASTOperandType.INTEGER && typeR == ASTOperandType.INTEGER) {
						return simplifyIntBinary(lhsOperand, rhsOperand);
					} else if (isNumberType(typeL) && isNumberType(typeR)) {
						//implicit cast to floating points
						return simplifyFloatBinary(lhsOperand, rhsOperand);
					}
				}
			}

			//FP division to multiplication
			if (operator.getOperationType() == OperatorOperation.DIV && rhsOperand != null && rhsOperand.isImmediate()) {
				if (rhsOperand.getImmValueType() == ASTOperandType.FLOATING_POINT) {
					CmnMathOp.Div divOp = (CmnMathOp.Div) operator;
					operator = new CmnMathOp.Mul(divOp.isDoubleOp, divOp.isSetTo);
					rhsOperand.operand = 1f / rhsOperand.getImmValueF();
				}
			}
		}

		return this;
	}

	private static boolean isNumberType(ASTOperandType t) {
		return t == ASTOperandType.FLOATING_POINT || t == ASTOperandType.INTEGER;
	}

	private ASTNode simplifyBoolBinary(ASTOperandNode lhs, ASTOperandNode rhs) {
		boolean bool1 = lhs.getImmValueB();
		boolean bool2 = rhs.getImmValueB();
		switch (operator.getOperationType()) {
			case BIT_AND:
			case BOOL_AND:
				return new ASTOperandNode(bool1 && bool2);
			case BIT_OR:
			case BOOL_OR:
				return new ASTOperandNode(bool1 || bool2);
			case BIT_XOR:
				return new ASTOperandNode(bool1 ^ bool2);
		}
		return this;
	}

	private ASTNode simplifyIntBinary(ASTOperandNode lhs, ASTOperandNode rhs) {
		int int1 = lhs.getImmValueI();
		int int2 = rhs.getImmValueI();
		switch (operator.getOperationType()) {
			case ADD:
				return new ASTOperandNode(int1 + int2);
			case SUB:
				return new ASTOperandNode(int1 - int2);
			case MUL:
				return new ASTOperandNode(int1 * int2);
			case DIV:
				return new ASTOperandNode(int1 / int2);
			case MOD:
				return new ASTOperandNode(int1 % int2);
			case BIT_AND:
				return new ASTOperandNode(int1 & int2);
			case BIT_OR:
				return new ASTOperandNode(int1 | int2);
			case BIT_XOR:
				return new ASTOperandNode(int1 ^ int2);
		}
		return this;
	}

	private ASTNode simplifyFloatBinary(ASTOperandNode lhs, ASTOperandNode rhs) {
		float float1 = lhs.getImmValueF();
		float float2 = rhs.getImmValueF();
		switch (operator.getOperationType()) {
			case ADD:
				return new ASTOperandNode(float1 + float2);
			case SUB:
				return new ASTOperandNode(float1 - float2);
			case MUL:
				return new ASTOperandNode(float1 * float2);
			case DIV:
				return new ASTOperandNode(float1 / float2);
			case MOD:
				return new ASTOperandNode(float1 % float2);
		}
		return this;
	}

	private ASTNode simplifyBoolUnary(ASTOperandNode rhs) {
		if (operator.getOperationType() == OperatorOperation.NOT) {
			return new ASTOperandNode(!rhs.getImmValueB());
		}
		return this;
	}

	private ASTNode simplifyIntUnary(ASTOperandNode rhs) {
		if (operator.getOperationType() == OperatorOperation.NEG) {
			return new ASTOperandNode(-rhs.getImmValueI());
		}
		return this;
	}

	private ASTNode simplifyFloatUnary(ASTOperandNode rhs) {
		if (operator.getOperationType() == OperatorOperation.NEG) {
			ASTOperandNode ret = new ASTOperandNode(-rhs.getImmValueF());
			return ret;
		}
		return this;
	}

	private Throughput[] arrayJoin(Iterable<Throughput[]> arrays) {
		Throughput[] out = new Throughput[ArraysEx.elementCount(arrays, true)];
		ArraysEx.join(arrays, out, true);
		return out;
	}

	private Throughput[] getChildThroughputs(int index, NCompileGraph cg) {
		ASTNode ch = getChild(index);
		if (ch == null) {
			return null;
		} else if (ch.isOperatorNode() && ((ASTOperatorNode) ch).operator.getType() == OperatorType.SEQUENTIAL) {
			ASTOperatorNode op = (ASTOperatorNode) ch;
			List<Throughput[]> l = new ArrayList<>(op.children.size());
			for (int i = 0; i < op.children.size(); i++) {
				l.add(op.getChildThroughputs(i, cg));
			}
			return arrayJoin(l);
		}
		return new Throughput[]{ch.toThroughput(cg)};
	}

	private Throughput[] getInputs(NCompileGraph cg) {
		List<Throughput[]> tps = new ArrayList<>();
		for (int i = 0; i < children.size(); i++) {
			tps.add(getChildThroughputs(i, cg));
		}
		Throughput[] joint = arrayJoin(tps);
		return joint;
	}

	@Override
	public Throughput toThroughput(NCompileGraph cg) {
		switch (operator.getType()) {
			case BINARY:
			case UNARY:
				Throughput[] inputs = getInputs(cg);
				operator.attachOperands(cg, inputs);
				List<AInstruction> l = operator.getOperation(tree.getLine(), cg, inputs);
				return new Throughput(operator.getOutputType(), l);
		}
		return null;
	}
}
