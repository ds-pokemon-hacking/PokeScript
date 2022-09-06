package ctrmap.pokescript.ide.system.project.include;

import ctrmap.pokescript.LangPlatform;
import xstandard.fs.FSFile;
import java.util.List;

public interface IInclude {
	public DependencyType getDepType();
	public List<FSFile> getIncludeSources(LangPlatform plaf);
	public String getProductID();
}
