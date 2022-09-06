package ctrmap.pokescript.stage0.content;

import ctrmap.pokescript.LangConstants;
import ctrmap.pokescript.stage0.Preprocessor;
import ctrmap.pokescript.data.Variable;
import ctrmap.pokescript.instructions.abstractcommands.ACaseTable;
import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.stage0.EffectiveLine;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.pokescript.stage1.PendingLabel;
import xstandard.util.ParsingUtils;

/**
 *
 */
public class LabelContent extends AbstractContent {

	public LabelType type;
	public String label;

	public LabelContent(EffectiveLine line) {
		super(line);
		String source = line.data;
		EffectiveLine.Word word = EffectiveLine.getWord(0, source);
		type = LabelType.getLabelType(word.wordContent);
		if (type != LabelType.SWITCH_CASE) {
			label = word.wordContent;
			if (label.isEmpty()) {
				line.throwException("Label can not be empty.");
			}
			checkExpectedEnd(source, line, word.sourceEndIdx, LangConstants.CH_LABEL_IDENT);
		} else {
			EffectiveLine.Word word1 = EffectiveLine.getWord(word.sourceEndIdx, source);
			if (word1.wordContent.isEmpty()) {
				line.throwException("Label can not be empty.");
			}
			label = word1.wordContent;
			checkExpectedEnd(source, line, word1.sourceEndIdx, LangConstants.CH_LABEL_IDENT);
		}
	}

	public static void checkExpectedEnd(String source, EffectiveLine line, int startIdx, char expectedChar) {
		int endIdx = EffectiveLine.getFirstNonWhiteSpaceIndex(startIdx, source);
		char c = Preprocessor.safeCharAt(source, endIdx);
		if (c != expectedChar) {
			line.throwException(expectedChar + " expected at " + source);
		}
	}

	@Override
	public CompilerContentType getContentType() {
		return CompilerContentType.LABEL;
	}

	@Override
	public void addToGraph(NCompileGraph graph) {
		PendingLabel pl = graph.addPendingLabel(label);
		switch (type) {
			case SWITCH_DEFAULT_CASE:
			case SWITCH_CASE:
				AInstruction control = graph.getCurrentBlock().blockEndControlInstruction;
				if (control instanceof ACaseTable) {
					ACaseTable tbl = (ACaseTable) control;
					if (type == LabelType.SWITCH_DEFAULT_CASE) {
						tbl.defaultCase = pl.getFullLabel();
					} else {
						Variable.Global tryGlobal = graph.resolveGVar(label);
						if (tryGlobal != null) {
							if (tryGlobal.isImmediate()) {
								tbl.targets.put(tryGlobal.getImmediateValue(), pl.getFullLabel());
							} else {
								line.throwException("Case has to be a constant value.");
							}
						}
						else {
							int value = 0;
							try {
								value = ParsingUtils.parseBasedInt(label);
							}
							catch (NumberFormatException ex){
								line.throwException("Case has to be either a constant or an integer value.");
							}
							tbl.targets.put(value, pl.getFullLabel());
						}
					}
				} else {
					line.throwException("Case label outside of a switch block. - control instruction is of type " + (control == null ? null : control.getClass().getSimpleName()));
				}
				break;
			default:
				pl.isUserLabel = true;
				break;
		}
	}

	public enum LabelType {
		STANDARD,
		SWITCH_CASE("case"),
		SWITCH_DEFAULT_CASE("default");

		private String idStatement;

		private LabelType() {
			this(null);
		}

		private LabelType(String statement) {
			idStatement = statement;
		}

		public static LabelType getLabelType(String source) {
			for (LabelType t : values()) {
				if (source.equals(t.idStatement)) {
					return t;
				}
			}
			return STANDARD;
		}
	}
}
