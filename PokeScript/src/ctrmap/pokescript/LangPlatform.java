
package ctrmap.pokescript;

import xstandard.gui.file.ExtensionFilter;

public enum LangPlatform {
	AMX_CTR("ctr", LangConstants.LANG_BINARY_EXTENSION_FILTER_PAWN),
	AMX_NX("nx", LangConstants.LANG_BINARY_EXTENSION_FILTER_PAWN),
	EV_PL("ntrpl", LangConstants.LANG_BINARY_EXTENSION_FILTER_NTR),
	EV_SWAN("ntrv", LangConstants.LANG_BINARY_EXTENSION_FILTER_NTR);
	
	public final String name;
	public final ExtensionFilter extensionFilter;
	
	private LangPlatform(String plafId, ExtensionFilter extensionFilter){
		this.name = plafId;
		this.extensionFilter = extensionFilter;
	}
	
	public static LangPlatform fromName(String str){
		for (LangPlatform p : values()){
			if (p.name.equals(str)){
				return p;
			}
		}
		return null;
	}
	
	public static LangPlatform fromEnumName(String str){
		for (LangPlatform p : values()){
			if (p.toString().equals(str)){
				return p;
			}
		}
		return null;
	}
}
