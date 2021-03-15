package ctrmap.pokescript.instructions.ctr.instructions;

import ctrmap.scriptformats.gen6.PawnInstruction;
import ctrmap.pokescript.instructions.abstractcommands.AAccessGlobal;
import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.stdlib.util.ArraysEx;
import java.util.List;

public abstract class PAccessGlobal extends AAccessGlobal {

	public PAccessGlobal(String name) {
		super(name);
	}

	protected abstract PawnInstruction getAccessIns(NCompileGraph g);

	@Override
	public int getAllocatedPointerSpace(NCompileGraph g) {
		return getAccessIns(g).getSize();
	}

	@Override
	public List<PawnInstruction> compile(NCompileGraph g) {
		return ArraysEx.asList(getAccessIns(g));
	}
	
	@Override
	public List<AInstruction> getAllInstructions(){
		return ArraysEx.asList(this);
	}
	
	public static class Write extends PAccessGlobal {

		public Write(String name) {
			super(name);
		}

		@Override
		public PawnInstruction getAccessIns(NCompileGraph g) {
			return new PawnInstruction(PawnInstruction.Commands.STOR_PRI, g.globals.getVariable(gName).getPointer(g));
		}
	}

	public static class Read extends PAccessGlobal {

		public Read(String name) {
			super(name);
		}

		@Override
		public PawnInstruction getAccessIns(NCompileGraph g) {
			return new PawnInstruction(PawnInstruction.Commands.LOAD_PRI, g.globals.getVariable(gName).getPointer(g));
		}
	}
}
