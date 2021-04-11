package ctrmap.pokescript.instructions.providers.floatlib;

import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.pokescript.types.AbstractTypeHandler;

/**
 *
 */
public class FXFloatHandler implements IFloatHandler{

	public static final int FX_SCALE = 4096;
	
	public static final FXFloatHandler INSTANCE = new FXFloatHandler();
	
	@Override
	public int floatToIntBits(float f) {
		return (int)(f * 4096);
	}

	@Override
	public void castFloatToInt(AbstractTypeHandler.CastResult cast, NCompileGraph cg) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void castIntToFloat(AbstractTypeHandler.CastResult cast, NCompileGraph cg) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

}
