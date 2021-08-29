package ctrmap.pokescript.instructions.providers.floatlib;

import ctrmap.pokescript.InboundDefinition;
import ctrmap.pokescript.stage0.content.DeclarationContent;
import ctrmap.pokescript.types.DataType;

public class PawnFloatLib {

	public static final String LIBRARY_NAME = "Float";

	public static final InboundDefinition _float = InboundDefinition.makeResidentNative("float",
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
	public static final InboundDefinition floatround = InboundDefinition.makeResidentNative("floatround",
			new DeclarationContent.Argument[]{
				new DeclarationContent.Argument("value", DataType.FLOAT.typeDef()),
				new DeclarationContent.Argument("roundingMethod", DataType.INT.typeDef())
			},
			DataType.INT.typeDef()
	);

	public static enum PawnFloatRoundMethod {
		ROUND,
		FLOOR,
		CEILING,
		TO_ZERO
	}
}
