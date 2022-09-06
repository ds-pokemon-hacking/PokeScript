package ctrmap.pokescript.types;

import ctrmap.pokescript.LangConstants;
import java.util.Objects;

/**
 *
 */
public class TypeDef {

	public DataType baseType;

	public String className;

	public TypeDef(String className) {
		this(className, false);
	}

	public TypeDef(TypeDef td) {
		this.baseType = td.baseType;
		this.className = td.className;
	}

	public TypeDef(String className, boolean isEnum) {
		baseType = DataType.fromName(className);
		if (baseType == null || baseType == DataType.CLASS) {
			if (isEnum) {
				baseType = DataType.ENUM;
			} else {
				baseType = DataType.CLASS;
			}
			this.className = className;
		}
	}

	TypeDef(DataType baseType) {
		this.baseType = baseType;
	}

	public boolean isClass() {
		return baseType == DataType.CLASS || baseType == DataType.VAR_CLASS;
	}

	public boolean isEnum() {
		return baseType == DataType.ENUM || baseType == DataType.VAR_ENUM;
	}

	public boolean isClassOrEnum() {
		return isClass() || isEnum();
	}

	public String getClassName() {
		if (isClassOrEnum()) {
			return className;
		}
		return baseType.getFriendlyName();
	}

	@Override
	public String toString() {
		String cn = getClassName();
		return (cn == null) ? baseType.toString() : (baseType + ":" + getClassName());
	}

	public String toFriendliestString() {
		String cn = getClassName();
		if (cn != null) {
			return LangConstants.getLastPathElem(cn);
		}
		return baseType.toString().toLowerCase();
	}

	public boolean acceptsIncoming(TypeDef incomingType) {
		if (incomingType == null) {
			return false;
		}
		if (!this.equals(incomingType)) {
			if (isClass() || possiblyDowncast(incomingType.baseType) != getCompatEnumType(baseType)) {
				if (baseType == DataType.FLOAT && possiblyDowncast(incomingType.baseType) == DataType.INT) {
					return true;
				}
				return false;
			}
		}
		return true;
	}

	public static DataType getCompatEnumType(DataType t) {
		switch (t) {
			case ENUM:
			case VAR_ENUM:
				return DataType.INT;
		}
		return t;
	}

	public static DataType possiblyDowncast(DataType t) {
		switch (t) {
			case VAR_BOOLEAN:
				return DataType.BOOLEAN;
			case VAR_INT:
			case VAR_ENUM:
			case ENUM:
				return DataType.INT;
			case VAR_FLOAT:
				return DataType.FLOAT;
			default:
				return t;
		}
	}

	@Override
	public boolean equals(Object o) {
		if (o != null && o instanceof TypeDef) {
			TypeDef a = (TypeDef) o;
			String cn1 = a.getClassName();
			String cn2 = getClassName();
			return Objects.equals(cn1, cn2);
		}
		return false;
	}
}
