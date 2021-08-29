package ctrmap.pokescript.types.declarers;

import ctrmap.pokescript.InboundDefinition;
import ctrmap.pokescript.data.Variable;
import ctrmap.pokescript.types.classes.ClassDefinition;
import ctrmap.pokescript.types.classes.ClassField;
import ctrmap.pokescript.types.classes.EnumDefinition;

public class ClassDeclarer implements IDeclarer {

	public ClassDefinition def;

	public ClassDeclarer(String className, boolean isEnum) {
		if (isEnum) {
			def = new EnumDefinition();
		} else {
			def = new ClassDefinition();
		}
		def.className = className;
	}

	@Override
	public void addGlobal(Variable.Global glb) {
		ClassField fld = new ClassField(def);
		fld.type = glb.typeDef;
		fld.variableName = glb.name;
	}

	@Override
	public void addMethod(InboundDefinition def) {
		this.def.methods.add(def);
	}

}
