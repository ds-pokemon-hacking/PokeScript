package ctrmap.pokescript.instructions.gen5.metahandlers;

import ctrmap.pokescript.instructions.abstractcommands.ACompiledInstruction;
import ctrmap.pokescript.instructions.abstractcommands.MetaCall;
import ctrmap.pokescript.instructions.gen5.VConstants;
import ctrmap.pokescript.instructions.gen5.VOpCode;
import ctrmap.pokescript.instructions.gen5.instructions.VLocalCall;
import ctrmap.pokescript.instructions.providers.MetaFunctionHandler;
import ctrmap.pokescript.stage0.content.DeclarationContent;
import ctrmap.pokescript.stage1.NCompilableMethod;
import ctrmap.pokescript.stage1.NCompileGraph;
import java.util.ArrayList;
import java.util.List;

public class VExternFuncHandler implements MetaFunctionHandler {

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
	public void onCompileBegin(NCompileGraph graph, NCompilableMethod method) {
	}

	@Override
	public void onCompileEnd(NCompileGraph graph, NCompilableMethod method) {

	}

	@Override
	public List<ACompiledInstruction> compileMetaCall(MetaCall call, NCompileGraph g) {
		List<ACompiledInstruction> l = new ArrayList<>();

		VLocalCall.compilePreCall(l, call, g, VConstants.GP_REG_PRI); //the global call locals start overwrite the reserved primary register

		if (async) {
			l.add(VOpCode.GlobalCallAsync.createCall(SCRID));
		} else {
			l.add(VOpCode.GlobalCall.createCall(SCRID));
		}

		VLocalCall.compilePostCall(l, g); //pop variables back from the stack

		return l;
	}
}
