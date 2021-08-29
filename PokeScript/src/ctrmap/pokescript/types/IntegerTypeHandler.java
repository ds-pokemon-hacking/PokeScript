
package ctrmap.pokescript.types;

import ctrmap.pokescript.expr.Throughput;
import ctrmap.pokescript.stage0.CompilerPragma;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.stdlib.util.ParsingUtils;

/**
 *
 */
public class IntegerTypeHandler implements AbstractTypeHandler{

	@Override
	public Throughput tryAssign(String source, NCompileGraph cg) {
		int intValue = 0;

		try {
			intValue = ParsingUtils.parseBasedInt(source);
			return new Throughput(intValue, cg);
		} catch (NumberFormatException ex) {

		}
		return null;
	}

	@Override
	public CastResult getInstructionForCast(DataType castedType, NCompileGraph cg) {
		CastResult r = new CastResult();
		r.success = true;
		switch (castedType){
			case INT:
				return r;
			case FLOAT:
				cg.provider.getFloatingPointHandler().castIntToFloat(r, cg);
				return r;
			default:
				if (cg.getIsBoolPragmaEnabledSimple(CompilerPragma.ALLOW_UNSAFE_CASTS)) {
					return r;
				}
				break;
		}
		r.success = false;
		r.exception = "Integer can not be casted to " + castedType;
		return r;
	}

}
