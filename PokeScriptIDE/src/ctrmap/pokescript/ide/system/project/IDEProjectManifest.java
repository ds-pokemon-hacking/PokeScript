package ctrmap.pokescript.ide.system.project;

import ctrmap.pokescript.LangPlatform;
import ctrmap.pokescript.ide.system.project.include.Dependency;
import ctrmap.scriptformats.pkslib.LibraryManifest;
import ctrmap.scriptformats.pkslib.PlatformSourceTarget;
import ctrmap.stdlib.formats.yaml.YamlNode;
import ctrmap.stdlib.fs.FSFile;
import java.util.ArrayList;
import java.util.List;

public class IDEProjectManifest extends LibraryManifest {
	
	public static final String DEFAULT_SOURCE_DIR = "src";
	
	public IDEProjectManifest(FSFile fsf) {
		super(fsf);
	}
	
	public IDEProjectManifest(FSFile fsf, String projectName, String productId, LangPlatform platform){
		this(fsf);
		
		setProductName(projectName);
		setProductId(productId);
		
		setSupportedPlatforms(new PlatformSourceTarget(platform, DEFAULT_SOURCE_DIR));
		
		saveProjectData();
	}
	
	public String getManifestPath(){
		return document.getPath();
	}
	
	public List<Dependency> getProjectDependencies(){
		List<Dependency> l = new ArrayList<>();
		for (YamlNode dn : getRootNodeKeyNode(ProjectAttributes.AK_PROJECT_DEPS).children){
			l.add(new Dependency(dn));
		}
		return l;
	}
	
	public void addDependency(Dependency dep){
		YamlNode depsNode = getEnsureRootNodeKeyNode(ProjectAttributes.AK_PROJECT_DEPS);
		if (!getProjectDependencies().contains(dep)){
			depsNode.addChild(dep.createNode());
		}
		saveProjectData();
	}
	
	public void saveProjectData(){
		write();
	}
}
