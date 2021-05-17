package ctrmap.pokescript.ide.system;

import ctrmap.stdlib.formats.yaml.Yaml;
import ctrmap.stdlib.fs.FSFile;
import java.util.ArrayList;
import java.util.List;

public class IDESetupFile extends Yaml {
	
	public List<String> platformInfoPaths;
	public List<String> compilerDefinitions;
	
	public IDESetupFile(FSFile fsf){
		super(fsf);
		
		platformInfoPaths = getRootNodeKeyNode("PlatformInfos").getChildValuesAsListStr();
		compilerDefinitions = getRootNodeKeyNode("Definitions").getChildValuesAsListStr();
	}
	
	public List<PlatformInfoFile> getPlatformInfoFiles(){
		List<PlatformInfoFile> list = new ArrayList<>();
		for (String pip : platformInfoPaths){
			list.add(new PlatformInfoFile(document.getParent().getChild(pip)));
		}
		return list;
	}
}
