package ctrmap.pokescript.ide.system.project;

import ctrmap.pokescript.LangPlatform;
import ctrmap.pokescript.ide.system.project.include.Dependency;
import ctrmap.scriptformats.pkslib.LibraryManifest;
import ctrmap.scriptformats.pkslib.PlatformSourceTarget;
import xstandard.formats.yaml.YamlListElement;
import xstandard.formats.yaml.YamlNode;
import xstandard.fs.FSFile;
import java.util.ArrayList;
import java.util.List;

public class IDEProjectManifest extends LibraryManifest {

	public static final String DEFAULT_SOURCE_DIR = "src";

	public IDEProjectManifest(FSFile fsf) {
		super(fsf);
	}

	public IDEProjectManifest(FSFile fsf, String projectName, String productId, LangPlatform platform) {
		this(fsf);

		setProductName(projectName);
		setProductId(productId);

		setSupportedPlatforms(new PlatformSourceTarget(platform, DEFAULT_SOURCE_DIR));

		saveProjectData();
	}

	public String getManifestPath() {
		return document.getPath();
	}

	public String getMainClass() {
		if (root.hasChildren(ProjectAttributes.AK_MAIN_CLASS)) {
			return root.getChildValue(ProjectAttributes.AK_MAIN_CLASS);
		}
		return null;
	}

	public void setMainClass(String classPath) {
		root.getEnsureChildByName(ProjectAttributes.AK_MAIN_CLASS).setValue(classPath);
		saveProjectData();
	}

	public List<Dependency> getProjectDependencies() {
		List<Dependency> l = new ArrayList<>();
		if (root.hasChildren(ProjectAttributes.AK_PROJECT_DEPS)) {
			for (YamlNode dn : getRootNodeKeyNode(ProjectAttributes.AK_PROJECT_DEPS).children) {
				l.add(new Dependency(dn));
			}
		}
		return l;
	}

	public boolean addDependency(Dependency dep) {
		YamlNode depsNode = getEnsureRootNodeKeyNode(ProjectAttributes.AK_PROJECT_DEPS);
		if (!getProjectDependencies().contains(dep)) {
			depsNode.addChild(dep.createNode());
			saveProjectData();
			return true;
		}
		return false;
	}

	public List<String> getCompilerDefinitions() {
		return getEnsureRootNodeKeyNode(ProjectAttributes.AK_COMPILE_DEFS).getChildValuesAsListStr();
	}

	public void setCompilerDefinitions(List<String> defs) {
		YamlNode n = getEnsureRootNodeKeyNode(ProjectAttributes.AK_COMPILE_DEFS);
		n.removeAllChildren();
		for (String s : defs) {
			YamlNode listElem = new YamlNode(new YamlListElement());
			listElem.addChildValue(s);
			n.addChild(listElem);
		}
		saveProjectData();
	}

	public void addCompilerDefinition(String def) {
		if (def != null) {
			YamlNode listElem = new YamlNode(new YamlListElement());
			listElem.addChildValue(def);
			getEnsureRootNodeKeyNode(ProjectAttributes.AK_COMPILE_DEFS).addChild(listElem);
			saveProjectData();
		}
	}

	public void removeCompilerDefinition(String def) {
		getEnsureRootNodeKeyNode(ProjectAttributes.AK_COMPILE_DEFS).removeChildByValue(def);
	}

	public void saveProjectData() {
		write();
	}
}
