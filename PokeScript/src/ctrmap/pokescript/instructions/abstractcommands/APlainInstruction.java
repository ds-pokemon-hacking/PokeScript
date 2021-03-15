
package ctrmap.pokescript.instructions.abstractcommands;

import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.stdlib.util.ArraysEx;
import java.util.List;

/**
 *
 */
public abstract class APlainInstruction extends AInstruction {

	public APlainOpCode opCode;
	public int[] args;
	
	public APlainInstruction(APlainOpCode opCode){
		this(opCode, new int[0]);
	}
	
	public APlainInstruction(APlainOpCode opCode, int... args){
		this.opCode = opCode;
		this.args = args;
	}
	
	public int getArgument(int idx){
		return args[idx];
	}
	
	@Override
	public List<AInstruction> getAllInstructions() {
		return ArraysEx.asList(this);
	}

	@Override
	public abstract int getAllocatedPointerSpace(NCompileGraph g);

	@Override
	public abstract List<? extends ACompiledInstruction> compile(NCompileGraph g);

}
