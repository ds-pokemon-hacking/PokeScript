package ctrmap.pokescript.stage0.content;

import ctrmap.pokescript.stage0.BraceContent;
import ctrmap.pokescript.stage0.CompilerAnnotation;
import ctrmap.pokescript.stage0.EffectiveLine;
import ctrmap.pokescript.stage0.Preprocessor;
import ctrmap.pokescript.stage1.NCompileGraph;

/**
 *
 */
public class AnnotationContent extends AbstractContent {

	private EffectiveLine line;

	public CompilerAnnotation annot;

	public AnnotationContent(EffectiveLine line) {
		this.line = line;
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
				String[] argDefs = braceCnt.getContentInBraces().split(",");
				
				for (String argDef : argDefs){
					argDef = argDef.trim();
					int asgnIdx = argDef.indexOf('=');
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
