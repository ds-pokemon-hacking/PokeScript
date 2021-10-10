package ctrmap.pokescript;

import ctrmap.stdlib.gui.file.ExtensionFilter;
import ctrmap.stdlib.util.ArraysEx;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LangConstants {

	public static final String LANG_GENERAL_HEADER_EXTENSION = ".h";
	public static final String LANG_NATIVE_DEFINITION_HEADER_EXTENSION = ".nd";
	
	public static final String[] LANG_HEADER_EXTENSIONS = new String[]{LANG_GENERAL_HEADER_EXTENSION, LANG_NATIVE_DEFINITION_HEADER_EXTENSION};
	public static final String LANG_SOURCE_FILE_EXTENSION = ".pks";

	public static final List<String> SUPPORTED_EXTENSIONS = new ArrayList<>();

	static {
		SUPPORTED_EXTENSIONS.addAll(ArraysEx.asList(LANG_HEADER_EXTENSIONS));
		SUPPORTED_EXTENSIONS.add(LANG_SOURCE_FILE_EXTENSION);
	}

	public static final ExtensionFilter LANG_SOURCE_FILE_EXTENSION_FILTER = new ExtensionFilter("PokéScript source", "*.pks");
	public static final ExtensionFilter LANG_HEADER_EXTENSION_FILTER = new ExtensionFilter("PokéScript header", "*.h", "*.nd");
	public static final ExtensionFilter LANG_COMPILABLES_FILTER_COMB = ExtensionFilter.combine(LANG_SOURCE_FILE_EXTENSION_FILTER, LANG_HEADER_EXTENSION_FILTER);
	public static final ExtensionFilter LANG_LIBRARY_EXTENSION_FILTER = new ExtensionFilter("PokéScript Library", "*.lib");

	public static final ExtensionFilter LANG_BINARY_EXTENSION_FILTER_CTR = new ExtensionFilter("Abstract Machine Executable", "*.amx");
	public static final ExtensionFilter LANG_BINARY_EXTENSION_FILTER_NTR = new ExtensionFilter("Event binary", "*.ev");

	public static final char CH_PP_KW_IDENTIFIER = '#';
	public static final char CH_ANNOT_KW_IDENTIFIER = '@';
	
	public static final char CH_COMMENT_START_CAND = '/';
	public static final char CH_COMMENT_ONELINE = '/';
	public static final char CH_COMMENT_BLOCK = '*';
	
	public static final char CH_ASSIGNMENT = '=';
	
	public static final char CH_METHOD_EXTENDS_IDENT = ':';
	public static final char CH_LABEL_IDENT = ':';
	
	public static final char CH_ELEMENT_SEPARATOR = ',';
	public static final char CH_STATEMENT_SEPARATOR = ';';
	public static final String CHSEQ_ELEMENT_SEPARATOR = ",";
	
	public static final char CH_PATH_SEPARATOR = '.';
	
	public static final String CHSEQ_COMMENT_TERM_ONELINE = "\n";
	public static final String CHSEQ_COMMENT_TERM_BLOCK = "*/";

	public static final Character[] COMMON_LINE_TERM = new Character[]{';', '{', '}', ':'};

	public static final String MAIN_METHOD_NAME = "main";

	public static final List<Character> allowedNonAlphaNumericNameCharacters = Arrays.asList(new Character[]{
		'.',
		'_'
	});
	
	public static boolean isAllowedNameChar(char c) {
		return Character.isLetterOrDigit(c) || allowedNonAlphaNumericNameCharacters.contains(c);
	}
	
	public static String getLastPathElem(String path) {
		if (path == null) {
			return null;
		}
		int didx = path.lastIndexOf('.');
		if (didx != -1) {
			return path.substring(didx + 1, path.length());
		}
		return path;
	}
	
	public static String makePath(String... elements) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < elements.length; i++) {
			if (i != 0) {
				sb.append(CH_PATH_SEPARATOR);
			}
			sb.append(elements[i]);
		}
		return sb.toString();
	}

	public static boolean isLangFile(String fileName) {
		return LANG_COMPILABLES_FILTER_COMB.accepts(fileName);
	}

	public static boolean isLangLib(String fileName) {
		return LANG_LIBRARY_EXTENSION_FILTER.accepts(fileName);
	}
}
