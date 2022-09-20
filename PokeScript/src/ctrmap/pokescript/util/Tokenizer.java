package ctrmap.pokescript.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Tokenizer {
	
	public static <T> List<Token<T>> tokenize(String s, Recognizer<T> recognizer) {
		List<Token<T>> tokens = new ArrayList<>();
		T type = null;
		boolean typeIsRepeatable = true;
		Token<T> currentToken = new Token<>(null);
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			ICharRecognizer<T> newRecognizer = recognizer.recognize(c);
			T newType = newRecognizer.getType();
			if (newType != type || !typeIsRepeatable) {
				if (type != null) {
					tokens.add(currentToken);
				}
				type = newType;
				typeIsRepeatable = newRecognizer.isRepeatable();
				currentToken = new Token<>(type);
			}
			currentToken.append(c);
		}
		if (currentToken.type != null) {
			tokens.add(currentToken);
		}
		return tokens;
	}
	
	
	public static class Recognizer<T> {
		
		private final ICharRecognizer<T>[] charRecognizers;
		
		public Recognizer(ICharRecognizer<T>... charsRecognizers) {
			this.charRecognizers = charsRecognizers;
		}
		
		public ICharRecognizer<T> recognize(char c) {
			for (ICharRecognizer<T> m : charRecognizers) {
				if (m.recognize(c)) {
					return m;
				}
			}
			throw new RuntimeException("Could not recognize character " + c);
		}
	}
	
	public static interface ICharRecognizer<T> {
		public boolean recognize(char c);
		public boolean isRepeatable();
		
		public T getType();
	}
	
	public static class CharFallbackMapping<T> extends CharFunctionMapping<T> {

		public CharFallbackMapping(T type) {
			super(type, true);
		}

		@Override
		public boolean recognize(char c) {
			return true;
		}
	}
	
	public static abstract class CharFunctionMapping<T> implements ICharRecognizer<T> {
		
		private final T type;
		private final boolean isRepeatable;
		
		public CharFunctionMapping(T type, boolean isRepeatable) {
			this.type = type;
			this.isRepeatable = isRepeatable;
		}
		
		@Override
		public T getType() {
			return type;
		}

		@Override
		public boolean isRepeatable() {
			return isRepeatable;
		}
	}
	
	public static class CharMapping<T> implements ICharRecognizer<T> {
		
		private final T type;
		private final char[] characters;
		
		public CharMapping(T type, char... characters) {
			this.type = type;
			this.characters = characters.clone();
			Arrays.sort(this.characters);
		}
		
		public boolean hasChar(char c) {
			return Arrays.binarySearch(characters, c) >= 0;
		}
		
		@Override
		public T getType() {
			return type;
		}

		@Override
		public boolean recognize(char c) {
			return hasChar(c);
		}

		@Override
		public boolean isRepeatable() {
			return false;
		}
	}
}
