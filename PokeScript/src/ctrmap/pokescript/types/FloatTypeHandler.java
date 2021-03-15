package ctrmap.pokescript.types;

import ctrmap.pokescript.FloatLib;
import ctrmap.pokescript.OutboundDefinition;
import ctrmap.pokescript.expr.Throughput;
import ctrmap.pokescript.instructions.ctr.instructions.CTRInstruction;
import ctrmap.pokescript.instructions.ctr.instructions.PNativeCall;
import ctrmap.pokescript.stage1.NCompileGraph;
import java.util.ArrayList;
import java.util.List;

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
				OutboundDefinition floatRoundCall = FloatLib.floatround.createDummyOutbound();
				floatRoundCall.args[0] = Throughput.getPrimaryRegisterPushDummyThroughput(cg);
				floatRoundCall.args[1] = new Throughput(FloatLib.PawnFloatRoundMethod.TO_ZERO.ordinal(), cg); //java-style rounding
				r.instructions.add(cg.provider.getNativeCall(floatRoundCall));
				return r;
		}
		r.exception = "Float can not be casted to " + castedType;
		r.success = false;
		return r;
	}

}
