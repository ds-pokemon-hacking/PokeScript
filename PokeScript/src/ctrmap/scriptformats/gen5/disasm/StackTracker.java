package ctrmap.scriptformats.gen5.disasm;

import ctrmap.pokescript.instructions.gen5.VConstants;
import ctrmap.pokescript.instructions.gen5.VOpCode;
import ctrmap.pokescript.instructions.gen5.VStackCmpOpRequest;
import ctrmap.scriptformats.gen5.VDecompiler;
import java.io.PrintStream;
import java.util.Stack;

public class StackTracker {

	private Stack<StackElement> elems = new Stack<>();

	public void push(StackElement elem) {
		elems.push(elem);
	}

	public boolean empty() {
		return elems.empty();
	}
	
	public StackElement pop() {
		if (!elems.empty()) {
			return elems.pop();
		} else {
			System.err.println("WARN: Tried to pop an empty stack.");
		}
		return null;
	}

	public void pushResult(int cmpType) {
		ResultElement elem = new ResultElement();
		elem.rhs = pop();
		elem.lhs = pop();
		elem.cmpType = cmpType;
		push(elem);
	}

	public void pushOperation(VOpCode operator) {
		OperationElement elem = new OperationElement();
		elem.rhs = pop();
		elem.lhs = pop();
		elem.operator = operator;
		push(elem);
	}

	public StackCommands handleInstruction(DisassembledCall call) {
		int opcode = call.definition.opCode;

		StackCommands cmd = StackCommands.valueOf(opcode);

		if (cmd != null) {
			switch (cmd) {
				case POP:
					pop();
					break;
				case POP_AND_DEREF:
					pop();
					break;
				case POP_TO:
					return cmd;
				case PUSH_CONST:
					push(new StackElement(StackElementType.CONSTANT, call.args[0]));
					break;
				case PUSH_FLAG:
					push(new StackElement(StackElementType.FLAG, call.args[0]));
					break;
				case PUSH_VAR:
					int param = call.args[0];
					StackElementType t = param < VConstants.WKVAL_START ? StackElementType.CONSTANT : StackElementType.VARIABLE;
					push(new StackElement(t, param));
					break;
				case ADD:
				case DIV:
				case MUL:
				case SUB:
					pushOperation(cmd.opCode);
					break;
				case CMP:
					pushResult(call.args[0]);
					break;
			}
			return cmd;
		}

		return null;
	}

	public static class StackElement {

		public StackElementType type;
		public int value;

		protected StackElement() {

		}

		public StackElement(StackElementType type, int value) {
			if (type == null) {
				throw new NullPointerException("Type can not be null.");
			}
			this.type = type;
			this.value = value;
		}

		public void print(PrintStream out) {
			out.print(toString());
		}

		@Override
		public String toString() {
			switch (type) {
				case CONSTANT:
					return String.valueOf(value);
				case VARIABLE:
					return VDecompiler.flex2Str(value);
				case FLAG:
					return "EventFlags.Get(" + value + ")";
			}
			return null;
		}
	}

	public static class ResultElement extends StackElement {

		public StackElement lhs;
		public StackElement rhs;
		public int cmpType;

		private StackElement getSideByType(StackElementType t) {
			if (lhs != null && lhs.type == t) {
				return lhs;
			}
			if (lhs != null && rhs.type == t) {
				return rhs;
			}
			return null;
		}

		private StackElement getOtherSideIfType(StackElement side, StackElementType t) {
			if (side == lhs && rhs.type == t) {
				return rhs;
			}
			if (side == rhs && lhs.type == t) {
				return lhs;
			}
			return null;
		}

		@Override
		public String toString() {
			StringBuilder out = new StringBuilder();
			StackElement flag = getSideByType(StackElementType.FLAG);

			StackElement cons = getSideByType(StackElementType.CONSTANT);
			StackElement var = getSideByType(StackElementType.VARIABLE);

			if (flag != null && (cons != null || var != null)) {
				//can only be equal or nequal - ignore side of equation

				if (var != null) {
					out.append(flag.toString());
					out.append(getOpStr());
					out.append("(boolean) ");
					out.append(var.toString());
				} else if (cons != null) {
					boolean isNequal = cmpType == VStackCmpOpRequest.NEQUAL;

					out.append((cons.value == 0 ^ isNequal) ? "!" : "");
					out.append(flag);
				}
			} else {
				if (lhs != null) {
					out.append(lhs.toString());
				} else {
					out.append("[STACK ERROR]");
				}
				out.append(getOpStr());
				if (rhs != null) {
					out.append(rhs.toString());
				} else {
					out.append("[STACK ERROR]");
				}
			}
			return out.toString();
		}

		private String getOpStr() {
			return " "
				+ VStackCmpOpRequest.getStrOperator(cmpType)
				+ " ";
		}
	}

	public static class OperationElement extends StackElement {

		public StackElement lhs;
		public StackElement rhs;
		public VOpCode operator;

		@Override
		public String toString() {
			StringBuilder out = new StringBuilder();
			out.append(lhs.toString());
			out.append(" ");
			switch (operator) {
				case AddPriAlt:
					out.append("+");
					break;
				case SubPriAlt:
					out.append("-");
					break;
				case MulPriAlt:
					out.append("*");
					break;
				case DivPriAlt:
					out.append("/");
					break;
			}
			out.append(" ");
			out.append(rhs.toString());
			return out.toString();
		}
	}

	public static enum StackElementType {
		CONSTANT,
		VARIABLE,
		FLAG,
		RESULT,
		OPERATION
	}
}
