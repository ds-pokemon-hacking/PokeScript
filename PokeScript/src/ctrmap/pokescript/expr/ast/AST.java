package ctrmap.pokescript.expr.ast;

import ctrmap.pokescript.util.Tokenizer;
import ctrmap.pokescript.LangCompiler;
import ctrmap.pokescript.expr.OperatorType;
import ctrmap.pokescript.data.Variable;
import ctrmap.pokescript.expr.*;
import ctrmap.pokescript.stage0.EffectiveLine;
import ctrmap.pokescript.stage1.NCompilableMethod;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.pokescript.types.DataType;
import ctrmap.pokescript.types.classes.ClassDefinition;
import ctrmap.pokescript.util.Token;
import java.util.ArrayList;
import java.util.List;

public class AST {

	private static final ASTOperatorMatcher[] OPERATOR_MATCHERS = new ASTOperatorMatcher[]{
		//Math + math assignment
		new ASTMathOperatorMatcher(CmnMathOp.Add.class, "+"),
		new ASTMathOperatorMatcher(CmnMathOp.Add.class, "+", "="),
		new ASTMathOperatorMatcher(CmnMathOp.Add.class, "+", "+"),
		new ASTMathOperatorMatcher(CmnMathOp.Sub.class, "-"),
		new ASTMathOperatorMatcher(CmnMathOp.Sub.class, "-", "="),
		new ASTMathOperatorMatcher(CmnMathOp.Sub.class, "-", "-"),
		new ASTMathOperatorMatcher(CmnMathOp.Mul.class, "*"),
		new ASTMathOperatorMatcher(CmnMathOp.Mul.class, "*", "="),
		new ASTMathOperatorMatcher(CmnMathOp.Div.class, "/"),
		new ASTMathOperatorMatcher(CmnMathOp.Div.class, "/", "="),
		new ASTMathOperatorMatcher(CmnMathOp.Mod.class, "%"),
		new ASTMathOperatorMatcher(CmnMathOp.Mod.class, "%", "="),
		new ASTMathOperatorMatcher(CmnMathOp.BitAnd.class, "&"),
		new ASTMathOperatorMatcher(CmnMathOp.BitAnd.class, "&", "="),
		new ASTMathOperatorMatcher(CmnMathOp.BitOr.class, "|"),
		new ASTMathOperatorMatcher(CmnMathOp.BitOr.class, "|", "="),
		new ASTMathOperatorMatcher(CmnMathOp.Xor.class, "^"),
		new ASTMathOperatorMatcher(CmnMathOp.Xor.class, "^", "="),
		//Boolops
		new ASTMathOperatorMatcher(And.class, false, "&", "&"),
		new ASTMathOperatorMatcher(Or.class, false, "|", "|"),
		//Comparators
		new ASTOperatorMatcher(CmnCompOp.Less.class, "<"),
		new ASTOperatorMatcher(CmnCompOp.LessOrEqual.class, "<", "="),
		new ASTOperatorMatcher(CmnCompOp.Greater.class, ">"),
		new ASTOperatorMatcher(CmnCompOp.GreaterOrEqual.class, ">", "="),
		new ASTOperatorMatcher(Equals.class, "=", "="),
		new ASTOperatorMatcher(NotEqual.class, "!", "="),
		//Assignment
		new ASTOperatorMatcher(SetEquals.class, "="),
		//Unary operators
		new ASTOperatorMatcher(Not.class, OperatorType.UNARY, "!"),
		new ASTOperatorMatcher(Neg.class, OperatorType.UNARY, "-"),
		//new
		new ASTOperatorMatcher(InitHeapObj.class, OperatorType.UNARY, ASTContentType.SYMBOL, "new"),
		//comma
		new ASTOperatorMatcher(GroupingOperator.class, OperatorType.SEQUENTIAL, ASTContentType.COMMA, ","),
		//method call
		new ASTCallOperatorMatcher(),
		//type cast
		new ASTTypeCastOperatorMatcher(),
		new ASTArrayOperatorMatcher()
	};

