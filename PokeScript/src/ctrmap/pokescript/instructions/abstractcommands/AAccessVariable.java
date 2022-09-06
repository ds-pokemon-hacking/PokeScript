package ctrmap.pokescript.instructions.abstractcommands;

import ctrmap.pokescript.data.Variable;
import xstandard.util.ArraysEx;
import java.util.List;

public abstract class AAccessVariable extends AInstruction {
	public Variable var;
	
	protected AAccessVariable(Variable var) {
		this.var = var;
	}
	
	@Override
	public String toString() {
		return getType() + "(" + var.name + ")";
	}
	
	public static class AWriteVariable extends AAccessVariable {

		public AWriteVariable(Variable var) {
			super(var);
		}

		@Override
		public List<AInstruction> getAllInstructions() {
			return ArraysEx.asList(this);
		}

		@Override
		public AInstructionType getType() {
			return AInstructionType.SET_VARIABLE;
		}
	}
	
	public static class AReadVariable extends AAccessVariable {

		public AReadVariable(Variable var) {
			super(var);
		}

		@Override
		public List<AInstruction> getAllInstructions() {
			return ArraysEx.asList(this);
		}
		
		@Override
		public AInstructionType getType() {
			return AInstructionType.GET_VARIABLE;
		}
	} 
}
