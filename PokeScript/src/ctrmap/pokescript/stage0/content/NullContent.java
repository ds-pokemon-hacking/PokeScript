
package ctrmap.pokescript.stage0.content;

import ctrmap.pokescript.stage0.Preprocessor;
import ctrmap.pokescript.stage0.EffectiveLine;
import ctrmap.pokescript.stage1.CompileBlock;
import ctrmap.pokescript.stage1.NCompileGraph;

public class NullContent extends AbstractContent{
	
	public NullContent(EffectiveLine l){
		super(l);
	}
	
	public static NullContent checkGetNullContent(EffectiveLine line){
		if (!line.data.isEmpty()){
			if (line.data.length() > 1){
				return null;
			}
			else {
				if (!Preprocessor.isTerminator(line.data.charAt(0))){
					return null;
				}
			}
		}
		return new NullContent(line);
	}

	@Override
	public CompilerContentType getContentType() {
		return CompilerContentType.NULL;
	}

	@Override
	public void addToGraph(NCompileGraph graph) {
		if (line.hasType(EffectiveLine.LineType.BLOCK_START) && line.context == EffectiveLine.AnalysisLevel.LOCAL){
			graph.pushBlock(new CompileBlock(graph));
		}
	}
}
