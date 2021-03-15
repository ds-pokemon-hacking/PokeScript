package ctrmap.pokescript.types;

/**
 *
 */
public class TypeDef {

	public DataType baseType;
	public String className;
	
	public TypeDef(String className){
		baseType = DataType.fromName(className);
		if (baseType == null || baseType == DataType.CLASS){
			baseType = DataType.CLASS;
			this.className = className;
		}
	}

	public boolean isClass(){
		return baseType == DataType.CLASS;
	}
	
	public String getClassName() {
		if (baseType == DataType.CLASS) {
			return className;
		}
		return baseType.getFriendlyName();
	}

	@Override
	public String toString(){
		return getClassName();
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
