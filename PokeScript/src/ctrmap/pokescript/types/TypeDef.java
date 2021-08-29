package ctrmap.pokescript.types;

import ctrmap.pokescript.LangConstants;

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
		if (cn != null){
			return LangConstants.getLastPathElem(cn);
		}
		return baseType.toString().toLowerCase();
	}

	@Override
	public boolean equals(Object o) {
		if (o != null && o instanceof TypeDef) {
			TypeDef a = (TypeDef) o;
			return a.getClassName().equals(getClassName());
		}
		return false;
	}
}
