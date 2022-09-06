package ctrmap.pokescript.expr.ast;

import ctrmap.pokescript.data.Variable;
import ctrmap.pokescript.expr.Throughput;
import ctrmap.pokescript.expr.VariableThroughput;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.pokescript.types.DataType;
import xstandard.util.ParsingUtils;

public class ASTOperandNode extends ASTNode {

	public String operandText;

	public Object operand;
	public ASTOperandType operandType;

	public ASTOperandNode(ASTContentType charType, String operand) {
		super(charType);
		this.operandText = operand;
	}

	public ASTOperandNode(Object operand) {
		super(ASTContentType.SYMBOL);
		this.operand = operand;
		if (operand instanceof Boolean) {
			operandType = ASTOperandType.BOOLEAN;
		} else if (operand instanceof Float) {
			operandType = ASTOperandType.FLOATING_POINT;
		} else if (operand instanceof Number) {
			operandType = ASTOperandType.INTEGER;
		} else {
			throw new RuntimeException("Could not make immediate operand of type " + (operand == null ? "NULL" : operand.getClass()));
		}
	}

	@Override
	public Throughput toThroughput(NCompileGraph cg) {
		if (operandType != null) {
			switch (operandType) {
				case BOOLEAN:
					return new Throughput((boolean)operand, cg);
				case FLOATING_POINT:
					return new Throughput((float)operand, cg);
				case INTEGER:
					return new Throughput((int)operand, cg);
				case GLOBAL_VARIABLE:
				case LOCAL_VARIABLE:
					return new VariableThroughput((Variable)operand, cg);
			}
		}
		return null;
	}

	public boolean isImmediate() {
		if (operandType != null) {
			switch (operandType) {
				case FLOATING_POINT:
				case INTEGER:
				case BOOLEAN:
					return true;
				case GLOBAL_VARIABLE:
					Variable.Global gvar = (Variable.Global) operand;
					return gvar.isImmediate() && !gvar.typeDef.isClass();
				case LOCAL_VARIABLE:
					return false;
			}
		}
		return false;
	}

	public ASTOperandType getImmValueType() {
		switch (operandType) {
			case GLOBAL_VARIABLE:
			case LOCAL_VARIABLE:
				Variable var = (Variable) operand;
				if (var != null) {
					switch (var.typeDef.baseType.getBaseType()) {
						case BOOLEAN:
							return ASTOperandType.BOOLEAN;
						case FLOAT:
							return ASTOperandType.FLOATING_POINT;
						case INT:
						case ENUM:
							return ASTOperandType.INTEGER;
					}
				}
				break;
		}
		return operandType;
	}

	public boolean getImmValueB() {
		switch (operandType) {
			case BOOLEAN:
				return (boolean) operand;
			case FLOATING_POINT:
			case INTEGER:
				return ((Number) operand).intValue() == 1;
			case GLOBAL_VARIABLE:
				Variable.Global gvar = (Variable.Global) operand;
				return gvar.getImmediateValue() == 1;
			case LOCAL_VARIABLE:
				return false;
		}
		return false;
	}

	public int getImmValueI() {
		switch (operandType) {
			case BOOLEAN:
				return ((Boolean) operand) ? 1 : 0;
			case FLOATING_POINT:
			case INTEGER:
				return ((Number) operand).intValue();
			case GLOBAL_VARIABLE:
				Variable.Global gvar = (Variable.Global) operand;
				if (gvar.typeDef.baseType == DataType.FLOAT) {
					return (int) Float.intBitsToFloat(gvar.getImmediateValue());
				}
				return gvar.getImmediateValue();
			case LOCAL_VARIABLE:
				return 0;
		}
		return 0;
	}

	public float getImmValueF() {
		switch (operandType) {
			case BOOLEAN:
				return ((Boolean) operand) ? 1f : 0f;
			case FLOATING_POINT:
			case INTEGER:
				return ((Number) operand).floatValue();
			case GLOBAL_VARIABLE:
				Variable.Global gvar = (Variable.Global) operand;
				if (gvar.typeDef.baseType == DataType.FLOAT) {
					return Float.intBitsToFloat(gvar.getImmediateValue());
				}
				return (float) gvar.getImmediateValue();
			case LOCAL_VARIABLE:
				return 0f;
		}
		return 0f;
	}

	@Override
	protected void analyzeThis(AST tree) {
		if (operandText.equals("true")) {
			operandType = ASTOperandType.BOOLEAN;
			operand = true;
			return;
		}
		if (operandText.equals("false")) {
			operandType = ASTOperandType.BOOLEAN;
			operand = false;
			return;
		}
		try {
			operand = ParsingUtils.parseBasedInt(operandText);
			operandType = ASTOperandType.INTEGER;
			return;
		} catch (NumberFormatException ex) {

		}
		try {
			if (operandText.endsWith("f")) {
				operand = Float.parseFloat(operandText.substring(0, operandText.length() - 1)); //remove f suffix
				operandType = ASTOperandType.FLOATING_POINT;
				return;
			}
		} catch (NumberFormatException ex) {

		}
		Variable local = tree.resolveLocal(operandText);
		if (local != null) {
			operand = local;
			operandType = ASTOperandType.LOCAL_VARIABLE;
			return;
		}
		Variable global = tree.resolveGVar(operandText);
		if (global != null) {
			operand = global;
			operandType = ASTOperandType.GLOBAL_VARIABLE;
			return;
		}
		tree.throwException("Could not resolve symbol \"" + operandText + "\".");
	}

	@Override
	public String toString() {
		if (operandType != null) {
			return "[" + operandType + "]" + operand;
		}
		return "[" + charType + "]" + operandText;
	}
}
