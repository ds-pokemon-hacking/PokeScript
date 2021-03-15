package ctrmap.pokescript.instructions.ctr;

import ctrmap.scriptformats.gen6.PawnInstruction;
import ctrmap.pokescript.instructions.abstractcommands.ACompiledInstruction;
import ctrmap.pokescript.instructions.abstractcommands.APlainInstruction;
import ctrmap.pokescript.instructions.abstractcommands.APlainOpCode;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.stdlib.util.ArraysEx;
import java.util.List;

public class PawnPlainInstruction extends APlainInstruction {

	public PawnInstruction ins;

	public PawnPlainInstruction(APlainOpCode opCode) {
		this(opCode, new int[0]);
	}

	public PawnPlainInstruction(APlainOpCode opCode, int... args) {
		super(opCode, args);
		PawnInstruction.Commands cmd = PokeScriptToPawnOpCode.getOpCode(opCode);
		
		if (args.length != 0) {
			ins = new PawnInstruction(cmd, args);
		} else {
			ins = new PawnInstruction(cmd);
		}
	}

	@Override
	public int getAllocatedPointerSpace(NCompileGraph g) {
		return ins.getSize();
	}

	@Override
	public List<? extends ACompiledInstruction> compile(NCompileGraph g) {
		ins.argumentCells = args;
		return ArraysEx.asList(ins);
	}

}
