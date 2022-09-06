package ctrmap.pokescript.instructions.gen5.metahandlers;

import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.instructions.abstractcommands.MetaCall;
import ctrmap.pokescript.instructions.gen5.VConstants;
import ctrmap.pokescript.instructions.gen5.VOpCode;
import ctrmap.pokescript.instructions.ntr.NTRInstructionCall;
import ctrmap.pokescript.stage0.content.DeclarationContent;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.pokescript.stage2.AbstractCodeFactory;
import ctrmap.pokescript.stage2.VCodeFactory;

public class VExternFuncHandler implements VMetaFuncHandler {

	private final int SCRID;
	private final boolean async;

	public VExternFuncHandler(int SCRID, boolean async) {
		this.SCRID = SCRID;
		this.async = async;
	}

	@Override
	public void onDeclare(DeclarationContent cnt) {

	}

	@Override
	public boolean assembleMetaInstruction(AbstractCodeFactory<NTRInstructionCall> assembler, AInstruction instruction, NCompileGraph graph) {
		return true; //... but do nothing. These shan't have a body.
	}

	@Override
	public NTRInstructionCall compileMetaCall(AbstractCodeFactory<NTRInstructionCall> assembler, MetaCall call, NCompileGraph graph) {
		VCodeFactory.compilePreCall(assembler, call, graph, VConstants.GP_REG_PRI);

		assembler.addInstruction((async ? VOpCode.GlobalCallAsync : VOpCode.GlobalCall).createCall(SCRID));

		VCodeFactory.compilePostCall(assembler, call);

		return null;
	}

	@Override
	public void linkCall(NTRInstructionCall source, NTRInstructionCall target) {
	}
}
