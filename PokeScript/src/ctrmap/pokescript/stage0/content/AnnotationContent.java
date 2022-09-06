package ctrmap.pokescript.stage0.content;

import ctrmap.pokescript.LangConstants;
import ctrmap.pokescript.stage0.BraceContent;
import ctrmap.pokescript.stage0.CompilerAnnotation;
import ctrmap.pokescript.stage0.EffectiveLine;
import ctrmap.pokescript.stage0.Preprocessor;
import ctrmap.pokescript.stage1.NCompileGraph;
import xstandard.text.StringEx;

public class AnnotationContent extends AbstractContent {

	public CompilerAnnotation annot;

	public AnnotationContent(EffectiveLine line) {
		super(line);
		String data = line.getUnterminatedData();

		EffectiveLine.Word annotName = EffectiveLine.getWord(1, data);
		annot = new CompilerAnnotation(line, annotName.wordContent);

		int argStart = data.indexOf('(');
		if (argStart != -1) {
			BraceContent braceCnt = Preprocessor.getContentInBraces(data, argStart);

			if (!braceCnt.hasIntegrity) {
				line.throwException("Unclosed bracket.");
			}
			else {
				String[] argDefs = StringEx.splitOnecharFast(braceCnt.getContentInBraces(), LangConstants.CH_ELEMENT_SEPARATOR);
				
				for (String argDef : argDefs){
					argDef = argDef.trim();
					int asgnIdx = argDef.indexOf(LangConstants.CH_ASSIGNMENT);
					if (asgnIdx == -1){
						line.throwException("No argument assignment.");
					}
					else {
						String argName = argDef.substring(0, asgnIdx).trim();
						String argValue = argDef.substring(asgnIdx + 1).trim();
						annot.args.put(argName, argValue);
					}
				}
			}
		}
	}

	@Override
	public void addToGraph(NCompileGraph graph) {
		graph.addPendingAnnot(annot);
	}

	@Override
	public CompilerContentType getContentType() {
		return CompilerContentType.ANNOTATION;
	}

}
