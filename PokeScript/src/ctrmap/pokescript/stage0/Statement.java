package ctrmap.pokescript.stage0;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public enum Statement {
	WHILE("while", EffectiveLine.StatementFlags.NEEDS_ARGUMENTS, EffectiveLine.StatementFlags.HAS_ARGUMENTS_IN_BRACES),
	IF("if", EffectiveLine.StatementFlags.NEEDS_ARGUMENTS, EffectiveLine.StatementFlags.HAS_ARGUMENTS_IN_BRACES),
	ELSE("else", EffectiveLine.StatementFlags.FORBIDS_ARGUMENTS),
	ELSE_IF("else", "if", EffectiveLine.StatementFlags.NEEDS_ARGUMENTS, EffectiveLine.StatementFlags.HAS_ARGUMENTS_IN_BRACES),
	FOR("for", EffectiveLine.StatementFlags.NEEDS_ARGUMENTS, EffectiveLine.StatementFlags.HAS_ARGUMENTS_IN_BRACES),
	SWITCH("switch", EffectiveLine.StatementFlags.NEEDS_ARGUMENTS, EffectiveLine.StatementFlags.HAS_ARGUMENTS_IN_BRACES),
	RETURN("return"), 
	PAUSE("pause", EffectiveLine.StatementFlags.FORBIDS_ARGUMENTS),
	IMPORT("import", EffectiveLine.StatementFlags.NEEDS_ARGUMENTS, EffectiveLine.StatementFlags.ALLOWS_GLOBAL_EXPLICIT),
	BREAK("break"),
	CONTINUE("continue"),
	GOTO("goto", EffectiveLine.StatementFlags.NEEDS_ARGUMENTS),
	
	//Preprocessor statements
	
	P_IFDEF("#ifdef", EffectiveLine.StatementFlags.NEEDS_ARGUMENTS, EffectiveLine.StatementFlags.PREPROCESSOR_STATEMENT),
	P_IFNDEF("#ifndef", EffectiveLine.StatementFlags.NEEDS_ARGUMENTS, EffectiveLine.StatementFlags.PREPROCESSOR_STATEMENT),
	P_DEFINE("#define", EffectiveLine.StatementFlags.NEEDS_ARGUMENTS, EffectiveLine.StatementFlags.PREPROCESSOR_STATEMENT),
	P_UNDEF("#undef", EffectiveLine.StatementFlags.NEEDS_ARGUMENTS, EffectiveLine.StatementFlags.PREPROCESSOR_STATEMENT),
	P_ELSE("#else", EffectiveLine.StatementFlags.FORBIDS_ARGUMENTS, EffectiveLine.StatementFlags.PREPROCESSOR_STATEMENT),
	P_ELSE_IF("#else", "if", EffectiveLine.StatementFlags.NEEDS_ARGUMENTS, EffectiveLine.StatementFlags.PREPROCESSOR_STATEMENT),
	P_ENDIF("#endif", EffectiveLine.StatementFlags.FORBIDS_ARGUMENTS, EffectiveLine.StatementFlags.PREPROCESSOR_STATEMENT),
	P_ECHO("#echo", EffectiveLine.StatementFlags.NEEDS_ARGUMENTS, EffectiveLine.StatementFlags.PREPROCESSOR_STATEMENT),
	P_ERROR("#error", EffectiveLine.StatementFlags.NEEDS_ARGUMENTS, EffectiveLine.StatementFlags.PREPROCESSOR_STATEMENT),
	P_PRAGMA("#pragma", EffectiveLine.StatementFlags.PREPROCESSOR_STATEMENT);
	
	private String word0;
	private String word1;
	private EffectiveLine.StatementFlags[] flags;

	private Statement(String word0) {
		this(word0, new EffectiveLine.StatementFlags[0]);
	}

	private Statement(String word0, EffectiveLine.StatementFlags... flags) {
		this(word0, null, flags);
	}

	private Statement(String word0, String word1, EffectiveLine.StatementFlags... flags) {
		this.word0 = word0;
		this.word1 = word1;
		this.flags = flags;
	}

	public boolean allowsWord1() {
		return word1 != null;
	}

	public boolean hasWord1(String word1) {
		return this.word1 != null && this.word1.equals(word1);
	}

	public boolean isAllowedInGlobalContext() {
		return hasFlag(EffectiveLine.StatementFlags.PREPROCESSOR_STATEMENT) || hasFlag(EffectiveLine.StatementFlags.ALLOWS_GLOBAL_EXPLICIT);
	}

	public boolean hasFlag(EffectiveLine.StatementFlags flag) {
		for (EffectiveLine.StatementFlags f : flags) {
			if (f == flag) {
				return true;
			}
		}
		return false;
	}

	public static List<Statement> getStatementsByWord0(String word0) {
		List<Statement> l = new ArrayList<>();
		for (Statement s : values()) {
			if (s.word0.equals(word0)) {
				l.add(s);
			}
		}
		return l;
	}

	public String getDeclarationStr(){
		String r = word0;
		if (word1 != null){
			r += word1;
		}
		return r;
	}
}
