
package ctrmap.pokescript.types.classes;

import ctrmap.pokescript.InboundDefinition;
import ctrmap.pokescript.stage0.IModifiable;
import ctrmap.pokescript.stage0.Modifier;
import ctrmap.pokescript.types.TypeDef;
import java.util.ArrayList;
import java.util.List;

public class ClassDefinition implements IModifiable {
	public String className;

	public List<String> aliases = new ArrayList<>();
	public List<Modifier> modifiers = new ArrayList<>();
	
	public List<ClassField> variables = new ArrayList<>();
	public List<InboundDefinition> methods = new ArrayList<>();
	
	public boolean hasName(String name) {
		return name.equals(this.className) || aliases.contains(name);
	}
	
	public TypeDef getTypeDef(){
		return new TypeDef(className);
	}
	
	public int sizeOf(){
		//the size in cells, not bytes
		return variables.size();
	}

	@Override
	public List<Modifier> getModifiers() {
		return modifiers;
	}
}
