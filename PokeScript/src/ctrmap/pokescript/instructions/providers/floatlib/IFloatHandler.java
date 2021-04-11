package ctrmap.pokescript.instructions.providers.floatlib;

import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.pokescript.types.AbstractTypeHandler;

public interface IFloatHandler {
	public int floatToIntBits(float f);
	
	public void castFloatToInt(AbstractTypeHandler.CastResult cast, NCompileGraph cg);
	public void castIntToFloat(AbstractTypeHandler.CastResult cast, NCompileGraph cg);
}
