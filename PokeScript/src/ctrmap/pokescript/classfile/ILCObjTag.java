package ctrmap.pokescript.classfile;

import java.util.Objects;

public enum ILCObjTag {
	ILCF_START("ILCF"),
	ILCF_END("TERM"),
	
	METHOD("MTHD"),
	TYPEDEF_REFERENCE("TREF"),
	
	CODE("CODE")
	;
	
	public final String fourcc;
	
	private static final ILCObjTag[] VALUES = values();
	
	private ILCObjTag(String fourcc) {
		this.fourcc = fourcc;
	}
	
	public static ILCObjTag identify(String fourcc) {
		for (ILCObjTag v : VALUES) {
			if (Objects.equals(v.fourcc, fourcc)) {
				return v;
			}
		}
		return null;
	}
}
