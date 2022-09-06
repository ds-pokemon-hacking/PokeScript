package ctrmap.pokescript.stage0;

import ctrmap.pokescript.CompilerExceptionData;
import ctrmap.pokescript.CompilerLogger;
import ctrmap.pokescript.InboundDefinition;
import ctrmap.pokescript.LangCompiler;
import ctrmap.pokescript.LangConstants;
import ctrmap.pokescript.MemberDocumentation;
import ctrmap.pokescript.stage0.content.AbstractContent;
import ctrmap.pokescript.stage0.content.DeclarationContent;
import ctrmap.pokescript.stage0.content.EnumConstantDeclarationContent;
import ctrmap.pokescript.stage1.NCompilableMethod;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.pokescript.types.DataType;
import ctrmap.pokescript.types.declarers.DeclarerController;
import xstandard.fs.FSFile;
import xstandard.io.base.iface.ReadableStream;
import xstandard.util.ArraysEx;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Preprocessor {

	private List<EffectiveLine> lines = new ArrayList<>();
	private List<CommentDump> comments = new ArrayList<>();

	private CompilerLogger log;
	private LangCompiler.CompilerArguments args;

	public String contextName = "UnnamedContext";
	public NCompileGraph parentGraph;
	public NCompileGraph cg;

	public Preprocessor(FSFile file, LangCompiler.CompilerArguments args, NCompileGraph parentGraph) {
		this(file.getInputStream(), file.getName(), args);
		this.parentGraph = parentGraph;
	}

	public Preprocessor(FSFile file, LangCompiler.CompilerArguments args) {
		this(file.getInputStream(), file.getName(), args);
	}

	public Preprocessor(ReadableStream stream, String contextName, LangCompiler.CompilerArguments args) {
		log = args.logger;
		this.contextName = contextName;
		this.args = args;
		read(stream);
	}

	public LangCompiler.CompilerArguments getArgs() {
		return args;
	}
	
	public void setArgs(LangCompiler.CompilerArguments args) {
		this.args = args;
	}

	public void read(FSFile fsf) {
		read(fsf.getInputStream());
	}

	public final void read(ReadableStream stream) {
		lines.clear();
		comments.clear();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream.getInputStream(), StandardCharsets.UTF_8))) {
			int line = 1;

			EffectiveLine.AnalysisState state = new EffectiveLine.AnalysisState();
			EffectiveLine.PreprocessorState ppState = new EffectiveLine.PreprocessorState();
			ppState.defined = args.preprocessorDefinitions;

			while (reader.ready()) {
				EffectiveLine l = readLine(line, reader, ppState, state, false, LangConstants.COMMON_LINE_TERM);
				//System.out.print(l.data);
				line += l.newLineCount;
				l.trim();
				l.analyze0(state);
				if (l.hasType(EffectiveLine.LineType.PREPROCESSOR_COMMAND)) {
					l.analyze1(state);

					new TextPreprocessorCommandReader(l, log).processState(ppState);
					lines.add(l);
				} else {
					if (ppState.getIsCodePassthroughEnabled()) {
						lines.add(l);
						l.analyze1(state);
					}
				}
			}
			if (!ppState.ppStack.empty()) {
				if (!lines.isEmpty()) {
					lines.get(lines.size() - 1).throwException("Unclosed preprocessor condition. (Count: " + ppState.ppStack.size() + ")");
				}
			}
		} catch (IOException ex) {
			Logger.getLogger(Preprocessor.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public static String getStrWithoutTerminator(String s) {
		for (char term : LangConstants.COMMON_LINE_TERM) {
			if (s.endsWith(String.valueOf(term))) {
				return s.substring(0, s.length() - 1);
			}
		}
		return s;
	}

	public CommentDump getCommentBeforeLine(int line) {
		EffectiveLine lastLine = null;
		int lmax = -1;
		for (EffectiveLine l : lines) {
			if (!l.hasType(EffectiveLine.LineType.PREPROCESSOR_COMMAND)) {
				int endl = l.startingLine;
				if (endl < line) {
					if (endl < lmax) {
						continue;
					}
					lmax = endl;
					lastLine = l;
				} else {
					break;
				}
			}
		}
		int lineReq = lastLine != null ? lmax : line - 1;
		//System.err.println(contextName);
		//System.err.println("req " + lineReq + ", " + line);
		CommentDump current = null;
		for (CommentDump cd : comments) {
			if (cd.endLine >= lineReq && cd.endLine <= line) {
				if (current == null) {
					current = cd;
				}
				else if (cd.endLine > current.endLine) {
					current = cd;
				}
			}
		}
		return current;
	}

	public List<NMember> getMembers() {
		return getMembers(true);
	}

	public List<NMember> getMembers(boolean localOnly) {
		if (cg == null) {
			getCompileGraph();
		}
		if (!localOnly) {
			if (cg == null) {
				return new ArrayList<>();
			}
		}
		List<NMember> members = new ArrayList<>();
		for (EffectiveLine el : lines) {
			if (el.content.getContentType() == AbstractContent.CompilerContentType.DECLARATION_ENMCONST || (el.content.getContentType() == AbstractContent.CompilerContentType.DECLARATION && el.context != EffectiveLine.AnalysisLevel.LOCAL)) {
				if (el.content.getContentType() == AbstractContent.CompilerContentType.DECLARATION_ENMCONST) {
					EnumConstantDeclarationContent ecdc = (EnumConstantDeclarationContent) el.content;

					for (EnumConstantDeclarationContent.EnumConstant c : ecdc.constants) {
						NMember m = new NMember();
						CommentDump cmt = getCommentBeforeLine(c.line);
						if (c.type != null) {
							m.type = c.type;
						} else if (cg != null && cg.currentClass != null) {
							m.type = cg.currentClass.getTypeDef();
						} else {
							m.type = DataType.ENUM.typeDef();
						}
						m.doc = cmt != null ? new MemberDocumentation(cmt.contents) : null;
						if (localOnly) {
							m.name = c.name;
						} else {
							m.name = (cg != null && cg.currentClass != null) ? c.name : LangConstants.makePath(cg.packageName, cg.currentClass.className, c.name);
						}
						m.modifiers = ArraysEx.asList(Modifier.VARIABLE, Modifier.STATIC, Modifier.FINAL);
						members.add(m);
					}
				} else {
					DeclarationContent decCnt = (DeclarationContent) el.content;
					CommentDump cmt = getCommentBeforeLine(el.startingLine);
					NMember n = new NMember();
					n.modifiers = decCnt.declaredModifiers;
					n.type = decCnt.declaredType;
					n.doc = cmt != null ? new MemberDocumentation(cmt.contents) : null;
					if (decCnt.isMethodDeclaration()) {
						NCompilableMethod m = decCnt.getMethod();

						if (localOnly) {
							n.name = m.def.name;
						} else {
							n.name = (cg.currentClass != null) ? m.def.name : LangConstants.makePath(cg.packageName, cg.currentClass.className, m.def.name);
						}
						n.args = m.def.args;
					} else {
						if (localOnly) {
							n.name = decCnt.declaredName;
						} else {
							n.name = (cg.currentClass != null) ? decCnt.declaredName : LangConstants.makePath(cg.packageName, cg.currentClass.className, decCnt.declaredName);
						}
					}
					members.add(n);
				}
			}
		}

		if (!localOnly) {
			for (Preprocessor sub : cg.includedReaders) {
				members.addAll(sub.getMembers());
			}
		}

		return members;
	}

	public List<InboundDefinition> getDeclaredMethods() {
		List<InboundDefinition> l = new ArrayList<>();
		for (EffectiveLine el : lines) {
			if (el.content != null && el.content.getContentType() == AbstractContent.CompilerContentType.DECLARATION && el.context != EffectiveLine.AnalysisLevel.LOCAL) {
				DeclarationContent decCnt = (DeclarationContent) el.content;
				if (decCnt.isMethodDeclaration()) {
					InboundDefinition def = decCnt.getMethod().def;
					l.add(def);
				}
			}
		}
		return l;
	}

	public List<String> getDeclaredFields() {
		List<String> l = new ArrayList<>();
		for (EffectiveLine el : lines) {
			if (el.content.getContentType() == AbstractContent.CompilerContentType.DECLARATION && el.context == EffectiveLine.AnalysisLevel.GLOBAL) {
				DeclarationContent decCnt = (DeclarationContent) el.content;
				if (decCnt.isVarDeclaration()) {
					l.add(decCnt.declaredName);
				}
			}
		}
		return l;
	}

	public NCompileGraph getCompileGraph() {
		cg = new NCompileGraph(args);
		if (parentGraph != null) {
			cg.merge(parentGraph);
		}
		cg.includePaths = args.includeRoots;

		DeclarerController declarer = new DeclarerController(cg);

		List<EffectiveLine> l = new ArrayList<>(lines);
		
		for (EffectiveLine line : l) {
			cg.currentCompiledLine = line;
			if (line.exceptions.isEmpty() && line.content != null) {
				line.content.declareToGraph(cg, declarer);
			}
		}

		List<CompilerExceptionData> exc;

		for (EffectiveLine line : l) {
			cg.currentCompiledLine = line;
			if (line.exceptions.isEmpty() && line.content != null) {
				line.content.addToGraph(cg);
				if (line.hasType(EffectiveLine.LineType.BLOCK_END) && line.context == EffectiveLine.AnalysisLevel.LOCAL) {
					cg.popBlock();
				}
			}
		}

		cg.finishCompileLoad();

		exc = collectExceptions();

		for (CompilerExceptionData d : exc) {
			log.println(CompilerLogger.LogLevel.ERROR, d.toString());
		}
		
		if (!exc.isEmpty()) {
			return null;
		}

		return cg;
	}

	public List<CompilerExceptionData> collectExceptions() {
		List<CompilerExceptionData> d = new ArrayList<>();
		for (EffectiveLine l : new ArrayList<>(lines)) {
			d.addAll(l.getExceptionData());
		}
		return d;
	}

	public boolean isCompileSuccessful() {
		for (EffectiveLine l : lines) {
			if (!l.exceptions.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	public static boolean isTerminator(char c) {
		return isTerminator(c, false);
	}

	public static boolean isTerminator(char c, boolean allowNewLine) {
		if (allowNewLine) {
			if (c == '\n') {
				return true;
			}
		}
		for (Character chara : LangConstants.COMMON_LINE_TERM) {
			if (chara == c) {
				return true;
			}
		}
		return false;
	}

	public static BraceContent getContentInBraces(String source, int firstBraceIndex) {
		return getContentInBraces(source, firstBraceIndex, false);
	}

	public static BraceContent getContentInBraces(String source, int firstBraceIndex, boolean noNeedBraceStartEnd) {
		int braceLevel = noNeedBraceStartEnd ? 1 : 0;
		int maxIdx = source.length() - 1;
		StringBuilder sb = new StringBuilder();
		BraceContent cnt = new BraceContent();
		for (int idx = firstBraceIndex; idx < source.length(); idx++) {
			char c = source.charAt(idx);
			if (c == '(') {
				braceLevel++;
			} else if (c == ')') {
				braceLevel--;
			}
			sb.append(c);
			if (braceLevel == 0 || braceLevel == 1 && idx == maxIdx) {
				cnt.hasIntegrity = true;
				cnt.endIndex = idx + 1;
				break;
			}
		}
		cnt.content = sb.toString();
		return cnt;
	}

	public static boolean checkNameValidity(String name) {
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if (!Character.isLetterOrDigit(c) && !LangConstants.allowedNonAlphaNumericNameCharacters.contains(c)) {
				return false;
			}
		}
		return true;
	}

	public static char safeCharAt(String str, int idx) {
		if (idx < str.length()) {
			return str.charAt(idx);
		}
		return 0;
	}

	private EffectiveLine readLine(int line, Reader reader, EffectiveLine.PreprocessorState ppState, EffectiveLine.AnalysisState anlState, boolean isPreprocessor, Character... terminators) throws IOException {
		List<Character> termList = ArraysEx.asList(terminators);
		char c;
		StringBuilder unfiltered = new StringBuilder();
		StringBuilder sb = new StringBuilder();
		StringBuilder commentSB = new StringBuilder();

		boolean notifyNextComment = false;
		boolean isInComment = false;
		CommentDump cd = new CommentDump();
		String commentTerm = null;

		boolean firstChar = true;
		boolean isCommentBegin = false;
		int beginCommentLines = 0;

		int lineAccumulator = 0;
		int blevel = 0;
		int charIndex = -1;

		EffectiveLine l = new EffectiveLine();

		while (reader.ready()) {
			c = (char) reader.read();

			if (!isInComment && !notifyNextComment && firstChar) {
				if (c == LangConstants.CH_PP_KW_IDENTIFIER || c == LangConstants.CH_ANNOT_KW_IDENTIFIER) {
					termList.add('\n');
					isPreprocessor = true;
				}
				if (!Character.isWhitespace(c) && c != LangConstants.CH_COMMENT_START_CAND) { //forbid comments
					firstChar = false;
				}
			} else if (c == LangConstants.CH_PP_KW_IDENTIFIER) {
				boolean allow = true;
				if (isInComment) {
					allow = false;
					for (int i = commentSB.length() - 1; i >= 0; i--) {
						char c2 = commentSB.charAt(i);
						if (c2 == '\n') {
							allow = true;
							break;
						} else if (!Character.isWhitespace(c2)) {
							break;
						}
					}
				}
				if (allow) {
					EffectiveLine ppLine = readLine(line + lineAccumulator, reader, ppState, anlState, true, '\n');
					ppLine.data = LangConstants.CH_PP_KW_IDENTIFIER + ppLine.data;
					ppLine.analyze0(anlState);
					ppLine.analyze1(anlState);
					new TextPreprocessorCommandReader(ppLine, log).processState(ppState);
					l.exceptions.addAll(ppLine.exceptions);
					lineAccumulator += ppLine.newLineCount;
					continue;
				}
			}

			if (!firstChar) {
				charIndex++;
			}
			if (notifyNextComment) {
				switch (c) {
					case LangConstants.CH_COMMENT_BLOCK:
						commentTerm = LangConstants.CHSEQ_COMMENT_TERM_BLOCK;
						isInComment = true;
						break;
					case LangConstants.CH_COMMENT_ONELINE:
						commentTerm = LangConstants.CHSEQ_COMMENT_TERM_ONELINE;
						isInComment = true;
						break;
				}
			}
			if (notifyNextComment && isInComment && ppState.getIsCodePassthroughEnabled()) {
				if (charIndex <= 1) {
					isCommentBegin = true;
				}
				sb.deleteCharAt(sb.length() - 1);
				cd.startingLine = line + lineAccumulator;
			}
			notifyNextComment = false;
			switch (c) {
				case '\n':
					lineAccumulator++;
					if (isCommentBegin) {
						beginCommentLines++;
					}
					break;
			}
			if (!isInComment) {
				switch (c) {
					case '(':
						blevel++;
						break;
					case ')':
						blevel--;
						break;
					case LangConstants.CH_COMMENT_START_CAND:
						notifyNextComment = true;
						break;
					default:
						break;
				}
			}

			if (isInComment) {
				if (unfiltered.toString().endsWith(commentTerm)) {
					cd.endLine = line + lineAccumulator;
					cd.contents = commentSB.toString();
					commentSB = new StringBuilder();
					comments.add(cd);
					cd = new CommentDump();

					isInComment = false;
					isCommentBegin = false;
				}
			}
			if (!isInComment) {
				if (ppState.getIsCodePassthroughEnabled() || isPreprocessor) {
					sb.append(c);
				}
			} else {
				commentSB.append(c);
			}

			unfiltered.append(c);

			if (c == LangConstants.CH_METHOD_EXTENDS_IDENT) {
				//might be part of a method declaration - scan back for a bracket
				boolean allowDDBreak = true;
				for (int i = sb.length() - 2; i >= 0; i--) {
					char c2 = sb.charAt(i);
					if (!Character.isWhitespace(c2)) {
						if (c2 == ')') {
							allowDDBreak = false;
						}
						break;
					}
				}

				if (!allowDDBreak) {
					continue;
				}
			}

			if (!isInComment && blevel == 0 && termList.contains(c)) {
				break;
			}
		}
		l.startingLine = line;
		l.startingLine += beginCommentLines;
		l.fileName = contextName;
		l.data = sb.toString();
		l.newLineCount = lineAccumulator;
		return l;
	}
}
