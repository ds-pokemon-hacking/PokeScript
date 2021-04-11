package ctrmap.pokescript.ide.system.project.tree.nodes;

import ctrmap.stdlib.fs.FSFile;

public class SourceDirNode extends PackageNode {

	public static int RESID = 4;

	public SourceDirNode(FSFile dir) {
		super(dir);
	}

	@Override
	public int getIconResourceID() {
		return RESID;
	}
}
