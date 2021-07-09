package ctrmap.pokescript.ide.system.project.remoteext;

import ctrmap.pokescript.ide.system.project.IDEContext;
import ctrmap.stdlib.fs.FSFile;

public interface IRemoteExtResolver {
	public String getName();
	public FSFile resolvePath(String path, IDEContext context, FSFile workDir);
}
