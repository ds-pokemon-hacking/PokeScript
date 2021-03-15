
package ctrmap.pokescript.types.classes;

import ctrmap.pokescript.stage1.NCompileGraph;
import java.util.ArrayList;
import java.util.List;

public class ClassDefinition {
	public String className;
	public boolean isNameAbsolute = false;
	public List<String> aliases = new ArrayList<>();
	
	public List<ClassField> variables = new ArrayList<>();
	
	public boolean hasName(String name) {
		return name.equals(this.className) || aliases.contains(name);
	}
	
	public int sizeOf(){
		//the size in cells, not bytes
		return variables.size();
	}
}
