
package ctrmap.pokescript.instructions.gen5.instructions;

import ctrmap.pokescript.instructions.abstractcommands.ACaseTable;
import ctrmap.pokescript.instructions.abstractcommands.ACompiledInstruction;
import ctrmap.pokescript.instructions.gen5.VCmpResultRequest;
import ctrmap.pokescript.instructions.gen5.VConstants;
import ctrmap.pokescript.instructions.gen5.VOpCode;
import ctrmap.pokescript.instructions.ntr.NTRInstructionCall;
import ctrmap.pokescript.stage1.NCompileGraph;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class VCaseTable extends ACaseTable {

	private static final int ONE_CASE_SIZEOF = VOpCode.CmpVarConst.getSize() + VOpCode.JumpIf.getSize();
	
	/*
	Since Gen V does not have proper switch/case function, it can be emulated with if/else if...
	This is very inefficient, but a sufficient fallback for this functionality.
	The structure is as follows:
	
	for each case in the table, insert:
	CmpVarConst(GP_REG_PRI, caseValue);
	JumpIf(VCmpResultRequest.EQUAL, caseJumpLabel);
	
	then the default case
	Jump(defaultJumpLabel)
	
	*/
	
	@Override
	public int getAllocatedPointerSpace(NCompileGraph g) {
		return targets.size() * ONE_CASE_SIZEOF + VOpCode.Jump.getSize();
	}

	@Override
	public List<? extends ACompiledInstruction> compile(NCompileGraph g) {
		List<NTRInstructionCall> l = new ArrayList<>();
	
		int caseIdx = 0;
		for (Map.Entry<Integer, String> caseEntry : targets.entrySet()){
			l.add(VOpCode.CmpVMVarConst.createCall(VConstants.GP_REG_PRI, caseEntry.getKey()));
			l.add(VOpCode.JumpIf.createCall(VCmpResultRequest.EQUAL, getJumpTarget(g, caseEntry.getValue(), caseIdx)));
			caseIdx++;
		}
		l.add(VOpCode.Jump.createCall(getJumpTarget(g, defaultCase, caseIdx) - VOpCode.Jump.getSize()));
		
		return l;
	}
	
	private int getJumpTarget(NCompileGraph g, String label, int caseIdx){
		return g.getInstructionByLabel(label).pointer - (pointer + (caseIdx + 1) * ONE_CASE_SIZEOF);
	}
}
