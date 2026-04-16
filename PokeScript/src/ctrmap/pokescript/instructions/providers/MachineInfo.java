package ctrmap.pokescript.instructions.providers;

public interface MachineInfo {
	public boolean getAllowsGotoStatement();
	public default boolean isValidRawVarPointer(int value) {
		return false;
	}
}
