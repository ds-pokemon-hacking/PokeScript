package ctrmap.pokescript.expr.ast;

import ctrmap.pokescript.util.Tokenizer;
import ctrmap.pokescript.LangConstants;

public enum ASTContentType {
	INVALID,
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

	public static Tokenizer.Recognizer<ASTContentType> RECOGNIZER = new Tokenizer.Recognizer<>(
			new Tokenizer.CharMapping<>(BRACKET_START, '('),
			new Tokenizer.CharMapping<>(BRACKET_END, ')'),
			new Tokenizer.CharMapping<>(ARRAY_BRACKET_START, '['),
			new Tokenizer.CharMapping<>(ARRAY_BRACKET_END, ']'),
			new Tokenizer.CharMapping<>(COMMA, ','),
			
			//Gap
			new Tokenizer.CharFunctionMapping<ASTContentType>(null, true) {
		@Override
		public boolean recognize(char c) {
			return Character.isWhitespace(c);
		}
	},
			new Tokenizer.CharFunctionMapping<ASTContentType>(SYMBOL, true) {
		@Override
		public boolean recognize(char c) {
			return LangConstants.isAllowedNameChar(c);
		}
	},
			//Default - operator character
			new Tokenizer.CharFunctionMapping<ASTContentType>(OP_CHAR, false) {
		@Override
		public boolean recognize(char c) {
			return true;
		}
	}
	);
}
