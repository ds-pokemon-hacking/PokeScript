package ctrmap.pokescript.ide.system.project.include;

import ctrmap.pokescript.LangPlatform;
import xstandard.fs.FSFile;
import java.util.ArrayList;
import java.util.List;

public class InvalidInclude implements IInclude {

	public final String invalidPath;
	
	public InvalidInclude(String invalidPath){
		this.invalidPath = invalidPath;
	}
	
	@Override
	public DependencyType getDepType() {
		return DependencyType.INVALID;
	}

	@Override
	public List<FSFile> getIncludeSources(LangPlatform plaf) {
		return new ArrayList<>();
	}

	@Override
	public String getProductID() {
		return null;
	}

}
