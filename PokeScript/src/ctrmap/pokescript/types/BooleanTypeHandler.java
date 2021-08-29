package ctrmap.pokescript.types;

import ctrmap.pokescript.expr.Throughput;
import ctrmap.pokescript.stage0.CompilerPragma;
import ctrmap.pokescript.stage1.NCompileGraph;
/**
 *
 */
public class BooleanTypeHandler implements AbstractTypeHandler {

	@Override
	public Throughput tryAssign(String source, NCompileGraph cg) {
		boolean boolValue = false;
		source = source.trim();
		if (!source.equals("true") && !source.equals("false")){
			return null;
		}
		
		boolValue = Boolean.parseBoolean(source);
		return new Throughput(boolValue, cg);
	}

	@Override
	public CastResult getInstructionForCast(DataType castedType, NCompileGraph cg) {	
		CastResult r = new CastResult();
		if (castedType != DataType.BOOLEAN && !cg.getIsBoolPragmaEnabledSimple(CompilerPragma.ALLOW_UNSAFE_CASTS)) {
			r.success = false;
			r.exception = "Can not cast a boolean.";
		}
		return r;
	}

}
