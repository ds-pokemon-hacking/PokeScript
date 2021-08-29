package ctrmap.pokescript.stage0;

import ctrmap.pokescript.stage0.content.StatementContent;

/**
 *
 */
public enum CompilerPragma {
	OPTIMIZE_COUNT("optimize_count", PragmaType.INT),
	LEAK_FOR_DECL("leak_for_declaration", PragmaType.BOOLEAN),
	FUNCARG_BYTES_DEFAULT("funcarg_bytes_default", PragmaType.INT),
	ALLOW_UNSAFE_CASTS("unsafe_casts", PragmaType.BOOLEAN);

	public final String tag;
	public final PragmaType type;

	private CompilerPragma(String tag, PragmaType type) {
		this.tag = tag;
		this.type = type;
	}
	
	public static PragmaValue tryIdentifyPragma(StatementContent cnt, EffectiveLine line){
		if (cnt.arguments.isEmpty()){
			line.throwException("No pragma tag found.");
			return null;
		}
		else if (cnt.arguments.size() > 2){
			line.throwException("Too many pragma arguments.");
			return null;
		}
		String tagStr = cnt.arguments.get(0);
		for (CompilerPragma p : values()){
			if (p.tag.equals(tagStr)){
				PragmaValue val = new PragmaValue(p, cnt.arguments.size() > 1 ? cnt.arguments.get(1) : "", line);
				if (val.val != null){
					return val;
				}
			}
		}
		line.throwException("Unrecognized pragma: " + tagStr);
		return null;
	}

	public static class PragmaValue {

		public CompilerPragma pragma;
		public Object val;

		public PragmaValue(CompilerPragma pragma, String text, EffectiveLine line) {
			this.pragma = pragma;
			text = text.trim();

			if (text.isEmpty() && pragma.type != PragmaType.BOOLEAN) {
				line.throwException("Missing pragma value.");
			} else {
				switch (pragma.type) {
					case BOOLEAN:
						if (text.isEmpty()){
							val = true;
						}
						else if (text.equals(Boolean.FALSE.toString())) {
							val = false;
						}
						else if (text.equals(Boolean.TRUE.toString())){
							val = true;
						}
						else {
							line.throwException("Invalid boolean value for pragma " + pragma.tag + ": " + text);
						}
						break;
					case INT:
						try {
							val = Integer.parseInt(text);
						}
						catch (NumberFormatException ex){
							line.throwException("Invalid integer value for pragma " + pragma.tag + ": " + text);
						}
						break;
					case STRING:
						val = text;
						break;
				}
			}
		}

		public boolean boolValue() {
			return (boolean) val;
		}

		public int intValue() {
			return (int) val;
		}

		public String stringValue() {
			return String.valueOf(val);
		}
	}

	public static enum PragmaType {
		BOOLEAN,
		INT,
		STRING
	}
}
