package ctrmap.pokescript.stage0;

/**
 *
 */
public enum Modifier {
	PUBLIC("public", ModifierTarget.METHOD, ModifierTarget.VAR, ModifierTarget.CLASS, ModifierTarget.ENUM),
	INTERNAL("internal", ModifierTarget.METHOD, ModifierTarget.VAR),
	NATIVE("native", ModifierTarget.METHOD),
	STATIC("static", ModifierTarget.METHOD, ModifierTarget.VAR),
	FINAL("final", ModifierTarget.METHOD, ModifierTarget.VAR, ModifierTarget.ARG),
	META("meta", ModifierTarget.METHOD),
	VAR("var", ModifierTarget.ARG),
	BITFLAG("bitflags", ModifierTarget.ENUM),
	
	//internal metamodifiers
	VARIABLE,
	CLASSDEF,
	ENUMDEF;

	public static final Modifier[] MODIFIERS_ALL = values();
	
	public final String name;
	private ModifierTarget[] targets;

	private Modifier() {
		this(null, ModifierTarget.ARG, ModifierTarget.METHOD, ModifierTarget.VAR, ModifierTarget.CLASS, ModifierTarget.ENUM);
	}

	private Modifier(String name, ModifierTarget... targets) {
		this.name = name;
		this.targets = targets;
	}
	
	public boolean supportsTarget(ModifierTarget tgt){
		for (ModifierTarget t : targets){
			if (t == tgt){
				return true;
			}
		}
		return false;
	}

	public static Modifier fromName(String name) {
		for (Modifier m : values()) {
			if (name.equals(m.name)) {
				return m;
			}
		}
		return null;
	}
	
	public static enum ModifierTarget {
		METHOD,
		VAR,
		ARG,
		CLASS,
		ENUM
	}
}
