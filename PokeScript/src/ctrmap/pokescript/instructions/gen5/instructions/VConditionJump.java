
package ctrmap.pokescript.instructions.gen5.instructions;

import ctrmap.pokescript.instructions.abstractcommands.ACompiledInstruction;
import ctrmap.pokescript.instructions.abstractcommands.AConditionJump;
import ctrmap.pokescript.instructions.abstractcommands.APlainOpCode;
import ctrmap.pokescript.instructions.gen5.VCmpResultRequest;
import ctrmap.pokescript.instructions.gen5.VConstants;
import ctrmap.pokescript.instructions.gen5.VOpCode;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.stdlib.util.ArraysEx;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class VConditionJump extends AConditionJump {

	public VConditionJump(APlainOpCode cmd, String targetLabel) {
		super(cmd, targetLabel);
	}

	@Override
	public int getAllocatedPointerSpace(NCompileGraph g) {
		switch (cmd){
			case JUMP:
			case SWITCH:
				return VOpCode.Jump.getSize();
			case JUMP_IF_ZERO:
				return VOpCode.CmpVarConst.getSize() + VOpCode.JumpOnCmp.getSize();
		}
		return 0;
	}

	@Override
	public List<? extends ACompiledInstruction> compile(NCompileGraph g) {
		switch (cmd){
			case JUMP:
			case SWITCH:
				return ArraysEx.asList(VOpCode.Jump.createCall(getJumpTarget(g)));
			case JUMP_IF_ZERO:
				return ArraysEx.asList(
						VOpCode.CmpVarConst.createCall(VConstants.GP_REG_PRI, 0), 
						VOpCode.JumpOnCmp.createCall(VCmpResultRequest.EQUAL, getJumpTarget(g))
				);
		}
		return new ArrayList<>();
	}

	private int getJumpTarget(NCompileGraph g){
		return g.getInstructionByLabel(targetLabel).pointer - (pointer + getAllocatedPointerSpace(g));
	}
}
