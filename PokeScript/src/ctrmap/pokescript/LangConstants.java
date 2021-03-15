package ctrmap.pokescript;

import ctrmap.stdlib.gui.file.ExtensionFilter;
import java.util.Arrays;
import java.util.List;

public class LangConstants {
	public static final ExtensionFilter LANG_SOURCE_FILE_EXTENSION_FILTER = new ExtensionFilter("PokéScript source", "*.pks");
	public static final ExtensionFilter LANG_NATIVE_DEFINITION_EXTENSION_FILTER = new ExtensionFilter("PokéScript native definition header", "*.nd");
	public static final ExtensionFilter LANG_BINARY_EXTENSION_FILTER_CTR = new ExtensionFilter("Abstract Machine Executable", "*.amx");
	public static final ExtensionFilter LANG_BINARY_EXTENSION_FILTER_NTR = new ExtensionFilter("Event binary", "*.ev");
	
	public static final String NATIVE_DEFINITION_EXTENSION = ".nd";
	public static final String LANG_SOURCE_FILE_EXTENSION = ".pks";
	
	public static final String AMX_BINARY_EXTENSION = ".amx";
	public static final String AMX_BINARY_EXTENSION_NTR = ".evt";
	
	public static final char CH_PP_KW_IDENTIFIER = '#';
	public static final char CH_ANNOT_KW_IDENTIFIER = '@';
	public static final String KW_PP_DEFINE = "define";
	public static final String KW_PP_UNDEFINE = "undef";
	public static final String KW_PP_IF_DEFINED = "ifdef";
	public static final String KW_PP_IF_NOT_DEFINED = "ifndef";
	public static final String KW_PP_END_IF = "endif";
	public static final String KW_PP_ELSE = "else";
	
	public static final Character[] COMMON_LINE_TERM = new Character[]{';', '{', '}', ':'};
	
	public static final String KW_L_IMPORT = "import";
	public static final String KW_L_SWITCH = "switch";
	public static final String KW_L_CASE = "case";
	public static final String KW_L_CASE_DEFAULT = "default";
	public static final String KW_L_IF = "if";
	public static final String KW_L_WHILE = "while";
	public static final String KW_L_PAUSE = "pause";
	public static final String KW_L_BREAK = "break";
	public static final String KW_L_GOTO = "goto";
	public static final String KW_L_RETURN = "return";
	
	public static final String MAIN_METHOD_NAME = "main";
	
	public static final String MM_PUBLIC = "public";
	public static final String MM_NATIVE = "native";
	
	public static final List<Character> allowedNonAlphaNumericNameCharacters = Arrays.asList(new Character[]{
		'.',
		'_',
		'-'
	});
	
	public static boolean isLangFile(String fileName){
		return fileName.endsWith(NATIVE_DEFINITION_EXTENSION) || fileName.endsWith(LANG_SOURCE_FILE_EXTENSION);
	}
}
