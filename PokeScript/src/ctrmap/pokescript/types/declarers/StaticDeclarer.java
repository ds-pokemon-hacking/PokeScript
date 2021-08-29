package ctrmap.pokescript.types.declarers;

import ctrmap.pokescript.InboundDefinition;
import ctrmap.pokescript.data.Variable;
import ctrmap.pokescript.stage1.NCompileGraph;

public class StaticDeclarer implements IDeclarer {
	private NCompileGraph g;
	
	public StaticDeclarer(NCompileGraph g) {
		this.g = g;
	}

	@Override
	public void addGlobal(Variable.Global glb) {
		g.addGlobal(glb);
	}

	@Override
	public void addMethod(InboundDefinition def) {
		g.methodHeaders.add(def);
	}
}
