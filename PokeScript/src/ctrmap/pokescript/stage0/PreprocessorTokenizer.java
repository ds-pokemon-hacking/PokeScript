package ctrmap.pokescript.stage0;

import ctrmap.pokescript.util.TokenSlicer;
import ctrmap.pokescript.util.Tokenizer;
import java.util.List;

public class PreprocessorTokenizer {

	public static final Tokenizer.Recognizer<TokenType> RECOGNIZER = new Tokenizer.Recognizer<>(
			new Tokenizer.CharMapping<>(null, '\r'), //remove Windows carriage return
			new Tokenizer.CharMapping<>(TokenType.PREPROCESSOR_MACRO_START, '#'),
			new Tokenizer.CharMapping<>(TokenType.NEWLINE, '\n'),
			new Tokenizer.CharMapping<>(TokenType.SLASH, '/'),
			new Tokenizer.CharMapping<>(TokenType.BACKSLASH, '\\'),
			new Tokenizer.CharMapping<>(TokenType.ASTERISK, '*'),

			//default outcome
			new Tokenizer.CharFallbackMapping<>(TokenType.CODE)
	);

	public static final TokenSlicer<TokenType, BlockType> SLICER = new TokenSlicer<>(
			new TokenSlicer.BlockPattern<>(
					BlockType.COMMENT,
					new TokenType[]{TokenType.SLASH, TokenType.SLASH},
					new TokenType[]{TokenType.NEWLINE},
					null
			),
			new TokenSlicer.BlockPattern<>(
					BlockType.COMMENT,
					new TokenType[]{TokenType.SLASH, TokenType.ASTERISK},
					new TokenType[]{TokenType.ASTERISK, TokenType.SLASH},
					null
			),
			new TokenSlicer.BlockPattern<>(
					BlockType.PREPROCESSOR_DIRECTIVE,
					new TokenType[]{TokenType.PREPROCESSOR_MACRO_START},
					new TokenType[]{TokenType.NEWLINE},
					TokenType.BACKSLASH
			),
			new TokenSlicer.BlockPattern<>(
					BlockType.CODE,
					null
			)
	);
	
	public static List<TokenSlicer.Block<TokenType, BlockType>> slice(String code) {
		return SLICER.slice(Tokenizer.tokenize(code, RECOGNIZER));
	}

	public static enum TokenType {
		PREPROCESSOR_MACRO_START,
		WHITESPACE,
		NEWLINE,
		SLASH,
		BACKSLASH,
		ASTERISK,
		CODE
	}

	public static enum BlockType {
		COMMENT,
		PREPROCESSOR_DIRECTIVE,
		CODE
	}
}
