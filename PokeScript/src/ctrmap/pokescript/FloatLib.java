package ctrmap.pokescript;

import ctrmap.pokescript.stage0.content.DeclarationContent;
import ctrmap.pokescript.types.DataType;
import ctrmap.pokescript.types.TypeDef;

public class FloatLib {
	public static final String LIBRARY_NAME = "Float";
	
	public static final InboundDefinition _float = new InboundDefinition("float", 
			new DeclarationContent.Argument[]{
				new DeclarationContent.Argument("value", DataType.INT.typeDef())
			},
			DataType.FLOAT.typeDef());
	
	/*
	floatround(float value, int roundMethod);
	
	roundMethod:
	0 = round
	1 = floor
	2 = ceil
	3 = toZero
	*/
	public static final InboundDefinition floatround = new InboundDefinition("floatround", 
			new DeclarationContent.Argument[]{
				new DeclarationContent.Argument("value", DataType.FLOAT.typeDef()),
				new DeclarationContent.Argument("roundingMethod", DataType.INT.typeDef())
			},
			DataType.INT.typeDef());
	
	public static enum PawnFloatRoundMethod {
		ROUND,
		FLOOR,
		CEILING,
		TO_ZERO
	}
}