	private static int OPERATOR_MATCHERS_TOKEN_MAX = 0;

	static {
		int max = 0;
		for (ASTOperatorMatcher m : OPERATOR_MATCHERS) {
			if (m.contents.length > max) {
				max = m.contents.length;
			}
		}
		OPERATOR_MATCHERS_TOKEN_MAX = max;
	}

	private List<Token<ASTContentType>> tokens = new ArrayList<>();

	private ASTNode root = new ASTOperatorNode(null);

	private EffectiveLine line;
	private NCompileGraph cg;
	private NCompilableMethod method;

	public AST(EffectiveLine line, NCompileGraph cg, String s) {
		this.cg = cg;
		this.line = line;
		if (cg.hasMethods()) {
			method = cg.getCurrentMethod();
		}
		tokenize(s);
		root = buildTree(makeNodes());
	}

	public Throughput toThroughput() {
		analyze();
		simplify();
		//System.out.println(printNodeTree());
		if (root != null) {
			return root.toThroughput(cg);
		}
		return null;
	}

	public void throwException(String text) {
		if (line != null) {
			line.throwException(text);
		} else {
			System.err.println(text);
		}
	}

	public Variable resolveGVar(String name) {
		return cg.resolveGVar(name);
	}

	public Variable resolveLocal(String name) {
		if (method == null) {
			return null;
		}
		return method.locals.getVariable(name);
	}

	public boolean isTypeName(String content) {
		DataType t = DataType.fromName(content);
		if (t != null) {
			return true;
		}
		if (cg != null) {
			for (ClassDefinition cls : cg.classDefs) {
				if (cls.hasName(content)) {
					return true;
				}
			}
		}
		return false;
	}

	public EffectiveLine getLine() {
		return line;
	}

	public void analyze() {
		if (root != null) {
			root.analyze(this);
		}
	}

	public void simplify() {
		if (root != null) {
			root.simplify(this);
		}
	}

	public static void main(String[] args) {
		//AST ast = new AST(null, new NCompileGraph(new LangCompiler.CompilerArguments()), "i = (new i + j) * -4096 + test(i = 4 * 4, i / (69f * -420f), 3(4), true && false)");
		//AST ast = new AST(null, new NCompileGraph(new LangCompiler.CompilerArguments()), "i = test[0][usu(2, 3)][2]");
		AST ast = new AST(null, new NCompileGraph(new LangCompiler.CompilerArguments()), "(boolean)Test.Test2()");
		ast.analyze();
		ast.simplify();
		System.out.println(ast.printTokens());
		System.out.println(ast.printNodeTree());
	}

	private void tokenize(String s) {
		tokens = Tokenizer.tokenize(s, ASTContentType.RECOGNIZER);
	}

	private boolean isCharTypeUnaryOnlyAfter(ASTContentType type) {
		if (type == null) {
			return false;
		}
		switch (type) {
			case SYMBOL:
			case BRACKET_END:
			case ARRAY_BRACKET_END:
				return false;
		}
		return true;
	}

	private List<ASTNode> makeNodes() {
		int tokenOffs = 0;
		ASTNode lastNode = null;
		List<ASTNode> nodes = new ArrayList<>();
		while (tokenOffs < tokens.size()) {
			tokenOffs = parseNode(nodes, tokenOffs, (lastNode != null && isCharTypeUnaryOnlyAfter(lastNode.charType)));
			lastNode = getLastParsedNode(nodes);
		}
		return nodes;
	}

	private ASTNode getLastParsedNode(List<ASTNode> nodes) {
		if (nodes.isEmpty()) {
			return null;
		}
		return nodes.get(nodes.size() - 1);
	}

	private int findClosingBracketIndex(List<ASTNode> queue, int startIndex) {
		int lv = 0;
		for (int i = startIndex + 1; i < queue.size(); i++) {
			switch (queue.get(i).charType) {
				case BRACKET_START:
				case ARRAY_BRACKET_START:
					lv++;
					break;
				case BRACKET_END:
				case ARRAY_BRACKET_END:
					lv--;
					if (lv < 0) {
						return i;
					}
					break;
			}
		}
		return -1;
	}

