package ctrmap.pokescript.expr;

import ctrmap.pokescript.types.DataType;
import ctrmap.pokescript.data.Variable;
import ctrmap.pokescript.instructions.abstractcommands.AFloatInstruction;
import ctrmap.pokescript.instructions.abstractcommands.AFloatOpCode;
import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.instructions.abstractcommands.AInstructionType;
import ctrmap.pokescript.instructions.abstractcommands.APlainInstruction;
import ctrmap.pokescript.instructions.abstractcommands.APlainOpCode;
import ctrmap.pokescript.stage0.EffectiveLine;
import ctrmap.pokescript.stage0.Modifier;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.pokescript.types.TypeDef;
import java.util.ArrayList;
import java.util.List;

public class Throughput {

	//specifies the data interchanged between operators
	//gets exported from a child expression to a parent with all previous instructions
	public TypeDef type;
	protected List<AInstruction> code = new ArrayList<>();

	public Throughput(TypeDef t, List<AInstruction> code) {
		type = t;
		this.code = code;
	}

	public Throughput(int immediateValue, NCompileGraph cg) {
		type = DataType.INT.typeDef();
		makeConstInt(immediateValue, cg);
	}

	public Throughput(float immediateValue, NCompileGraph cg) {
		type = DataType.FLOAT.typeDef();
		code.add(cg.getPlainFloat(AFloatOpCode.VCONST_PRI, immediateValue));
	}

	private void makeConstInt(int constValue, NCompileGraph cg) {
		code.add(cg.getPlain(APlainOpCode.CONST_PRI, constValue));
	}

	public static Throughput getPrimaryRegisterDummyThroughput(TypeDef type) {
		Throughput t = new Throughput();
		t.type = type;
		return t;
	}

	public static Throughput getAlternateRegisterDummyThroughput(TypeDef type, NCompileGraph cg) {
		Throughput t = new Throughput();
		t.code.add(cg.getPlain(APlainOpCode.MOVE_ALT_TO_PRI));
		t.type = type;
		return t;
	}

	public Throughput(boolean immediateValue, NCompileGraph cg) {
		makeConstInt(immediateValue ? 1 : 0, cg);
		type = DataType.BOOLEAN.typeDef();
	}

	public Throughput() {
		type = DataType.ANY.typeDef();
	}

	public boolean isImmediateConstant() {
		if (code.size() == 1) {
			AInstruction ins = code.get(0);
			
			switch (ins.getType()) {
				case PLAIN:
					return ((APlainInstruction) ins).opCode == APlainOpCode.CONST_PRI;
				case PLAIN_FLOAT:
					return ((AFloatInstruction) ins).opCode == AFloatOpCode.VCONST_PRI;
				default:
					return false;
			}
		}
		return false;
	}

	public boolean isImmediate() {
		if (code.isEmpty()) {
			return true; //uninitialized
		}
		if (isImmediateConstant()) {
			return true;
		}
		if (isVariable()) {
			Variable var = getVariable();

			if (var.getLocation() == Variable.VarLoc.DATA) {
				Variable.Global g = (Variable.Global) var;
				if (g.modifiers.contains(Modifier.FINAL)) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean isVariable() {
		return this instanceof VariableThroughput;
	}

	public Variable getVariable() {
		if (isVariable()) {
			return ((VariableThroughput) this).var;
		}
		return null;
	}

	public int getImmediateValue() {
		if (isImmediate()) {
			if (isVariable()) {
				Variable var = getVariable();

				if (var.getLocation() == Variable.VarLoc.DATA) {
					Variable.Global g = (Variable.Global) var;

					return g.getImmediateValue();
				}
			} else {
				AInstruction i = code.get(0);
				
				switch (i.getType()) {
					case PLAIN:
						return ((APlainInstruction) i).args[0];
					case PLAIN_FLOAT:
						return Float.floatToRawIntBits(((AFloatInstruction) i).args[0]);
					default:
						return 0;
				}
			}
		}
		return 0;
	}

	public List<AInstruction> getCode(TypeDef input) {
		checkCast(input);
		return code;
	}

	public void checkCast(TypeDef input) {
		checkImplicitCast(input, (EffectiveLine) null);
	}

	public boolean checkImplicitCast(TypeDef input, EffectiveLine line) {
		boolean val = checkImplicitCast(input, type);
		if (!val) {
			if (line == null) {
				//throw new CompileGraph.CompileException("Invalid operand: " + input + " expected, got " + type);
			} else {
				line.throwException("Invalid operand: " + input + " expected, got " + type);
			}
		}
		return val;
	}

	public static boolean checkImplicitCast(TypeDef input, TypeDef output) {
		boolean allow = false;

		DataType type = output.baseType;

		switch (input.baseType) {
			case ANY:
				allow = true;
				break;
			case ANY_VAR:
				allow = type == DataType.VAR_BOOLEAN || type == DataType.VAR_INT || type == DataType.VAR_FLOAT || type == DataType.VAR_CLASS || type == DataType.VAR_ENUM;
				break;
			case BOOLEAN:
				allow = type == DataType.BOOLEAN || type == DataType.VAR_BOOLEAN;
				break;
			case INT:
				allow = type == DataType.INT || type == DataType.VAR_INT || type == DataType.ENUM || type == DataType.VAR_ENUM;
				break;
			case ENUM:
				allow = type == DataType.INT || type == DataType.VAR_INT || output.equals(input);
				break;
			case FLOAT:
				allow = type == DataType.FLOAT || type == DataType.VAR_FLOAT || type == DataType.INT || type == DataType.VAR_INT;
				break;
			case VAR_BOOLEAN:
			case VAR_INT:
			case VAR_FLOAT:
				allow = type == input.baseType;
				break;
			case VAR_CLASS:
				allow = type == DataType.VAR_CLASS && input.equals(output);
				break;
			case VAR_ENUM:
				allow = type == DataType.VAR_ENUM && input.equals(output);
				break;
		}
		return allow;
	}

}
