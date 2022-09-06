/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ctrmap.pokescript.ide.system.project.include;

import ctrmap.pokescript.LangPlatform;
import ctrmap.pokescript.ide.system.project.IDEProject;
import xstandard.fs.FSFile;
import java.util.List;

/**
 *
 */
public class ProjectInclude implements IInclude {

	private IDEProject proj;
	
	public ProjectInclude(IDEProject projectFile){
		proj = projectFile;
	}
	
	public IDEProject getProject(){
		return proj;
	}
	
	@Override
	public List<FSFile> getIncludeSources(LangPlatform plaf) {
		List<FSFile> underlyingIncludes = proj.getAllIncludeFiles();
		underlyingIncludes.add(proj.getSourceDirForPlatform(plaf));
		return underlyingIncludes;
	}

	@Override
	public String getProductID() {
		return proj.getManifest().getProductId();
	}

	@Override
	public DependencyType getDepType() {
		return DependencyType.PROJECT;
	}

}
