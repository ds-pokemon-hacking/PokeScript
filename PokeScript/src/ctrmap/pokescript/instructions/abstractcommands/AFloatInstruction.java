package ctrmap.pokescript.instructions.abstractcommands;

import xstandard.util.ArraysEx;
import java.util.List;

public class AFloatInstruction extends AInstruction {

	public AFloatOpCode opCode;
	public float[] args;

	public AFloatInstruction(AFloatOpCode opCode, float... args) {
		this.opCode = opCode;
		this.args = args;
	}

	@Override
	public List<AInstruction> getAllInstructions() {
		return ArraysEx.asList(this);
	}

	@Override
	public AInstructionType getType() {
		return AInstructionType.PLAIN_FLOAT;
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
