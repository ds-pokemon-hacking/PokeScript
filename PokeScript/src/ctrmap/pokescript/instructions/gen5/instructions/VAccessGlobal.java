package ctrmap.pokescript.instructions.gen5.instructions;

import ctrmap.pokescript.instructions.abstractcommands.AAccessGlobal;
import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.instructions.gen5.VConstants;
import ctrmap.pokescript.instructions.gen5.VOpCode;
import ctrmap.pokescript.instructions.ntr.NTRInstructionCall;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.stdlib.util.ArraysEx;
import java.util.List;

public abstract class VAccessGlobal extends AAccessGlobal {

	public VAccessGlobal(String name) {
		super(name);
	}

	protected abstract NTRInstructionCall getAccessIns(NCompileGraph g);

	@Override
	public int getAllocatedPointerSpace(NCompileGraph g) {
		return getAccessIns(g).getSize();
	}

	@Override
	public List<NTRInstructionCall> compile(NCompileGraph g) {
		return ArraysEx.asList(getAccessIns(g));
	}
	
	@Override
	public List<AInstruction> getAllInstructions(){
		return ArraysEx.asList(this);
	}
	
	public static class Write extends VAccessGlobal {

		public Write(String name) {
			super(name);
		}

		@Override
		public NTRInstructionCall getAccessIns(NCompileGraph g) {
			return VOpCode.VarUpdateVar.createCall(VConstants.VAR_TOP_GLOBAL + g.globals.getVariable(gName).getPointer(g), VConstants.GP_REG_PRI);
		}
	}

	public static class Read extends VAccessGlobal {

		public Read(String name) {
			super(name);
		}

		@Override
		public NTRInstructionCall getAccessIns(NCompileGraph g) {
			return VOpCode.VarUpdateVar.createCall(VConstants.GP_REG_PRI, VConstants.VAR_TOP_GLOBAL + g.globals.getVariable(gName).getPointer(g));
		}
	}
}
