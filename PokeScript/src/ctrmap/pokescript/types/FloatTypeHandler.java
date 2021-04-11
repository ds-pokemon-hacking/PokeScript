package ctrmap.pokescript.types;

import ctrmap.pokescript.instructions.providers.floatlib.PawnFloatLib;
import ctrmap.pokescript.OutboundDefinition;
import ctrmap.pokescript.expr.Throughput;
import ctrmap.pokescript.stage1.NCompileGraph;


/**
 *
 */
public class FloatTypeHandler implements AbstractTypeHandler {

	@Override
	public Throughput tryAssign(String source, NCompileGraph cg) {
		if (source.endsWith("f")) {
			source = source.substring(0, source.length() - 1);
			//floating point
			float floatValue = 0f;
			try {
				floatValue = Float.parseFloat(source);
				return new Throughput(floatValue, cg);
			} catch (NumberFormatException ex) {

			}
		}
		return null;
	}

	@Override
	public CastResult getInstructionForCast(DataType castedType, NCompileGraph cg) {
		CastResult r = new CastResult();
		switch (castedType){
			case FLOAT:
				return r;
			case INT:
				cg.provider.getFloatingPointHandler().castFloatToInt(r, cg);
				return r;
		}
		r.exception = "Float can not be casted to " + castedType;
		r.success = false;
		return r;
	}

}
