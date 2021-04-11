package ctrmap.pokescript.instructions.providers.floatlib;

import ctrmap.pokescript.OutboundDefinition;
import ctrmap.pokescript.expr.Throughput;
import ctrmap.pokescript.instructions.abstractcommands.APlainOpCode;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.pokescript.types.AbstractTypeHandler;
import ctrmap.pokescript.types.DataType;
import ctrmap.stdlib.util.ArraysEx;

/**
 *
 */
public class PawnFloatHandler implements IFloatHandler {
	
	public static final PawnFloatHandler INSTANCE = new PawnFloatHandler();

	@Override
	public int floatToIntBits(float f) {
		return Float.floatToIntBits(f);
	}

	@Override
	public void castFloatToInt(AbstractTypeHandler.CastResult cast, NCompileGraph cg) {
		OutboundDefinition floatRoundCall = PawnFloatLib.floatround.createDummyOutbound();
		floatRoundCall.args[0] = Throughput.getPrimaryRegisterPushDummyThroughput(cg);
		floatRoundCall.args[1] = new Throughput(PawnFloatLib.PawnFloatRoundMethod.TO_ZERO.ordinal(), cg); //java-style rounding
		cast.instructions.add(cg.provider.getNativeCall(floatRoundCall));
	}

	@Override
	public void castIntToFloat(AbstractTypeHandler.CastResult cast, NCompileGraph cg) {
		OutboundDefinition flt = PawnFloatLib._float.createDummyOutbound();
		flt.args[0] = new Throughput(DataType.INT, ArraysEx.asList(cg.getPlain(APlainOpCode.PUSH_PRI)));
		cast.instructions.add(cg.provider.getNativeCall(flt));
	}

}
