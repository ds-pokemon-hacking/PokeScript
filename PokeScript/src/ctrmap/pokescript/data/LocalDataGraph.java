package ctrmap.pokescript.data;

import ctrmap.pokescript.stage1.NCompileGraph;
import java.util.ArrayList;
import java.util.List;

public class LocalDataGraph extends DataGraph {

	private int stk = 0;

	private int argOffset;

	public LocalDataGraph(NCompileGraph g) {
		argOffset = g.provider.getMemoryInfo().getFuncArgOffset();
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
			//System.out.println("removing local " + v.name);
			stackTop -= v.getSizeOf(cg);
		}
		variables.removeAll(toRemove);
		int argCount = stk;
		for (int i = 0; i < variables.size(); i++) {
			//System.out.println("Checking variable " + variables.get(i).index);
			int expectedIndex = i + stk;
			if (i < argCount) {
				expectedIndex -= argOffset;
			}
			if (expectedIndex != variables.get(i).index) {
				throw new UnsupportedOperationException("Tried to remove variable " + variables.get(i).name + " from in between the stack. (" + variables.get(i).index + ").");
			}
		}
	}

	public void addVariableUnderStackFrame(Variable v, NCompileGraph cg) {
		if (cg.provider.getMemoryInfo().isArgsUnderStackFrame()) {
			variables.add(0, v);
			stk += v.getSizeOf(cg);
			v.setNumeric(-argOffset - stk); //before stack frame
		}
		else {
			addVariable(v, cg);
		}
	}
}
