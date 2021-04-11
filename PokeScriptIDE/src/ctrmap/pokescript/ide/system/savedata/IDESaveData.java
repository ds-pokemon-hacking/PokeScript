/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ctrmap.pokescript.ide.system.savedata;

import ctrmap.stdlib.formats.yaml.Yaml;
import ctrmap.stdlib.formats.yaml.YamlListElement;
import ctrmap.stdlib.formats.yaml.YamlNode;
import ctrmap.stdlib.fs.FSFile;
import java.util.ArrayList;
import java.util.List;

public class IDESaveData extends Yaml {
	public static final String KEY_PROJECT_LIST = "Projects";
	public static final String KEY_PROJECT_STATE = "IDEState";
	
	public static final String PS_KEY_OPEN_FILES = "OpenFiles";
	
	public static final String OF_KEY_FILE_PATH = "Path";
	public static final String OF_KEY_PROJECT_UID = "ParentProject";
	
	public List<String> openedProjectPaths = new ArrayList<>();
	
	public IDESaveData(FSFile f){
		super(f);
		
		if (f.exists()){
			readData();
		}
	}
	
	public void readData(){
		YamlNode openedProjects = getRootNodeKeyNode(KEY_PROJECT_LIST);
		if (openedProjects != null){
			openedProjectPaths = openedProjects.getChildValuesAsListStr();
		}
	}
	
	public void writeData(){
		YamlNode openedProjects = getEnsureRootNodeKeyNode(KEY_PROJECT_LIST);
		openedProjects.removeAllChildren();
		for (String opp : openedProjectPaths){
			YamlNode opNode = new YamlNode();
			YamlListElement elem = new YamlListElement(opNode);
			opNode.addChild(new YamlNode(opp));
			openedProjects.addChild(opNode);
		}
		
		write();
	}
	
	public void putOpenedProjectPath(String pth){
		if (!openedProjectPaths.contains(pth)){
			openedProjectPaths.add(pth);
			writeData();
		}
	}
}
