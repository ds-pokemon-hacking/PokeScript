package ctrmap.pokescript.instructions.abstractcommands;

import xstandard.util.ArraysEx;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class ACaseTable extends AInstruction {

	public Map<Integer, String> targets = new HashMap<>();
	public String defaultCase;

	@Override
	public List<AInstruction> getAllInstructions() {
		return ArraysEx.asList(this);
	}

	@Override
	public AInstructionType getType() {
		return AInstructionType.CASE_TABLE;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("CASETBL {");
		List<Map.Entry<Integer, String>> l = new ArrayList<>(targets.entrySet());
		l.sort(Map.Entry.comparingByKey());
		
		for (Map.Entry<Integer, String> e : l) {
			sb.append("\t").append(e.getKey()).append(" => ").append(e.getValue());
		}
		
		sb.append("\tdefault => ").append(defaultCase);
		sb.append("}");
		
		return sb.toString();
	}
}
