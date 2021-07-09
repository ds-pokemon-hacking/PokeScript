/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ctrmap.pokescript.ide.system.savedata;

import ctrmap.pokescript.ide.system.project.IDEFile;
import ctrmap.pokescript.ide.system.project.tree.IDEProjectTree;
import ctrmap.stdlib.formats.yaml.Yaml;
import ctrmap.stdlib.formats.yaml.YamlListElement;
import ctrmap.stdlib.formats.yaml.YamlNode;
import ctrmap.stdlib.formats.yaml.YamlReflectUtil;
import ctrmap.stdlib.fs.FSFile;
import java.util.ArrayList;
import java.util.List;

public class IDESaveData extends Yaml {

	public static final String KEY_PROJECT_LIST = "Projects";
	public static final String KEY_PROJECT_STATE = "IDEState";

	public static final String PS_KEY_OPEN_FILES = "OpenFiles";
	public static final String PS_KEY_OPEN_FILE = "LastOpenFile";
	public static final String PS_KEY_TREE_STATE = "TreeExpansionState";

	public List<String> openedProjectPaths = new ArrayList<>();

	public IDEFileReference lastOpenedFile;
	private ProjectTreeExpansionState treeExpansionState;
	public List<IDEFileReference> openedFilePaths = new ArrayList<>();

	public IDESaveData(FSFile f) {
		super(f);

		if (f.exists()) {
			readData();
		}
	}
	
	public void saveTreeExpansionState(IDEProjectTree tree){
		treeExpansionState = new ProjectTreeExpansionState(tree);
		write();
	}
	
	public void loadTreeExpansionState(IDEProjectTree tree){
		if (treeExpansionState != null){
			treeExpansionState.loadTreeExpansionState(tree);
		}
	}

	public void readData() {
		YamlNode openedProjects = getRootNodeKeyNode(KEY_PROJECT_LIST);
		if (openedProjects != null) {
			openedProjectPaths = openedProjects.getChildValuesAsListStr();
		}

		YamlNode projectState = getRootNodeKeyNode(KEY_PROJECT_STATE);

		if (projectState != null) {
			YamlNode openedFiles = projectState.getChildByName(PS_KEY_OPEN_FILES);

			if (openedFiles != null) {
				for (YamlNode child : openedFiles.children) {
					openedFilePaths.add(YamlReflectUtil.deserialize(child, IDEFileReference.class));
				}
			}

			YamlNode openFile = projectState.getChildByName(PS_KEY_OPEN_FILE);
			if (openFile != null) {
				lastOpenedFile = YamlReflectUtil.deserialize(openFile, IDEFileReference.class);
			}
			
			YamlNode treeExpState = projectState.getChildByName(PS_KEY_TREE_STATE);
			if (treeExpState != null) {
				treeExpansionState = new ProjectTreeExpansionState(treeExpState);
			}
		}
	}

	@Override
	public void write() {
		List<String> usedProjPaths = new ArrayList<>();
		for (int i = 0; i < openedProjectPaths.size(); i++) {
			String opp = openedProjectPaths.get(i);
			if (usedProjPaths.contains(opp)) {
				openedProjectPaths.remove(i);
				i--;
			} else {
				usedProjPaths.add(opp);
			}
		}

		YamlNode openedProjects = getEnsureRootNodeKeyNode(KEY_PROJECT_LIST);
		openedProjects.removeAllChildren();

		for (String opp : openedProjectPaths) {
			YamlNode opNode = new YamlNode(new YamlListElement());
			opNode.addChild(new YamlNode(opp));
			openedProjects.addChild(opNode);
		}

		YamlNode state = getEnsureRootNodeKeyNode(KEY_PROJECT_STATE);

		YamlNode openedFiles = state.getOrCreateChildKeyByName(PS_KEY_OPEN_FILES);
		openedFiles.removeAllChildren();

		for (IDEFileReference ref : openedFilePaths) {
			openedFiles.addChild(ref.getYML());
		}

		state.removeChildByName(PS_KEY_OPEN_FILE);
		if (lastOpenedFile != null) {
			state.addChild(YamlReflectUtil.serialize(PS_KEY_OPEN_FILE, lastOpenedFile));
		}
		
		state.removeChildByName(PS_KEY_TREE_STATE);
		if (treeExpansionState != null){
			state.addChild(treeExpansionState);
		}

		super.write();
	}

	public void setLastOpenedFile(IDEFile f) {
		lastOpenedFile = new IDEFileReference(f);
		write();
	}

	public void putOpenedProjectPath(String pth) {
		if (!openedProjectPaths.contains(pth)) {
			openedProjectPaths.add(pth);
			write();
		}
	}

	public void removeOpenedProjectPath(String pth) {
		if (openedProjectPaths.remove(pth)) {
			write();
		}
	}

	public void putOpenFile(IDEFile file) {
		putOpenFile(file, true);
	}

	public void putOpenFile(IDEFile file, boolean write) {
		IDEFileReference ref = new IDEFileReference(file);
		if (!openedFilePaths.contains(ref)) {
			openedFilePaths.add(ref);
			if (write) {
				write();
			}
		}
	}

	public void removeOpenFile(IDEFile f) {
		IDEFileReference ref = new IDEFileReference(f);
		boolean write = false;
		if (lastOpenedFile.equals(ref)) {
			lastOpenedFile = null;
			write = true;
		}
		write |= openedFilePaths.remove(new IDEFileReference(f));
		if (write) {
			write();
		}
	}

	public void clearOpenedFiles() {
		openedFilePaths.clear();
	}

	public static class IDEFileReference {

		public String projectProdId;
		public String path;
		public boolean openRW;

		public IDEFileReference() {

		}

		public IDEFileReference(IDEFile f) {
			path = f.getPathInProject();
			projectProdId = f.getProject().getManifest().getProductId();
			openRW = f.canWrite();
		}

		public YamlNode getYML() {
			YamlNode n = new YamlNode(new YamlListElement());
			YamlReflectUtil.addFieldsToNode(n, this);
			return n;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}
			if (obj != null && obj instanceof IDEFileReference) {
				IDEFileReference r = (IDEFileReference) obj;
				return r.path.equals(path) && r.projectProdId.equals(projectProdId);
			}
			return false;
		}

	}
}
