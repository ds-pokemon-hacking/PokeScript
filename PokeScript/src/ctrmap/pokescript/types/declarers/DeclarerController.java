package ctrmap.pokescript.types.declarers;

import ctrmap.pokescript.InboundDefinition;
import ctrmap.pokescript.LangConstants;
import ctrmap.pokescript.data.Variable;
import ctrmap.pokescript.stage0.Modifier;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.pokescript.types.classes.ClassDefinition;

public class DeclarerController {

	private NCompileGraph cg;

	public StaticDeclarer staticDecl;

	public ClassDeclarer classDecl;

	public DeclarerController(NCompileGraph cg) {
		this.cg = cg;
		staticDecl = new StaticDeclarer(cg);
	}

	public void setPackage(String packageName) {
		cg.packageName = packageName;
	}

	public ClassDefinition beginClass(String className) {
		beginClassImpl(className, false);
		return classDecl.def;
	}

	public ClassDefinition beginEnum(String enumName) {
		beginClassImpl(enumName, true);
		return classDecl.def;
	}

	private void beginClassImpl(String name, boolean isEnum) {
		classDecl = new ClassDeclarer(name, isEnum);
		if (cg.packageName != null) {
			classDecl.def.className = LangConstants.makePath(cg.packageName, classDecl.def.className);
		}
		cg.classDefs.add(classDecl.def);
	}

	public void endClassOrEnum() {
		classDecl = null;
	}

	public void addGlobal(Variable.Global glb) {
		if (glb.hasModifier(Modifier.STATIC)) {
			staticDecl.addGlobal(glb);
		} else {
			if (classDecl != null) {
				classDecl.addGlobal(glb);
			}
		}
	}

	public void addMethod(InboundDefinition method) {
		if (method.hasModifier(Modifier.STATIC) || method.hasModifier(Modifier.NATIVE)) {
			staticDecl.addMethod(method);
		} else {
			if (classDecl != null) {
				classDecl.addMethod(method);
			}
		}
	}
}
