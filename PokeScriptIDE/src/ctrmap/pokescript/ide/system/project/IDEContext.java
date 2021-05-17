/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ctrmap.pokescript.ide.system.project;

import ctrmap.pokescript.ide.system.IDEResourceReference;
import ctrmap.pokescript.ide.system.project.remoteext.IRemoteExtResolver;
import ctrmap.stdlib.fs.FSFile;
import ctrmap.stdlib.gui.DialogUtils;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class IDEContext {

	public List<IRemoteExtResolver> remoteExtResolvers = new ArrayList<>();

	public List<IDEProject> openedProjects = new ArrayList<>();
	
	public boolean setUpProjectToContext(IDEProject prj){
		if (!openedProjects.contains(prj)){
			openedProjects.add(prj);
			prj.loadIncludes(this);
			return true;
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
		for (IRemoteExtResolver resolver : remoteExtResolvers) {
			if (resolver.getName().equals(ref.remoteExtType)) {
				return resolver.resolvePath(ref.path);
			}
		}
		DialogUtils.showErrorMessage("Could not resolve extended remote", "Extended remote handler " + ref.remoteExtType + " does not exist!");
		return null;
	}
}
