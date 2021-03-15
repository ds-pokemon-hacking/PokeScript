
package ctrmap.pokescript.instructions.abstractcommands;

import ctrmap.pokescript.OutboundDefinition;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.pokescript.stage1.NExpression;

/**
 *
 */
public abstract class ANativeCall extends ALocalCall {

	public ANativeCall(String target, NExpression[] arguments, NCompileGraph graph) {
		super(target, arguments, graph);
	}
	
	public ANativeCall(OutboundDefinition call) {
		super(call);
	}

}
