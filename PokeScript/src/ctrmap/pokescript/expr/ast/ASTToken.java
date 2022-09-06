package ctrmap.pokescript.expr.ast;

public class ASTToken {

	public StringBuilder content = new StringBuilder();
	public ASTContentType type;

	public ASTToken(ASTContentType type) {
		this.type = type;
	}

	public void append(char c) {
		content.append(c);
	}

}
