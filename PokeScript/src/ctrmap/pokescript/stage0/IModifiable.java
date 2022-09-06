package ctrmap.pokescript.stage0;

import xstandard.util.ArraysEx;
import java.util.List;

public interface IModifiable {
	
	public List<Modifier> getModifiers();
	
	public default boolean hasModifier(Modifier m) {
		return getModifiers().contains(m);
	}
	
	public default void addModifier(Modifier m) {
		ArraysEx.addIfNotNullOrContains(getModifiers(), m);
	}
}
