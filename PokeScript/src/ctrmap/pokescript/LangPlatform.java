
package ctrmap.pokescript;

import ctrmap.stdlib.gui.file.ExtensionFilter;

public enum LangPlatform {
	AMX_CTR("ctr", LangConstants.LANG_BINARY_EXTENSION_FILTER_CTR),
	AMX_NX("nx", LangConstants.LANG_BINARY_EXTENSION_FILTER_NTR),
	EV_PL("ntrpl", LangConstants.LANG_BINARY_EXTENSION_FILTER_NTR),
	EV_WB("ntrwb", LangConstants.LANG_BINARY_EXTENSION_FILTER_NTR),
	EV_SWAN("ntrswan", LangConstants.LANG_BINARY_EXTENSION_FILTER_NTR);
	
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
}
