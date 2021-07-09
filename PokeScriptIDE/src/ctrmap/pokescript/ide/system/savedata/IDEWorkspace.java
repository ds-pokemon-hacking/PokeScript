package ctrmap.pokescript.ide.system.savedata;

import ctrmap.stdlib.fs.FSFile;

/**
 * Handles access to the IDE's files in a workspace.
 */
public class IDEWorkspace {
	public static String IDE_DIR_FILE_NAME = ".ide";
	
	private FSFile workspaceRoot;
	
	private FSFile ideDir;
	
	public IDESaveData saveData;
	
	public IDEWorkspace(FSFile f){
		workspaceRoot = f;
		
		ideDir = workspaceRoot.getChild(IDE_DIR_FILE_NAME);
		if (!ideDir.exists()){
			ideDir.mkdirs();
		}
		
		saveData = new IDESaveData(getWSFile(WorkspaceFile.IDE_SAVE_DATA));
	}
	
	public static IDEWorkspace openWorkspaceIfApplicable(FSFile f){
		if (f == null || !f.exists() || !f.isDirectory()){
			return null;
		}
		FSFile ideDir = f.getChild(IDE_DIR_FILE_NAME);
		if (!ideDir.exists() || !ideDir.isDirectory()){
			return null;
		}
		return new IDEWorkspace(f);
	}
	
	public FSFile getRoot(){
		return workspaceRoot;
	}
	
	public final FSFile getWSFile(WorkspaceFile f){
		FSFile fsf = ideDir.getChild(f.path);
		FSFile parent = fsf.getParent();
		if (parent != null && !parent.exists()) {
			parent.mkdirs();
		}
		return fsf;
	}
	
	public final FSFile getWSDir(WorkspaceDir f){
		FSFile fsf = ideDir.getChild(f.path);
		fsf.mkdirs();
		return fsf;
	}
	
	public final FSFile getProjectDir(String projectName){
		return workspaceRoot.getChild(projectName);
	}
	
	public enum WorkspaceFile {
		IDE_SAVE_DATA("IDESaveData.yml"),
		IDE_SETTINGS("settings/IDE.yml"),
		;
		private final String path;
			
		private WorkspaceFile(String path){
			this.path = path;
		}
	}
	
	public enum WorkspaceDir {
		REMOTE_EXT_DATA("RemoteExtData")
		;
		private final String path;
			
		private WorkspaceDir(String path){
			this.path = path;
		}
	}
}
