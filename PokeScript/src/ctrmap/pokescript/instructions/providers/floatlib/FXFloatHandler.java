package ctrmap.pokescript.instructions.providers.floatlib;

import ctrmap.pokescript.instructions.abstractcommands.APlainOpCode;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.pokescript.types.AbstractTypeHandler;

public class FXFloatHandler implements IFloatHandler{

	public static final int FX_SCALE = 4096;
	
	public static final FXFloatHandler INSTANCE = new FXFloatHandler();
	
	@Override
	public int floatToIntBits(float f) {
		return (int)(f * FX_SCALE);
	}

	@Override
	public void castFloatToInt(AbstractTypeHandler.CastResult cast, NCompileGraph cg) {
		cast.instructions.add(cg.getPlain(APlainOpCode.CONST_ALT, FX_SCALE));
		cast.instructions.add(cg.getPlain(APlainOpCode.DIVIDE));
	}

	@Override
	public void castIntToFloat(AbstractTypeHandler.CastResult cast, NCompileGraph cg) {
		cast.instructions.add(cg.getPlain(APlainOpCode.CONST_ALT, FX_SCALE));
		cast.instructions.add(cg.getPlain(APlainOpCode.MULTIPLY));
	}

	@Override
	public boolean isFixedPoint() {
		return true;
	}

}
