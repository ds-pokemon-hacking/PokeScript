package ctrmap.pokescript.expr.ast;

import ctrmap.pokescript.expr.OperatorType;
import ctrmap.pokescript.expr.Operator;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ASTOperatorMatcher {

	final Class<? extends Operator> cls;
	public final ASTContentType[] types;
	public final String[] contents;
	public final OperatorType type;

	public ASTOperatorMatcher(Class<? extends Operator> cls, String... contents) {
		this(cls, OperatorType.BINARY, contents);
	}

	public ASTOperatorMatcher(Class<? extends Operator> cls, OperatorType type, String... contents) {
		this.cls = cls;
		this.contents = contents;
		types = new ASTContentType[contents.length];
		for (int i = 0; i < types.length; i++) {
			types[i] = ASTContentType.OP_CHAR;
		}
		this.type = type;
	}
	
	public int getEvalStartIndexOffs() {
		return 0;
	}
	
	public ASTNode makeNode(String[] actualContent) {
		return new ASTOperatorNode(instantiate(actualContent));
	}
	
	public boolean matchType(AST ast, int index, ASTContentType type) {
		ASTContentType t = types[index];
		return type == null || type == t;
	}
	
	public boolean matchContent(AST ast, int index, String input) {
		String cnt = contents[index];
		return cnt == null || (cnt.equals(input));
	}

	public ASTOperatorMatcher(Class<? extends Operator> cls, OperatorType opType, ASTContentType type, String content) {
		this.cls = cls;
		this.type = opType;
		this.types = new ASTContentType[]{type};
		this.contents = new String[]{content};
	}

	protected ASTOperatorMatcher(Class<? extends Operator> cls, OperatorType opType, ASTContentType[] types, String[] contents) {
		this.cls = cls;
		this.type = opType;
		this.types = types;
		this.contents = contents;
	}

	public int getRecognitionSize() {
		return types.length;
	}

	public int getStepSize() {
		return contents.length;
	}

	public Operator instantiate(String[] actualContent) {
		try {
			return cls.newInstance();
		} catch (InstantiationException | IllegalAccessException ex) {
			Logger.getLogger(AST.class.getName()).log(Level.SEVERE, null, ex);
		}
		return null;
	}

}
