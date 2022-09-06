package ctrmap.pokescript.instructions.abstractcommands;

import xstandard.util.ArraysEx;
import java.util.List;

public class APlainInstruction extends AInstruction {

	public APlainOpCode opCode;
	public int[] args;

	public APlainInstruction(APlainOpCode opCode) {
		this(opCode, new int[0]);
	}

	public APlainInstruction(APlainOpCode opCode, int... args) {
		this.opCode = opCode;
		this.args = args;
	}

	public int getArgument(int idx) {
		return args[idx];
	}

	@Override
	public List<AInstruction> getAllInstructions() {
		return ArraysEx.asList(this);
	}

	@Override
	public AInstructionType getType() {
		return AInstructionType.PLAIN;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append(opCode);
		
		sb.append("(");
		for (int i = 0; i < args.length; i++) {
			if (i != 0) {
				sb.append(", ");
			}
			sb.append(args[i]);
		}
		sb.append(")");
		
		return sb.toString();
	}
}
