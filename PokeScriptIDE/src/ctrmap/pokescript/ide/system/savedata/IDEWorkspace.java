/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ctrmap.pokescript.ide.system.savedata;

import ctrmap.stdlib.fs.accessors.DiskFile;
import java.io.File;

public class IDEWorkspace {
	public static String IDE_DIR_FILE_NAME = ".ide";
	
	private File workspaceRoot;
	
	private File ideDir;
	
	public IDESaveData saveData;
	
	public IDEWorkspace(File f){
		workspaceRoot = f;
		
		ideDir = new File(workspaceRoot + "/" + IDE_DIR_FILE_NAME);
		if (!ideDir.exists()){
			ideDir.mkdirs();
		}
		
		saveData = new IDESaveData(new DiskFile(getWSFile(WorkspaceFile.IDE_SAVE_DATA)));
	}
	
	public static IDEWorkspace openWorkspaceIfApplicable(File f){
		if (f == null || !f.exists() || !f.isDirectory()){
			return null;
		}
		File ideDir = new File(f + "/" + IDE_DIR_FILE_NAME);
		if (!ideDir.exists() || !ideDir.isDirectory()){
			return null;
		}
		return new IDEWorkspace(f);
	}
	
	public File getRoot(){
		return workspaceRoot;
	}
	
	public final File getWSFile(WorkspaceFile f){
		File fsf = new File(ideDir + "/" + f.path);
		File parent = fsf.getParentFile();
		if (parent != null && !parent.exists()) {
			parent.mkdirs();
		}
		return fsf;
	}
	
	public final File getProjectDir(String projectName){
		return new File(workspaceRoot + "/" + projectName);
	}
	
	public enum WorkspaceFile {
		IDE_SAVE_DATA("IDESaveData.yml"),
		IDE_SETTINGS("settings/IDE.yml")
		;
		private final String path;
			
		private WorkspaceFile(String path){
			this.path = path;
		}
	}
}
