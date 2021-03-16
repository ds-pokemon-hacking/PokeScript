package ctrmap.pokescript.instructions.providers;

import ctrmap.pokescript.instructions.abstractcommands.ACompiledInstruction;
import ctrmap.pokescript.instructions.abstractcommands.MetaCall;
import ctrmap.pokescript.stage0.content.DeclarationContent;
import ctrmap.pokescript.stage1.NCompilableMethod;
import ctrmap.pokescript.stage1.NCompileGraph;
import java.util.List;

public interface MetaFunctionHandler {
	public void onDeclare(DeclarationContent cnt);
	public void onCompileBegin(NCompileGraph graph, NCompilableMethod method);
	public void onCompileEnd(NCompileGraph graph, NCompilableMethod method);
	
	public List<ACompiledInstruction> compileMetaCall(MetaCall call, NCompileGraph graph);
}
