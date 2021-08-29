package ctrmap.pokescript.types.classes;

import ctrmap.pokescript.types.DataType;
import ctrmap.pokescript.types.TypeDef;

public class EnumDefinition extends ClassDefinition {
	@Override
	public TypeDef getTypeDef() {
		TypeDef td = new TypeDef(className);
		td.baseType = DataType.ENUM;
		return td;
	}
}
