/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ctrmap.pokescript.ide.system.project.include;

import ctrmap.pokescript.LangPlatform;
import ctrmap.scriptformats.pkslib.LibraryFile;
import xstandard.fs.FSFile;
import xstandard.util.ArraysEx;
import java.util.List;

/**
 *
 */
public class LibraryInclude implements IInclude {

	private LibraryFile lib;
	
	public LibraryInclude(LibraryFile file){
		lib = file;
	}
	
	public LibraryFile getLibrary(){
		return lib;
	}
	
	@Override
	public List<FSFile> getIncludeSources(LangPlatform plaf) {
		return ArraysEx.asList(lib.getSourceDirForPlatform(plaf));
	}

	@Override
	public String getProductID() {
		return lib.getManifest().getProductId();
	}

	@Override
	public DependencyType getDepType() {
		return DependencyType.LIBRARY;
	}

}
