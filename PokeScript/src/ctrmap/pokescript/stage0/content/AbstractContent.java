
package ctrmap.pokescript.stage0.content;

import ctrmap.pokescript.stage0.EffectiveLine;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.pokescript.types.declarers.DeclarerController;

/**
 *
 */
public abstract class AbstractContent {
	
	public final EffectiveLine line;
	
	public AbstractContent(EffectiveLine line) {
		this.line = line;
	}
	
	public abstract void addToGraph(NCompileGraph graph);
	
	public void declareToGraph(NCompileGraph graph, DeclarerController declarer) {
		
	}
	
	public abstract CompilerContentType getContentType();
	
	public static enum CompilerContentType {
		DECLARATION,
		DECLARATION_ENMCONST,
		EXPRESSION,
		LABEL,
		STATEMENT,
		ANNOTATION,
		NULL
	}
}
