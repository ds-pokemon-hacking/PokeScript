package ctrmap.pokescript.instructions.abstractcommands;

import ctrmap.scriptformats.gen6.PawnInstruction;
import ctrmap.pokescript.stage1.NCompileGraph;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public abstract class AInstruction {

	public int pointer;
	private List<String> labels = new ArrayList<>();

	public abstract List<AInstruction> getAllInstructions();

	public abstract int getAllocatedPointerSpace(NCompileGraph g);

	public abstract List<? extends ACompiledInstruction> compile(NCompileGraph g);
	
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
