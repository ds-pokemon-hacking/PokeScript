package ctrmap.pokescript.instructions.ctr.instructions;

import ctrmap.pokescript.instructions.abstractcommands.AConditionJump;
import ctrmap.pokescript.instructions.abstractcommands.APlainOpCode;
import ctrmap.pokescript.instructions.ctr.PokeScriptToPawnOpCode;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.scriptformats.gen6.PawnInstruction;
import java.util.ArrayList;
import java.util.List;

public class PConditionJump extends AConditionJump {

	public PConditionJump(APlainOpCode cmd, String targetLabel) {
		super(cmd, targetLabel);
	}

	@Override
	public int getAllocatedPointerSpace(NCompileGraph g) {
		return 8;
	}

	@Override
	public List<PawnInstruction> compile(NCompileGraph g) {
		List<PawnInstruction> r = new ArrayList<>();
		r.add(new PawnInstruction(PokeScriptToPawnOpCode.getOpCode(cmd), g.getInstructionByLabel(targetLabel).pointer - pointer));

		return r;
	}
}
