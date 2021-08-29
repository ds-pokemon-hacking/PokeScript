package ctrmap.pokescript.types.declarers;

import ctrmap.pokescript.InboundDefinition;
import ctrmap.pokescript.data.Variable;

public interface IDeclarer {
	public void addGlobal(Variable.Global glb);
	public void addMethod(InboundDefinition def);
}
