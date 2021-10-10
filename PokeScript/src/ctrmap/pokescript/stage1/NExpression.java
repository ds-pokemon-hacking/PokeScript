package ctrmap.pokescript.stage1;

import ctrmap.pokescript.InboundDefinition;
import ctrmap.pokescript.LangConstants;
import ctrmap.pokescript.stage0.Preprocessor;
import ctrmap.pokescript.OutboundDefinition;
import ctrmap.pokescript.data.Variable;
import ctrmap.pokescript.expr.CmnMathOp;
import ctrmap.pokescript.expr.Neg;
import ctrmap.pokescript.types.DataType;
import ctrmap.pokescript.expr.Operator;
import ctrmap.pokescript.expr.Throughput;
import ctrmap.pokescript.expr.TypeCast;
import ctrmap.pokescript.expr.VariableThroughput;
import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.instructions.abstractcommands.ALocalCall;
import ctrmap.pokescript.stage0.BraceContent;
import ctrmap.pokescript.stage0.EffectiveLine;
import ctrmap.stdlib.util.ArraysEx;
import java.util.ArrayList;
import java.util.List;

public class NExpression {

	private EffectiveLine line;

	private String debugText;
	public Throughput left;
	public Throughput right;
	public Operator op;

	public NExpression(String text, EffectiveLine line, NCompileGraph cg) {
		this.line = line;
		debugText = text;
		//System.out.println(text);

		//NExpressions can be safely stripped of whitespaces
		for (DataType t : DataType.values()) {
			if (text.contains(t.getFriendlyName() + " ")) {
				line.throwException("Declarations in expressions are not supported. - " + text);
			}
		}

		text = getTextWithoutStartEndPairedBrackets(EffectiveLine.removeAllWhitespaces(text));
		//System.out.println("abt to parse " + text);

		//slice left/op/right
		StringBuilder leftB = new StringBuilder();
		StringBuilder opB = new StringBuilder();
		StringBuilder rightB = new StringBuilder();
		StringBuilder opB2 = new StringBuilder();
		StringBuilder restB = new StringBuilder();

		StringBuilder sb = rightB;

		int bracketLevel = 0;
		
		char lastC = 0;

		for (int i = text.length() - 1; i >= 0; i--) {
			char c = text.charAt(i);

			switch (c) {
				case ')':
					bracketLevel++;
					break;
			}

			if (bracketLevel == 0) {
				//only allow sb switch when we are not inside a bracket
				if (sb == rightB || sb == leftB) {
					if (!LangConstants.isAllowedNameChar(c)) {
						if (sb == leftB) {
							//try detect new operator
							//line.throwException("Illegal character: " + c);
							sb = opB2;
						} else {
							sb = opB;
						}
					}
				} else {
					//opB
					if (LangConstants.isAllowedNameChar(c)) {
						if (sb == opB) {
							sb = leftB;
						} else {
							//reached second operator
							sb = restB;
						}
					}
					else if (lastC == '!' || lastC == '-') {
						//negation or not have to be separated from other ops
						if (sb == opB) {
							sb = opB2;
						}
					}
				}
			} else {
				if (sb == opB) {
					sb = leftB;
				} else if (sb == opB2) {
					sb = restB;
				}
			}

			switch (c) {
				case '(':
					bracketLevel--;
					if (bracketLevel < 0) {
						line.throwException("Unclosed bracket! - expression " + text);
					}
					break;
			}

			sb.append(c);
			lastC = c;
		}
		if (bracketLevel > 0) {
			line.throwException("Unopened bracket! - expression " + text);
		}

		rightB.reverse();
		opB.reverse();
		leftB.reverse();
		opB2.reverse();
		restB.reverse();

		String rightS = rightB.toString();
		String opS = opB.toString();
		String opS2 = opB2.toString();
		String leftS = leftB.toString();
		String restS = restB.toString();
		boolean hasOperator = !opS.isEmpty();
		boolean hasOperator2 = !opS2.isEmpty();
		boolean hasLHS = !leftS.isEmpty();
		boolean hasRHS = !rightS.isEmpty();
		boolean hasRest = !restS.isEmpty();
		boolean isRHSExpr = true;

		if (opS.endsWith("-") && opS.length() > 1) {
			char charAtBefore = opS.charAt(opS.length() - 2);
			if ((charAtBefore != '-') || hasRHS) { //can't be -- if there is a right side operand
				//this is a NEG wrongly merged with another op
				restS = restS + opS2 + leftS;
				hasRest = !restS.isEmpty();
				hasLHS = false;
				opS2 = opS.substring(0, opS.length() - 1);
				opS = "-";
				leftS = "";
				hasOperator2 = true;
			}
		}

		op = null;
		boolean validOp = true;
		if (hasOperator) {
			op = Operator.create(opS, line);
			if (op != null) {
				if (op instanceof CmnMathOp.Sub) {
					if (!hasLHS) {
						op = new Neg();
					}
				}

				if (hasOperator2) {
					//the order is rest + op2 + left + op1 + right
					Operator op2 = Operator.create(opS2, line);
					if (op2 != null) {
						if (op2 instanceof CmnMathOp.Sub) {
							if (!hasRest) {
								op2 = new Neg();
							}
						}
						if (op.getPriority().ordinal() > op2.getPriority().ordinal()) {
							//set right to left + op1 + right
							//set op to op2
							//set left to rest
							rightS = leftS + opS + rightS;
							op = op2;
							leftS = restS;
							hasLHS = hasRest;
							hasRest = false;
						} else {
							//set left to rest + op2 + left
							leftS = restS + opS2 + leftS;
							hasRest = false;
							hasLHS = true;
						}
					}
				}

				if (!op.getHasOnlyRHS()) {
					if (!hasLHS) {
						line.throwException("Incomplete expression - operator " + opS + " (" + op.getClass().getSimpleName() + ") requires a left hand side operand.");
					}
				} else {
					if (hasLHS) {
						line.throwException("Operator " + opS + "can not have a left hand side operand.");
					}
				}
				if (!op.getHasOnlyLHS()) {
					if (!hasRHS) {
						line.throwException("Incomplete expression - operator " + opS + " requires a right hand side operand.");
					}
				} else {
					if (hasRHS) {
						line.throwException("Operator " + opS + "can not have a right hand side operand.");
					}
				}
			} else {
				validOp = false;
			}
		} else if (hasRHS) {
			//First, let's check for type casts. Those have only one possible format - a (type) at the beginning of the expression
			//It probably does no harm to just force-decode them through iteration
			isRHSExpr = false;
			DataType castType = DataType.getTypeCast(rightS);
			if (castType != null) {
				op = new TypeCast(castType.typeDef());

				//weird, but will work to get the cast end
				String uncasted = rightS.substring(rightS.indexOf(')') + 1);
				right = new NExpression(uncasted, line, cg).toThroughput(cg);
			} else {
				//No cast and...

				//No operator and no left hand side => constant value
				//That can be either:
				//1) a number
				//2) a variable
				//3) a call
				right = DataType.getThroughput(rightS, cg);
				if (right == null) {
					//Not a number
					Variable locVar = cg.resolveLocal(rightS);
					if (locVar != null) {
						right = new VariableThroughput(locVar, cg);
					} else {
						Variable gVar = cg.resolveGVar(rightS);
						if (gVar != null) {
							right = new VariableThroughput(gVar, cg);
						} else {
							//needs args in braces
							int braceIdx = rightS.indexOf('(');
							if (braceIdx == -1) {
								for (Variable glb : cg.globals.variables) {
									//System.err.println("has glb " + glb.name + " / " + glb.aliases);
								}
								//System.out.println(cg.getCurrentMethod().def.name + " CUREMNTS line " + line.startingLine);
								for (Variable loc : cg.getCurrentMethod().locals) {
									//System.err.println("has loc " + loc.name);
								}
								line.throwException("Unresolved variable: " + rightS);
							} else {
								String methodName = rightS.substring(0, braceIdx);
								BraceContent bc = Preprocessor.getContentInBraces(rightS, braceIdx);
								String[] argCmds = bc.getContentInBraces().split(",(?=[^\\)]*(?:\\(|$))"); //https://stackoverflow.com/questions/732029/how-to-split-string-by-unless-is-within-brackets-using-regex
								List<Throughput> args = new ArrayList<>();
								for (int i = 0; i < argCmds.length; i++) {
									String ac = argCmds[i].trim();
									if (!ac.isEmpty()) {
										args.add(new NExpression(argCmds[i], line, cg).toThroughput(cg));
									}
								}
								OutboundDefinition def = new OutboundDefinition(methodName, args.toArray(new Throughput[args.size()]));
								InboundDefinition m = cg.resolveMethod(def);

								if (m != null) {
									if (m.isNameAbsolute) {
										def.name = m.name;
									}
									ALocalCall ins;
									ins = cg.getMethodCall(def, m);
									right = new Throughput(m.retnType, ArraysEx.asList(ins));
								} else {
									line.throwException("Unresolved symbol: " + rightS);
								}
							}
						}
					}
				}
			}
		}

		if (!line.exceptions.isEmpty()) {
			left = null;
			right = null;
		} else if (validOp) {
			if (hasLHS) {
				left = new NExpression(leftS, line, cg).toThroughput(cg);
			}
			if (hasRHS && isRHSExpr) {
				right = new NExpression(rightS, line, cg).toThroughput(cg);
			}

			if (op instanceof Neg) {
				//Merge negations into -(immediate constants) where possible
				if (right != null && right.isImmediateConstant()) {
					op = null;
					int imm = right.getImmediateValue();
					if (right.type.baseType == DataType.FLOAT && !cg.provider.getFloatingPointHandler().isFixedPoint()) {
						imm ^= 0x80000000;
					} else {
						imm = -imm;
					}
					right.setImmediateConstantValue(imm);
				}
			}
		}
	}

	public Throughput toThroughput(NCompileGraph cg) {
		if (op != null && (right != null || left != null) && op.checkCast(left, right, line)) {
			List<AInstruction> il = op.getOperation(left, right, line, cg);
			Throughput out = new Throughput(op.getOutputType(), il);
			return out;
		} else {
			return getDefaultThroughput();
		}
	}

	private Throughput getDefaultThroughput() {
		if (left != null) {
			return left; //should not happen though
		}
		return right;
	}

	private static String getTextWithoutStartEndPairedBrackets(String text) {
		int trimSize = 0;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c != '(') {
				break;
			} else {
				int bl = 1;
				int endBracketIdx = i + 1;
				for (; endBracketIdx < text.length(); endBracketIdx++) {
					char c2 = text.charAt(endBracketIdx);
					switch (c2) {
						case '(':
							bl++;
							break;
						case ')':
							bl--;
							break;
					}
					if (bl == 0) {
						break;
					}
				}
				if (bl == 0) {
					if (endBracketIdx == text.length() - 1 - i) {
						trimSize++;
						continue;
					}
				}
				break;
			}
		}
		return text.substring(trimSize, text.length() - trimSize);
	}
}
