
package ctrmap.pokescript.instructions.abstractcommands;

import ctrmap.stdlib.util.ArraysEx;
import java.util.List;

public abstract class AAccessGlobal extends AInstruction {

	public String gName;

	public AAccessGlobal(String name) {
		gName = name;
	}
	
	@Override
	public List<AInstruction> getAllInstructions() {
		return ArraysEx.asList(this);
	}
}
