package ctrmap.pokescript.util;

import java.util.List;

public class Token<T> {
	
	private final StringBuilder content = new StringBuilder();
	public final T type;

	public Token(T type) {
		this.type = type;
	}

	void append(char c) {
		content.append(c);
	}

	public String getContent() {
		return content.toString();
	}
	
	public static String join(List<? extends Token> tokens) {
		StringBuilder sb = new StringBuilder();
		for (Token t : tokens) {
			sb.append(t.content);
		}
		return sb.toString();
	}
	
	@Override
	public String toString() {
		return "[" + type + "] " + getContent().trim();
	}
}