	private static boolean isTypeBracketStart(ASTContentType type) {
		switch (type) {
			case BRACKET_START:
			case ARRAY_BRACKET_START:
				return true;
		}
		return false;
	}

	private static boolean isTypeBracketEnd(ASTContentType type) {
		switch (type) {
			case BRACKET_END:
			case ARRAY_BRACKET_END:
				return true;
		}
		return false;
	}

	private static boolean isTypeBracket(ASTContentType type) {
		switch (type) {
			case BRACKET_END:
			case ARRAY_BRACKET_END:
			case BRACKET_START:
			case ARRAY_BRACKET_START:
				return true;
		}
		return false;
	}

	private ASTNode buildTree(List<ASTNode> queue) {
		Operator.Priority[] priorities = Operator.Priority.values();

		int start = 0;
		int end = queue.size();

		for (; start < (end - 1); start++, end--) {
			ASTNode l = queue.get(start);
			ASTNode r = queue.get(end - 1);
			if (!isTypeBracketStart(l.charType) || !isTypeBracketEnd(r.charType) || findClosingBracketIndex(queue, start) != (end - 1)) {
				break;
			}
		}

		//System.out.println("parsing queue " + queue + " start " + start + " end " + end);
		for (int i = 0; i < priorities.length; i++) {
			Operator.Priority p = priorities[i];
			//System.out.println("prio " + p);
			int blevel = 0;
			for (int nIdx = end - 1; nIdx >= start; nIdx--) {
				ASTNode node = queue.get(nIdx);
				if (node.charType != null) {
					switch (node.charType) {
						case OP_CHAR: {
							if (blevel == 0) {
								Operator op = ((ASTOperatorNode) node).operator;
								if (op.getPriority() == p && op.getType() == OperatorType.BINARY_INV) {
									//System.out.println("op " + op + " type " + op.getType());
									ASTNode lhs = buildTree(queue.subList(start, nIdx));
									ASTNode rhs = buildTree(queue.subList(nIdx + 1, end));
									node.addChild(lhs);
									node.addChild(rhs);
									return node;
								}
							}
							break;
						}
						case BRACKET_START:
						case ARRAY_BRACKET_START:
							blevel--;
							break;
						case BRACKET_END:
						case ARRAY_BRACKET_END:
							blevel++;
							break;
						default:
							break;
					}
				}
			}
			blevel = 0;
			for (int nIdx = start; nIdx < end; nIdx++) {
				ASTNode node = queue.get(nIdx);
				if (node.charType != null) {
					switch (node.charType) {
						case OP_CHAR: {
							if (blevel == 0) {
								Operator op = ((ASTOperatorNode) node).operator;
								if (op.getPriority() == p && op.getType() != OperatorType.BINARY_INV) {
									//System.out.println("op " + op + " type " + op.getType());
									ASTNode lhs = null;
									if (op.getType() != OperatorType.UNARY) {
										lhs = buildTree(queue.subList(start, nIdx));
									}
									ASTNode rhs = buildTree(queue.subList(nIdx + 1, end));
									node.addChild(lhs);
									node.addChild(rhs);
									if (op.getType() == OperatorType.SEQUENTIAL) {
										List<ASTNode> newChildrenParents = new ArrayList<>();
										for (ASTNode child : node) {
											if (child.charType == ASTContentType.OP_CHAR && ((ASTOperatorNode) child).operator.getType() == OperatorType.SEQUENTIAL) {
												newChildrenParents.add(child);
											}
										}
										for (ASTNode ncp : newChildrenParents) {
											node.removeChild(ncp);
											for (ASTNode ch : ncp) {
												node.addChild(ch);
											}
										}
										return node;
									} else {
										return node;
									}
								}
							}
							break;
						}
						case BRACKET_START:
						case ARRAY_BRACKET_START:
							blevel++;
							break;
						case BRACKET_END:
						case ARRAY_BRACKET_END:
							blevel--;
							break;
						default:
							break;
					}
				}
			}
		}

		int actQueueSize = end - start;

		switch (actQueueSize) {
			default:
			case 1:
				if (actQueueSize > 1) {
					System.out.println("Operator expected!! at index " + end + " of queue " + queue);
				}
				return queue.get(start);
			case 0:
				return null;
		}
	}

