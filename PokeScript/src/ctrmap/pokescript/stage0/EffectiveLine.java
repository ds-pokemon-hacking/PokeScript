package ctrmap.pokescript.stage0;

import ctrmap.pokescript.CompilerExceptionData;
import ctrmap.pokescript.LangConstants;
import ctrmap.pokescript.stage0.content.AbstractContent;
import ctrmap.pokescript.stage0.content.AnnotationContent;
import ctrmap.pokescript.stage0.content.DeclarationContent;
import ctrmap.pokescript.stage0.content.ExpressionContent;
import ctrmap.pokescript.stage0.content.LabelContent;
import ctrmap.pokescript.stage0.content.NullContent;
import ctrmap.pokescript.stage0.content.StatementContent;
import ctrmap.pokescript.types.classes.ClassDefinition;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class EffectiveLine {

	public String fileName;
	public String data;
	public int startingLine;
	public int newLineCount;
	public AnalysisLevel context;
	public List<LineType> types = new ArrayList<>();

	public AbstractContent content;

	public List<String> exceptions = new ArrayList<>();

	public void trim() {
		for (int i = 0; i < data.length(); i++) {
			//trim whitespaces at the beginning and adjust line count accordingly
			char c = data.charAt(i);
			if (!Character.isWhitespace(c)) {
				break;
			} else if (c == '\n') {
				startingLine++;
				newLineCount--;
			}
		}

		data = trimAndSetWsToOne(data);
	}

	public List<CompilerExceptionData> getExceptionData() {
		List<CompilerExceptionData> l = new ArrayList<>();
		for (String s : exceptions) {
			CompilerExceptionData d = new CompilerExceptionData();
			d.fileName = fileName;
			d.lineNumberStart = startingLine;
			d.lineNumberEnd = startingLine + newLineCount;
			d.text = s;
			l.add(d);
		}
		return l;
	}

	public String getUnterminatedData() {
		return Preprocessor.getStrWithoutTerminator(data);
	}

	public void throwException(String cause) {
		exceptions.add(cause);
	}

	public void analyze0(AnalysisState state) {
		types.clear();
		content = null;
		if (data.length() > 0) {
			char lastChar = data.charAt(data.length() - 1);
			switch (lastChar) {
				case '{':
					types.add(LineType.BLOCK_START);
					break;
				case '}':
					types.add(LineType.BLOCK_END);
					break;
				case ';':
					types.add(LineType.NORMAL);
					break;
				case ':':
					types.add(LineType.LABEL);
					break;
			}
			char firstChar = data.charAt(0);
			if (firstChar == LangConstants.CH_PP_KW_IDENTIFIER) {
				types.add(LineType.PREPROCESSOR_COMMAND);
			} else if (firstChar == LangConstants.CH_ANNOT_KW_IDENTIFIER){
				types.add(LineType.COMPILER_ANNOTATION);
			}
		}
	}

	public void analyze1(AnalysisState state) {
		context = state.level;
		
		if (hasType(LineType.COMPILER_ANNOTATION)){
			trySetContent(new AnnotationContent(this));
		}
		
		if (state.level == AnalysisLevel.LOCAL) {
			//Inside a method
			if (hasType(LineType.LABEL)) {
				trySetContent(new LabelContent(this));
			} else {
				trySetContent(getStatementCnt(state));
			}
			if (content == null) {
				DeclarationContent dc = DeclarationContent.getDeclarationCnt(data, this, state);
				trySetContent(dc);
				if (dc != null) {
					if (dc.isMethodDeclaration()) {
						throwException("Can not declare methods in a local context.");
					}
				}
			}
		} else if (state.level == AnalysisLevel.CLASS) {
			//Inside a class - only declarations and preprocessor keywords are accepted
			DeclarationContent dc = DeclarationContent.getDeclarationCnt(data, this, state);
			trySetContent(dc);
			if (dc != null) {
				if (dc.isMethodDeclaration() && hasType(LineType.BLOCK_START)) {
					types.add(LineType.METHOD_BODY_START);
				}
			}
		} else {
			//Only static methods, imports and class definitions are accepted
			trySetContent(getStatementCnt(state));

			if (content == null) {
				DeclarationContent dc = DeclarationContent.getDeclarationCnt(data, this, state);

				if (dc != null) {
					if (!dc.isClassDeclaration() && !dc.hasModifier(Modifier.STATIC)) {
						throwException("Non-static members are only allowed inside classes.");
					}
					else {
						if (dc.isMethodDeclaration() && hasType(LineType.BLOCK_START)){
							state.incrementBlockToVirtualClass();
						}
					}
					trySetContent(dc);
					if (dc.isMethodDeclaration() && hasType(LineType.BLOCK_START)) {
						types.add(LineType.METHOD_BODY_START);
					}
				}
			}
		}
		trySetContent(NullContent.checkGetNullContent(this));
		if (content == null && state.level != AnalysisLevel.LOCAL) {
			throwException("Expressions are not allowed in a global context. - " + data);
			content = new NullContent(this);
		} else {
			trySetContent(new ExpressionContent(this)); //if everything fails, an expression is assumed
		}


		if (hasType(LineType.BLOCK_START)) {
			state.incrementBlock();
		} else if (hasType(LineType.BLOCK_END)) {
			state.decrementBlock();
			if (state.blockLevel < 0) {
				throwException("Unexpected block end: } (delete this token)");
			}
		}
	}

	public boolean hasType(LineType t) {
		return types.contains(t);
	}

	private void trySetContent(AbstractContent content) {
		if (this.content != null) {
			return;
		}
		if (content != null) {
			this.content = content;
		}
	}

	public static String trimAndSetWsToOne(String str) {
		str = str.trim();

		StringBuilder sb = new StringBuilder();
		char c;
		boolean ws = false;
		for (int i = 0; i < str.length(); i++) {
			c = str.charAt(i);
			if (Character.isWhitespace(c)) {
				if (!ws) {
					sb.append(c); //trims all whitespaces to size 1
				}
				ws = true;
			} else {
				ws = false;
				sb.append(c);
			}
		}
		return sb.toString();
	}

	public static String removeAllWhitespaces(String str) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			if (!Character.isWhitespace(c)) {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	public StatementContent getStatementCnt(AnalysisState state) {
		if (data.isEmpty()) {
			return null;
		}
		boolean isPreprocessor = data.charAt(0) == LangConstants.CH_PP_KW_IDENTIFIER;
		int wordStartIndex = isPreprocessor ? 1 : 0;
		Word statementWord = getWord(wordStartIndex, data);
		if (isPreprocessor) {
			statementWord.wordContent = "#" + statementWord;
		}

		Statement stm = null;
		List<Statement> sl = Statement.getStatementsByWord0(statementWord.wordContent);
		int statementContentStart = statementWord.sourceEndIdx;

		switch (sl.size()) {
			case 0:
				return null;
			case 1:
				stm = sl.get(0);
				break;
			case 2:
				Word word1 = getWord(statementWord.sourceEndIdx, data);
				boolean found = false;
				if (word1.wordContent.trim().isEmpty()) {
					for (Statement s : sl) {
						if (!s.allowsWord1()) {
							stm = s;
							found = true;
							break;
						}
					}
				}
				if (!found) {
					for (Statement s : sl) {
						if (s.hasWord1(word1.wordContent)) {
							stm = s;
							statementContentStart = word1.sourceEndIdx;
							found = true;
							break;
						}
					}
				}
				if (!found) {
					throwException("Error: " + statementWord + " " + word1 + " is not a valid multi-statement.");
				}
				break;
		}
		if (stm != null) {
			if (state.level != AnalysisLevel.LOCAL && !stm.isAllowedInGlobalContext()) {
				throwException("Illegal statement used in a non-local context: " + statementWord.wordContent);
			} else if (state.level == AnalysisLevel.LOCAL && stm == Statement.IMPORT) {
				throwException("Import statement is not allowed in a local context.");
			} else {
				StatementContent cnt = new StatementContent(stm, this, statementContentStart);
				return cnt;
			}
		}
		return null;
	}

	public static Word getWord(int startIndex, String str) {
		Word word = new Word();
		word.sourceStartIdx = startIndex;
		StringBuilder sb = new StringBuilder();
		for (int i = getFirstNonWhiteSpaceIndex(startIndex, str); i < str.length(); i++) {
			char c = str.charAt(i);
			if (!(Character.isLetterOrDigit(c) || LangConstants.allowedNonAlphaNumericNameCharacters.contains(c))) {
				word.sourceEndIdx = i;
				break;
			}
			sb.append(c);
		}
		if (word.sourceEndIdx == -1) {
			word.sourceEndIdx = str.length();
		}
		word.wordContent = sb.toString();
		return word;
	}

	public static int getFirstNonWhiteSpaceIndex(int startIndex, String str) {
		for (int i = startIndex; i < str.length(); i++) {
			if (!Character.isWhitespace(str.charAt(i))) {
				return i;
			}
		}
		return str.length();
	}

	public static enum LineType {
		NORMAL,
		BLOCK_START,
		BLOCK_END,
		STATEMENT,
		EXPRESSION,
		LABEL,
		METHOD_BODY_START,
		METHOD_END,
		PREPROCESSOR_COMMAND,
		COMPILER_ANNOTATION
	}

	public static class PreprocessorState {

		public List<String> defined = new ArrayList<>();
		public Stack<Boolean> ppStack = new Stack<>();
		public Map<CompilerPragma, CompilerPragma.PragmaValue> pragmata = new HashMap<>(); //yeah I searched up the plural on Wiktionary. I'm just cool like that.

		public boolean getIsCodePassthroughEnabled() {
			for (Boolean bln : ppStack) {
				if (!bln) {
					return false;
				}
			}
			return true;
		}
	}

	public static class AnalysisState {

		public List<ClassDefinition> classDefs = new ArrayList<>();

		public AnalysisLevel level = AnalysisLevel.GLOBAL;
		private int blockLevel = 0;
		private boolean virtualClass = false;
		private ClassDefinition currentClass = null;

		public void incrementBlock() {
			blockLevel++;
			setAnLvl();
		}
		
		public void incrementBlockToVirtualClass(){
			virtualClass = true;
			blockLevel++;
			setAnLvl();
		}

		public void decrementBlock() {
			blockLevel--;
			setAnLvl();
			if (level == AnalysisLevel.CLASS && virtualClass){
				decrementBlock();
				virtualClass = false;
			}
		}

		public void setCurrentClass(String className) {
			currentClass = getClassDef(className);
		}

		public ClassDefinition getClassDef(String className) {
			for (ClassDefinition cdef : classDefs) {
				if (cdef.hasName(className)) {
					return cdef;
				}
			}
			return null;
		}

		private void setAnLvl() {
			level = blockLevel == 0 ? AnalysisLevel.GLOBAL : blockLevel == 1 ? AnalysisLevel.CLASS : AnalysisLevel.LOCAL;
			if (level != AnalysisLevel.CLASS) {
				currentClass = null;
			}
		}
	}

	public static enum AnalysisLevel {
		GLOBAL,
		CLASS,
		LOCAL
	}

	public static enum StatementFlags {
		NEEDS_ARGUMENTS,
		HAS_ARGUMENTS_IN_BRACES,
		FORBIDS_ARGUMENTS,
		PREPROCESSOR_STATEMENT,
		ALLOWS_GLOBAL_EXPLICIT
	}

	public static class Word {

		public String wordContent;
		public int sourceStartIdx;
		public int sourceEndIdx = -1;

		@Override
		public String toString() {
			return wordContent;
		}
	}
}
