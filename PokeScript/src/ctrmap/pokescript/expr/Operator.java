package ctrmap.pokescript.expr;

import ctrmap.pokescript.types.DataType;
import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.instructions.abstractcommands.APlainOpCode;
import ctrmap.pokescript.stage0.EffectiveLine;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.pokescript.types.TypeDef;
import java.util.ArrayList;
import java.util.List;

public abstract class Operator {

	public boolean getHasOnlyRHS() {
		return getInputTypeLHS().baseType == DataType.VOID;
	}

	public boolean getHasOnlyLHS() {
		return getInputTypeRHS().baseType == DataType.VOID;
	}

	public abstract TypeDef getInputTypeLHS();

	public abstract TypeDef getInputTypeRHS();

	public abstract TypeDef getOutputType();

	public abstract Priority getPriority();

	protected abstract List<AInstruction> createOperation(NCompileGraph cg);

	public boolean checkCast(Throughput left, Throughput right, EffectiveLine line){
		boolean b = true;
		if (left != null){
			b &= left.checkImplicitCast(getInputTypeLHS(), line);
		}
		if (right != null){
			b &= right.checkImplicitCast(getInputTypeRHS(), line);
		}
		return b;
	}
	
	public List<AInstruction> getOperation(Throughput left, Throughput right, EffectiveLine line, NCompileGraph cg) {
		List<AInstruction> r = createCommonOpSequence(left.getCode(getInputTypeLHS()), right.getCode(getInputTypeRHS()), cg);
		r.addAll(createOperation(cg));
		return r;
	}

	public static List<AInstruction> createCommonOpSequence(List<AInstruction> leftSource, List<AInstruction> rightSource, NCompileGraph cg) {
		List<AInstruction> l = new ArrayList<>();
		l.addAll(leftSource);
		//left rsl now in PRI
		l.add(cg.getPlain(APlainOpCode.PUSH_PRI));
		l.addAll(rightSource);
		//right rsl now in PRI
		l.add(cg.getPlain(APlainOpCode.MOVE_PRI_TO_ALT));
		//right rsl now in ALT
		l.add(cg.getPlain(APlainOpCode.POP_PRI));
		//left rsl, popped from stack, now in PRI
		//PRI = Lrsl, ALT = Rrsl
		return l;
	}

	public static Operator create(String source) {
		return create(source, null);
	}

	public static Operator create(String source, EffectiveLine line) {
		if (source == null) {
			return null;
		}
		String s = source.trim();
		if (s.length() > 0 && s.length() <= 2) {
			switch (s) {
				case "||":
					return new Or();
				case "&&":
					return new And();
				case "==":
					return new Equals();
				case "!=":
					return new NotEqual();

				//comparison
				case ">=":
					return new CmnCompOp.GreaterOrEqual();
				case "<=":
					return new CmnCompOp.LessOrEqual();
			}
			//common
			switch (s.charAt(0)) {
				case '+':
					return new CmnMathOp.Add(s);
				case '-':
					return new CmnMathOp.Sub(s);
				case '*':
					return new CmnMathOp.Mul(s);
				case '/':
					return new CmnMathOp.Div(s);
				case '&':
					return new CmnMathOp.BitAnd(s);
				case '|':
					return new CmnMathOp.BitOr(s);
				case '^':
					return new CmnMathOp.Xor(s);
				case '!':
					return new Not();
				case '>':
					return new CmnCompOp.Greater();
				case '<':
					return new CmnCompOp.Less();
				case '=':
					return new SetEquals();
				case '%':
					return new CmnMathOp.Mod(s);
			}
		}

		if (line == null) {
//			throw new CompileGraph.CompileException("Not a valid operator " + s);
		}
		else {
			line.throwException("Invalid operator: " + s);
			return null;
		}
		return null;
	}

	public static boolean isOp(String check) {
		if (check.length() < 1) {
			return false;
		}
		String s = check.substring(0, Math.min(check.length(), 2));
		switch (s) {
			case "||":
			case "&&":
			case "==":
			case "!=":
			case ">=":
			case "<=":
				return true;
		}
		//common
		switch (s.charAt(0)) {
			case '+':
			case '-':
			case '*':
			case '/':
			case '&':
			case '|':
			case '^':
			case '!':
			case '>':
			case '<':
				return true;
		}
		return false;
	}

	public static enum Priority {
		ASSIGNMENT,
		BOOLOPS,
		COMPARE,
		NORMAL,
		ALG_MULT,
		NEGATE,
	}
}
