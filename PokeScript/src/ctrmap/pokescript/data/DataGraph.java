package ctrmap.pokescript.data;

import ctrmap.pokescript.stage1.NCompileGraph;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DataGraph implements Iterable<Variable>{

	public List<Variable> variables = new ArrayList<>();
	
	protected int stackTop = 0;

	public void clear(){
		variables.clear();
	}
	
	public void removeAll(List<? extends Variable> toRemove, NCompileGraph cg){
		for (Variable v : toRemove){
			variables.remove(v);
			stackTop -= v.getSizeOf(cg);
		}
		for (int i = 0; i < variables.size(); i++){
			variables.get(i).setNumeric(i);
		}
	}
	
	public int getStackTop(){
		return stackTop;
	}
	
	public void addVariable(Variable v, NCompileGraph cg) {
		v.setNumeric(getStackTop());
		variables.add(v);
		stackTop += v.getSizeOf(cg);
	}

	public Variable getVariable(String name) {
		for (Variable v : variables) {
			if (v.name.equals(name)) {
				v.timesUsed++;
				return v;
			}
		}
		return null;
	}
	
	@Override
	public Iterator<Variable> iterator() {
		return variables.iterator();
	}
}
