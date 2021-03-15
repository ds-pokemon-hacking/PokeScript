
package ctrmap.pokescript.stage0.content;

import ctrmap.pokescript.stage1.NCompileGraph;

/**
 *
 */
public abstract class AbstractContent {
	
	public abstract void addToGraph(NCompileGraph graph);
	
	public abstract CompilerContentType getContentType();
	
	public static enum CompilerContentType {
		DECLARATION,
		EXPRESSION,
		LABEL,
		STATEMENT,
		ANNOTATION,
		NULL
	}
}
