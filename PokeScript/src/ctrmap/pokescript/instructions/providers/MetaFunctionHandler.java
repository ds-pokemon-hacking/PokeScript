package ctrmap.pokescript.instructions.providers;

import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.instructions.abstractcommands.MetaCall;
import ctrmap.pokescript.stage0.content.DeclarationContent;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.pokescript.stage2.AbstractCodeFactory;

public interface MetaFunctionHandler<I> {
	public void onDeclare(DeclarationContent cnt);
		
	public boolean assembleMetaInstruction(AbstractCodeFactory<I> assembler, AInstruction instruction, NCompileGraph graph);
	public I compileMetaCall(AbstractCodeFactory<I> assembler, MetaCall call, NCompileGraph graph);
	public void linkCall(I source, I target);
}
