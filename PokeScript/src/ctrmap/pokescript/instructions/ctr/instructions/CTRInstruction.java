package ctrmap.pokescript.instructions.ctr.instructions;

import ctrmap.pokescript.instructions.abstractcommands.ACompiledInstruction;
import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.scriptformats.gen6.PawnInstruction;
import java.util.ArrayList;
import java.util.List;

public abstract class CTRInstruction extends AInstruction {

	public static List<PawnInstruction> compileIL(List<AInstruction> il, NCompileGraph g){
		List<PawnInstruction> r = new ArrayList<>();
		for (AInstruction i : il){
			List<? extends ACompiledInstruction> compiled = i.compile(g);
			for (ACompiledInstruction ci : compiled){
				if (ci instanceof PawnInstruction){
					r.add((PawnInstruction)ci);
				}
				else {
					throw new UnsupportedOperationException("Attempted to compile a non-Pawn instruction in a Pawn context.");
				}
			}
		}
		return r;
	}
}
