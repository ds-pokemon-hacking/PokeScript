
package ctrmap.pokescript.instructions.abstractcommands;

import ctrmap.pokescript.OutboundDefinition;
import ctrmap.pokescript.stage1.NCompilableMethod;
import ctrmap.pokescript.stage1.NCompileGraph;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class MetaCall extends ALocalCall {
	
	public MetaCall(OutboundDefinition call) {
		super(call);
	}

	@Override
	public List<? extends ACompiledInstruction> compile(NCompileGraph cg){
		NCompilableMethod method = cg.getMethodByDef(call);
		
		if (method != null && method.metaHandler != null){
			return method.metaHandler.compileMetaCall(this, cg);
		}
		else {
			return new ArrayList<>();
		}
	}

	@Override
	public int getAllocatedPointerSpace(NCompileGraph cg) {
		int ptr = 0;
		List<? extends ACompiledInstruction> precomp = compile(cg);
		for (ACompiledInstruction i : precomp) {
			ptr += i.getSize();
		}
		return ptr;
	}
}
