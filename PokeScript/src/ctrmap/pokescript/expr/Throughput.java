package ctrmap.pokescript.expr;

import ctrmap.pokescript.types.DataType;
import ctrmap.scriptformats.gen6.PawnInstruction;
import ctrmap.pokescript.data.Variable;
import ctrmap.pokescript.instructions.abstractcommands.AAccessGlobal;
import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.instructions.abstractcommands.APlainInstruction;
import ctrmap.pokescript.instructions.abstractcommands.APlainOpCode;
import ctrmap.pokescript.stage0.EffectiveLine;
import ctrmap.pokescript.stage0.Modifier;
import ctrmap.pokescript.stage1.NCompileGraph;
import java.util.ArrayList;
import java.util.List;

public class Throughput {

	//specifies the data interchanged between operators
	//gets exported from a child expression to a parent with all previous instructions
	public DataType type;
	protected List<AInstruction> code;

	public Throughput(DataType t, List<AInstruction> code) {
		type = t;
		this.code = code;
	}

	public Throughput(int immediateValue, NCompileGraph cg) {
		type = DataType.INT;
		makeConst(immediateValue, cg);
	}

	public Throughput(float immediateValue, NCompileGraph cg) {
		type = DataType.FLOAT;
		makeConst(Float.floatToIntBits(immediateValue), cg);
	}

	private void makeConst(int constValue, NCompileGraph cg) {
		code = new ArrayList<>();
		code.add(cg.getPlain(APlainOpCode.CONST_PRI, constValue));
	}

	public static Throughput getPrimaryRegisterPushDummyThroughput(NCompileGraph cg) {
		Throughput t = new Throughput();
		t.code.add(cg.getPlain(APlainOpCode.PUSH_PRI));
		return t;
	}

	public Throughput(boolean immediateValue, NCompileGraph cg) {
		makeConst(immediateValue ? 1 : 0, cg);
		type = DataType.BOOLEAN;
	}

	public Throughput() {
		type = DataType.ANY;
	}

	public boolean isImmediate() {
		if (code.isEmpty()) {
			return true; //uninitialized
		}
		if (code.size() == 1) {
			AInstruction ins = code.get(0);
			if (ins instanceof APlainInstruction) {
				APlainOpCode cmd = ((APlainInstruction) ins).opCode;
				if (cmd == APlainOpCode.CONST_PRI) {
					return true;
				}
			}
		}
		if (isVariable()) {
			Variable var = getVariable();

			if (var instanceof Variable.Global) {
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

				if (var instanceof Variable.Global) {
					Variable.Global g = (Variable.Global) var;
					
					return g.getImmediateValue();
				}
			} else {
				return ((APlainInstruction) code.get(0)).getArgument(0);
			}
		}
		return 0;
	}

	public List<AInstruction> getCode(DataType input) {
		checkCast(input);
		return code;
	}

	public void checkCast(DataType input) {
		checkCast(input, null);
	}

	public boolean checkCast(DataType input, EffectiveLine line) {
		boolean allow = false;
		switch (input) {
			case ANY:
				allow = true;
				break;
			case ANY_VAR:
				allow = type == DataType.VAR_BOOLEAN || type == DataType.VAR_INT || type == DataType.VAR_FLOAT;
				break;
			case BOOLEAN:
				allow = type == DataType.BOOLEAN || type == DataType.VAR_BOOLEAN;
				break;
			case INT:
				allow = type == DataType.INT || type == DataType.VAR_INT;
				break;
			case FLOAT:
				allow = type == DataType.FLOAT || type == DataType.VAR_FLOAT || type == DataType.INT || type == DataType.VAR_INT;
				break;
			case VAR_BOOLEAN:
			case VAR_INT:
			case VAR_FLOAT:
				allow = type == input;
				break;
		}
		if (!allow) {
			if (line == null) {
				//throw new CompileGraph.CompileException("Invalid operand: " + input + " expected, got " + type);
			} else {
				line.throwException("Invalid operand: " + input + " expected, got " + type);
			}
		}
		return allow;
	}

}
