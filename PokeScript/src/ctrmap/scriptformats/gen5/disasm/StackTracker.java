package ctrmap.scriptformats.gen5.disasm;

import ctrmap.pokescript.instructions.gen5.VConstants;
import ctrmap.pokescript.instructions.gen5.VOpCode;
import ctrmap.pokescript.instructions.gen5.VStackCmpOpRequest;
import java.io.PrintStream;
import java.util.Stack;

public class StackTracker {

	private Stack<StackElement> elems = new Stack<>();

	public void push(StackElement elem) {
		elems.push(elem);
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
					StackElementType t = param < VConstants.GP_REG_PRI ? StackElementType.CONSTANT : StackElementType.VARIABLE;
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

		public StackElement() {

		}

		public StackElement(StackElementType type, int value) {
			this.type = type;
			this.value = value;
		}

		public void print(PrintStream out) {
			switch (type) {
				case CONSTANT:
					out.print(value);
					break;
				case VARIABLE:
					out.print("v");
					out.print(value - VConstants.GP_REG_PRI);
					break;
				case FLAG:
					out.print("EventFlags.Get(");
					out.print(value);
					out.print(")");
					break;
			}
		}
	}

	public static class ResultElement extends StackElement {

		public StackElement lhs;
		public StackElement rhs;
		public int cmpType;

		private StackElement getSideByType(StackElementType t) {
			if (lhs.type == t) {
				return lhs;
			}
			if (rhs.type == t) {
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
		public void print(PrintStream out) {
			StackElement flag = getSideByType(StackElementType.FLAG);

			StackElement cons = getSideByType(StackElementType.CONSTANT);
			StackElement var = getSideByType(StackElementType.VARIABLE);

			if (flag != null && (cons != null || var != null)) {
				//can only be equal or nequal - ignore side of equation
				
				if (var != null){
					flag.print(out);
					printOperator(out);
					out.print("(boolean) ");
					var.print(out);
				}
				else if (cons != null) {
					boolean isNequal = cmpType == VStackCmpOpRequest.NEQUAL;
					
					out.print((cons.value == 0 ^ isNequal) ? "!" : "");
					flag.print(out);
				}
			} else {
				lhs.print(out);
				printOperator(out);
				rhs.print(out);
			}
		}

		private void printOperator(PrintStream out) {
			out.print(" ");
			out.print(VStackCmpOpRequest.getStrOperator(cmpType));
			out.print(" ");
		}
	}

	public static class OperationElement extends StackElement {

		public StackElement lhs;
		public StackElement rhs;
		public VOpCode operator;

		@Override
		public void print(PrintStream out) {
			lhs.print(out);
			out.print(" ");
			switch (operator) {
				case AddPriAlt:
					out.print("+");
					break;
				case SubPriAlt:
					out.print("-");
					break;
				case MulPriAlt:
					out.print("*");
					break;
				case DivPriAlt:
					out.print("/");
					break;
			}
			out.print(" ");
			rhs.print(out);
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
