package ctrmap.pokescript.ide.system.project.remoteext;

import ctrmap.stdlib.fs.FSFile;

public interface IRemoteExtResolver {
	public String getName();
	public FSFile resolvePath(String path);
}
