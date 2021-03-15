package ctrmap.pokescript.instructions.abstractcommands;

import ctrmap.stdlib.util.ArraysEx;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public abstract class ACaseTable extends AInstruction {

	public Map<Integer, String> targets = new HashMap<>();
	public String defaultCase;
	
	@Override
	public List<AInstruction> getAllInstructions() {
		return ArraysEx.asList(this);
	}
}
