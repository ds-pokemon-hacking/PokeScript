
package ctrmap.pokescript.instructions.providers;

public interface MemoryInfo {
	public boolean isArgsUnderStackFrame();
	public boolean isStackOrderNatural();
	public int getGlobalsIndexingStep();
	public int getStackIndexingStep();
	public int getFuncArgOffset();
}
