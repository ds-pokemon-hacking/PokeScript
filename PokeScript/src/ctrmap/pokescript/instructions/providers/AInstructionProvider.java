
package ctrmap.pokescript.instructions.providers;

public interface AInstructionProvider {	
	public abstract MachineInfo getMachineInfo();
	
	public abstract MetaFunctionHandler getMetaFuncHandler(String handlerName);
}
