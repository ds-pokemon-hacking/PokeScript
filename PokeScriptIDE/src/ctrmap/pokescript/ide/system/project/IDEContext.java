package ctrmap.pokescript.ide.system.project;

import ctrmap.pokescript.ide.system.IDEResourceReference;
import ctrmap.pokescript.ide.system.project.remoteext.IRemoteExtResolver;
import ctrmap.pokescript.ide.system.savedata.IDEWorkspace;
import ctrmap.stdlib.fs.FSFile;
import ctrmap.stdlib.fs.accessors.DiskFile;
import ctrmap.stdlib.gui.DialogUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a "state" of the IDE Workspace.
 */
public class IDEContext {
	
	private boolean locked = false;
	
	private IDEWorkspace workspace;

	public List<IRemoteExtResolver> remoteExtResolvers = new ArrayList<>();

	public List<IDEProject> openedProjects = new ArrayList<>();
	
	public boolean isLocked(){
		return locked;
	}
	
	public boolean isLoaded(){
		return workspace != null;
	}
	
	public boolean isAvailable(){
		return !isLocked() && isLoaded();
	}
	
	public void lock(){
		locked = true;
	}
	
	public void releaseLock(){
		locked = false;
	}
	
	public void loadWorkspace(IDEWorkspace ws){
		workspace = ws;
		openedProjects.clear();
	}
	
	public IDEWorkspace getWorkspace(){
		return workspace;
	}
	
	public boolean setUpProjectToContext(IDEProject prj){
		if (!openedProjects.contains(prj)){
			openedProjects.add(prj);
			prj.loadIncludes(this);
			return true;
		}
		return false;
	}
	
	public void closeProject(IDEProject prj){
		openedProjects.remove(prj);
		workspace.saveData.removeOpenedProjectPath(prj.getProjectPath());
	}
	
	public boolean hasOpenedProjectByPath(IDEProject proj){
		for (IDEProject p : openedProjects){
			if (p.getProjectPath().equals(proj.getProjectPath())){
				return true;
			}
		}
		return false;
	}
	
	public IDEProject getLoadedProject(FSFile projectFile) {
		FSFile projectRoot = projectFile.getParent();
		for (IDEProject prj : openedProjects) {
			if (prj.getRoot().equals(projectRoot)) {
				return prj;
			}
		}
		IDEProject prj = new IDEProject(projectFile);
		openedProjects.add(prj);
		return prj;
	}
	
	public IDEProject getProjectByProdId(String prodId){
		for (IDEProject p : openedProjects){
			if (p.getManifest().getProductId().equals(prodId)){
				return p;
			}
		}
		return null;
	}
	
	public void saveCacheData(){
		for (IDEProject proj : openedProjects){
			proj.saveCacheData();
		}
	}

	public void registerRemoteExtResolver(IRemoteExtResolver extResolver) {
		remoteExtResolvers.add(extResolver);
	}

	public FSFile resolveRemoteExt(IDEResourceReference ref) {
		return resolveRemoteExt(ref, null);
	}
	
	public FSFile resolveRemoteExt(IDEResourceReference ref, FSFile resolutionWorkDir) {
		if (resolutionWorkDir == null) {
			resolutionWorkDir = workspace.getWSDir(IDEWorkspace.WorkspaceDir.REMOTE_EXT_DATA);
		}
		for (IRemoteExtResolver resolver : remoteExtResolvers) {
			if (resolver.getName().equals(ref.remoteExtType)) {
				FSFile fsf = resolver.resolvePath(ref.path, this, resolutionWorkDir);
				if (fsf == null) {
					break;
				}
				return fsf;
			}
		}
		DialogUtils.showErrorMessage("Could not resolve extended remote", "Extended remote handler " + ref.remoteExtType + " does not exist!");
		return null;
	}
}
