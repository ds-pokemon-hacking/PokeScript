
package ctrmap.pokescript.instructions.ntr.instructions;

import ctrmap.pokescript.instructions.abstractcommands.ACompiledInstruction;
import ctrmap.pokescript.instructions.abstractcommands.APlainInstruction;
import ctrmap.pokescript.instructions.abstractcommands.APlainOpCode;
import ctrmap.pokescript.instructions.ntr.NTRInstructionCall;
import ctrmap.pokescript.stage1.NCompileGraph;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class PlainNTRInstruction extends APlainInstruction {
	public List<NTRInstructionCall> instructions = new ArrayList<>();

	public PlainNTRInstruction(APlainOpCode opCode) {
		this(opCode, new int[0]);
	}
	
	public PlainNTRInstruction(APlainOpCode opCode, int... args) {
		super(opCode, args);
	}

	@Override
	public int getAllocatedPointerSpace(NCompileGraph g) {
		int size = 0;
		for (NTRInstructionCall nic : instructions){
			size += nic.getSize();
		}
		return size;
	}

	@Override
	public List<? extends ACompiledInstruction> compile(NCompileGraph g) {
		return instructions;
	}
}
