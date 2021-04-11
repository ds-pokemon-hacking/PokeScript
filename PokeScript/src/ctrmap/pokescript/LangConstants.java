package ctrmap.pokescript;

import ctrmap.stdlib.gui.file.ExtensionFilter;
import java.util.Arrays;
import java.util.List;

public class LangConstants {
	public static final ExtensionFilter LANG_SOURCE_FILE_EXTENSION_FILTER = new ExtensionFilter("PokéScript source", "*.pks");
	public static final ExtensionFilter LANG_NATIVE_DEFINITION_EXTENSION_FILTER = new ExtensionFilter("PokéScript native definition header", "*.nd");
	public static final ExtensionFilter LANG_COMPILABLES_FILTER_COMB = ExtensionFilter.combine(LANG_SOURCE_FILE_EXTENSION_FILTER, LANG_NATIVE_DEFINITION_EXTENSION_FILTER);
	public static final ExtensionFilter LANG_LIBRARY_EXTENSION_FILTER = new ExtensionFilter("PokéScript Library", "*.lib");
	
	public static final ExtensionFilter LANG_BINARY_EXTENSION_FILTER_CTR = new ExtensionFilter("Abstract Machine Executable", "*.amx");
	public static final ExtensionFilter LANG_BINARY_EXTENSION_FILTER_NTR = new ExtensionFilter("Event binary", "*.ev");
	
	public static final String NATIVE_DEFINITION_EXTENSION = ".nd";
	public static final String LANG_SOURCE_FILE_EXTENSION = ".pks";
	
	public static final char CH_PP_KW_IDENTIFIER = '#';
	public static final char CH_ANNOT_KW_IDENTIFIER = '@';
	
	public static final Character[] COMMON_LINE_TERM = new Character[]{';', '{', '}', ':'};
	
	public static final String MAIN_METHOD_NAME = "main";
	
	public static final List<Character> allowedNonAlphaNumericNameCharacters = Arrays.asList(new Character[]{
		'.',
		'_',
		'-'
	});
	
	public static boolean isLangFile(String fileName){
		return LANG_COMPILABLES_FILTER_COMB.accepts(fileName);
	}
	
	public static boolean isLangLib(String fileName){
		return LANG_LIBRARY_EXTENSION_FILTER.accepts(fileName);
	}
}
