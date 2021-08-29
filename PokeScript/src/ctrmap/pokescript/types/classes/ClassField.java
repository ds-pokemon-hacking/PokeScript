
package ctrmap.pokescript.types.classes;

import ctrmap.pokescript.types.TypeDef;

/**
 *
 */
public class ClassField {
	private ClassDefinition cls;
	
	public String variableName;
	public TypeDef type;
	
	public ClassField(ClassDefinition cls){
		this.cls = cls;
	}
}
