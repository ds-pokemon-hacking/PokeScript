/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ctrmap.pokescript.ide.system.project.include;

import ctrmap.pokescript.LangPlatform;
import ctrmap.stdlib.fs.FSFile;
import ctrmap.stdlib.util.ArraysEx;
import java.util.List;

/**
 *
 */
public class SimpleInclude implements IInclude {

	private FSFile dir;
	
	public SimpleInclude(FSFile file){
		dir = file;
	}
	
	public FSFile getDir(){
		return dir;
	}
	
	@Override
	public List<FSFile> getIncludeSources(LangPlatform plaf) {
		return ArraysEx.asList(dir);
	}

	@Override
	public String getProductID() {
		return null;
	}

	@Override
	public DependencyType getDepType() {
		return DependencyType.DIRECTORY;
	}

}
