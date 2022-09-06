package ctrmap.pokescript.data;

import ctrmap.pokescript.stage1.NCompileGraph;
import java.util.ArrayList;
import java.util.List;

public class LocalDataGraph extends DataGraph {

	private int stk = 0;

	public LocalDataGraph(NCompileGraph g) {
		
	}

	@Override
	public void clear() {
		super.clear();
		stk = 0;
	}

	public List<Variable> getNonArgVars() {
		List<Variable> l = new ArrayList<>();
		for (Variable v : variables) {
			if (v.index >= 0) {
				l.add(v);
			}
		}
		return l;
	}

	@Override
	public void removeAll(List<? extends Variable> toRemove, NCompileGraph cg) {
		//only variables at the top of the stack should ever get removed, mostly at the end of the block
		//that means that the stack index values should remain unchanged
		//however, we can easily check this
		for (Variable v : toRemove) {
			if (v.getLocation() == Variable.VarLoc.STACK_UNDER) {
				throw new RuntimeException("STACK_UNDER variables should not be removed!");
			}
			//System.out.println("removing local " + v.name);
			stackTop -= v.getSizeOf(cg);
		}
		variables.removeAll(toRemove);
		int argCount = stk;
		//System.out.println("argcount " + stk);
		for (int i = 0; i < variables.size(); i++) {
			//System.out.println("Checking variable " + variables.get(i).index);
			int expectedIndex = i;
			if (i < argCount) {
				expectedIndex = argCount - i - 1;
			} else {
				//System.out.println("nonarg var " + variables.get(i).name);
				expectedIndex -= argCount;
			}
			if (expectedIndex != variables.get(i).index) {
				System.err.println("All vars: " + variables);
				throw new UnsupportedOperationException(
					"Tried to remove variable " + variables.get(i).name + " from in between the stack. (" + variables.get(i).index + "). "
					+ "Expected index " + expectedIndex);
			}
		}
	}

	public void addVariableUnderStackFrame(Variable v, NCompileGraph cg) {
		variables.add(0, v);
		v.setNumeric(stk); //before stack frame
		stk += v.getSizeOf(cg);
	}
}