	private boolean matchTokenTypes(ASTOperatorMatcher matcher, int offs) {
		offs += matcher.getEvalStartIndexOffs();
		int size = matcher.getRecognitionSize();
		if (tokens.size() < size + offs) {
			return false;
		}
		for (int inTokenIdx = offs, matcherTokenIdx = 0; matcherTokenIdx < size; inTokenIdx++, matcherTokenIdx++) {
			if (!matcher.matchType(this, matcherTokenIdx, inTokenIdx >= 0 ? tokens.get(inTokenIdx).type : ASTContentType.INVALID)) {
				return false;
			}
		}
		return true;
	}

	private boolean matchTokenContents(ASTOperatorMatcher matcher, int offs) {
		offs += matcher.getEvalStartIndexOffs();
		int conLen = matcher.getStepSize();
		if (tokens.size() < conLen + offs) {
			return false;
		}
		for (int i = offs, j = 0; j < conLen; i++, j++) {
			//null = allow any content
			if (!matcher.matchContent(this, j, i >= 0 ? tokens.get(i).getContent() : "")) {
				return false;
			}
		}
		return true;
	}

	private String[] getTokenContents(int offs, int count) {
		String[] arr = new String[count];
		for (int i = offs, j = 0; j < count; i++, j++) {
			arr[j] = i >= 0 ? tokens.get(i).getContent() : "";
		}
		return arr;
	}

	private int parseNode(List<ASTNode> nodes, int tokenOffs, boolean unaryOnly) {
		//System.out.println("parse " + tokens.get(tokenOffs).content + " unaryOnly " + unaryOnly);
		for (int i = OPERATOR_MATCHERS_TOKEN_MAX; i >= 1; i--) {
			for (ASTOperatorMatcher m : OPERATOR_MATCHERS) {
				if (!unaryOnly || m.type == OperatorType.UNARY) {
					//System.out.println("unary " + m.cls);
					if (m.getRecognitionSize() == i) {
						//System.out.println("try " + m.cls);
						if (matchTokenTypes(m, tokenOffs)) {
							if (matchTokenContents(m, tokenOffs)) {
								ASTNode op = m.makeNode(getTokenContents(tokenOffs + m.getEvalStartIndexOffs(), m.contents.length));
								nodes.add(op);
								return tokenOffs + m.getStepSize() + m.getEvalStartIndexOffs();
							}
						}
					}
				}
			}
		}
		Token<ASTContentType> t = tokens.get(tokenOffs);
		if (t.type == ASTContentType.OP_CHAR) {
			//throw invalid operator error
			throwException("Unexpected operator " + t.getContent());
			return tokenOffs + 1;
		}
		ASTOperandNode op = new ASTOperandNode(t.type, t.getContent());
		nodes.add(op);
		return tokenOffs + 1;
	}

	public String printTokens() {
		StringBuilder sb = new StringBuilder();
		for (Token t : tokens) {
			sb.append("[");
			sb.append(t.type);
			sb.append("]");
			sb.append(t.getContent());
		}
		return sb.toString();
	}

	public String printNodeTree() {
		StringBuilder sb = new StringBuilder();
		printNodeTree(root, sb, 0);
		return sb.toString();
	}

	private void printNodeTree(ASTNode node, StringBuilder sb, int indentLevel) {
		String indentor = "";
		for (int i = 0; i < indentLevel; i++) {
			indentor += "    ";
		}
		sb.append(indentor);
		if (indentLevel != 0) {
			sb.append("└─ ");
		}
		sb.append(node);
		sb.append("\n");
		for (ASTNode child : node) {
			printNodeTree(child, sb, indentLevel + 1);
		}
	}

	@Override
	public String toString() {
		return printNodeTree();
	}
}
