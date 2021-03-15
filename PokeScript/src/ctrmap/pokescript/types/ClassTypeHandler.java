package ctrmap.pokescript.types;

import ctrmap.pokescript.expr.Throughput;
import ctrmap.pokescript.stage1.NCompileGraph;

/**
 *
 */
public class ClassTypeHandler implements AbstractTypeHandler {

	@Override
	public Throughput tryAssign(String source, NCompileGraph cg) {
		return null;
	}

	@Override
	public CastResult getInstructionForCast(DataType castedType, NCompileGraph cg) {
		CastResult r = new CastResult();
		r.success = false;
		r.exception = "Can not cast a class.";
		return r;
	}

}
