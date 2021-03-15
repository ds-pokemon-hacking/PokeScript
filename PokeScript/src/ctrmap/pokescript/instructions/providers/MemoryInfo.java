
package ctrmap.pokescript.instructions.providers;

public interface MemoryInfo {
	public boolean isArgsUnderStackFrame();
	public int getGlobalsIndexingStep();
	public int getStackIndexingStep();
	public int getFuncArgOffset();
}
