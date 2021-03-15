package ctrmap.pokescript.stage0;

/**
 *
 */
public enum Modifier {
	PUBLIC("public"),
	NATIVE("native"),
	STATIC("static"),
	FINAL("final"),
	VARIABLE,
	CLASSDEF;

	private String name;

	private Modifier() {
		this(null);
	}

	private Modifier(String name) {
		this.name = name;
	}

	public static Modifier fromName(String name) {
		for (Modifier m : values()) {
			if (name.equals(m.name)) {
				return m;
			}
		}
		return null;
	}
}
