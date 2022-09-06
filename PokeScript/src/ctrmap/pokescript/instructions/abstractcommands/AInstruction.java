package ctrmap.pokescript.instructions.abstractcommands;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public abstract class AInstruction {

	private List<String> labels = new ArrayList<>();

	public abstract List<AInstruction> getAllInstructions();
	
	public abstract AInstructionType getType();
	
	public List<String> labels() {
		return labels;
	}
	
	public void addLabel(String lbl){
		labels.add(lbl);
	}
	
	public boolean hasLabel(String lbl){
		return labels.contains(lbl);
	}
	
	public void relocateLabels(String namespace){
		for (int i = 0; i < labels.size(); i++){
			labels.set(i, namespace + "_" + labels.get(i));
		}
	}
}
