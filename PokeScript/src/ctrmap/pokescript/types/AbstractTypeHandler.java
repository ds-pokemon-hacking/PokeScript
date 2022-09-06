
package ctrmap.pokescript.types;

import ctrmap.pokescript.expr.Throughput;
import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.stage1.NCompileGraph;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public interface AbstractTypeHandler {
	public Throughput tryAssign(String source, NCompileGraph cg);
	public CastResult getInstructionForCast(DataType castedType, NCompileGraph cg);
	
	public static class CastResult {
		public boolean success = true;
		public String exception;
		public List<AInstruction> instructions = new ArrayList<>();
	}
}
