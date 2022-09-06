package ctrmap.pokescript.expr.ast;

import ctrmap.pokescript.LangConstants;

public enum ASTContentType {
	GAP,
	SYMBOL(true),
	BRACKET_START,
	BRACKET_END,
	ARRAY_BRACKET_START,
	ARRAY_BRACKET_END,
	COMMA,
	OP_CHAR;
	public final boolean repeatableChars;

	private ASTContentType() {
		this(false);
	}

	private ASTContentType(boolean repeatableChars) {
		this.repeatableChars = repeatableChars;
	}

	public static ASTContentType getCharTokenType(char c) {
		switch (c) {
			case ' ':
				return ASTContentType.GAP;
			case '(':
				return ASTContentType.BRACKET_START;
			case ')':
				return ASTContentType.BRACKET_END;
			case '[':
				return ASTContentType.ARRAY_BRACKET_START;
			case ']':
				return ASTContentType.ARRAY_BRACKET_END;
			case ',':
				return ASTContentType.COMMA;
		}
		if (LangConstants.isAllowedNameChar(c)) {
			return ASTContentType.SYMBOL;
		}
		return ASTContentType.OP_CHAR;
	}
}
