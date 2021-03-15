package ctrmap.pokescript.stage0;

import ctrmap.pokescript.CompilerLogger;
import ctrmap.pokescript.stage0.content.StatementContent;

/**
 *
 */
public class TextPreprocessorCommandReader {

	private EffectiveLine l;
	private CompilerLogger log;

	private StatementContent cnt;

	public TextPreprocessorCommandReader(EffectiveLine l, CompilerLogger log) {
		this.log = log;
		this.l = l;
		if (!(l.content instanceof StatementContent)) {
			l.throwException("Preprocessor line content is not a statement!");
			return;
		}
		cnt = (StatementContent) l.content;
		if (cnt.statement == null || !cnt.statement.hasFlag(EffectiveLine.StatementFlags.PREPROCESSOR_STATEMENT)) {
			l.throwException(cnt.statement + " is not a preprocessor statement!");
		}
	}

	public void processState(EffectiveLine.PreprocessorState state) {
		if (cnt == null){
			return;
		}
		switch (cnt.statement) {
			case P_DEFINE:
				state.defined.add(cnt.arguments.get(0));
				break;
			case P_UNDEF:
				state.defined.remove(cnt.arguments.get(0));
				break;
			case P_ENDIF:
				if (!throwExOnEmptyStack(state, "EndIf not expected - remove this token.")) {
					state.ppStack.pop();
				}
				break;
			case P_ELSE:
				if (!throwExOnEmptyStack(state, "Else not expected - no condition is active.")) {
					state.ppStack.push(!state.ppStack.pop());
				}
				break;
			case P_ELSE_IF:
				state.ppStack.pop();
			case P_IFDEF:
				state.ppStack.push(state.defined.contains(cnt.arguments.get(0)));
				break;
			case P_IFNDEF:
				state.ppStack.push(!state.defined.contains(cnt.arguments.get(0)));
				break;
			case P_ECHO:
				log.println(CompilerLogger.LogLevel.INFO, cnt.arguments.get(0));
				break;
			case P_ERROR:
				l.throwException(cnt.arguments.get(0));
				break;
			case P_PRAGMA:
				CompilerPragma.PragmaValue val = CompilerPragma.tryIdentifyPragma(cnt, l);
				if (val != null){
					state.pragmata.put(val.pragma, val);
				}
				break;
		}
	}

	private boolean throwExOnEmptyStack(EffectiveLine.PreprocessorState state, String exText) {
		if (state.ppStack.empty()) {
			l.throwException(exText);
			return true;
		}
		return false;
	}
}