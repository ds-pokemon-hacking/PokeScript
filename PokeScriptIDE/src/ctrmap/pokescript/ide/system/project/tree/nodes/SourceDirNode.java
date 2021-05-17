package ctrmap.pokescript.ide.system.project.tree.nodes;

import ctrmap.pokescript.ide.PSIDE;
import ctrmap.stdlib.fs.FSFile;

public class SourceDirNode extends PackageNode {

	public static int RESID = 4;

	public SourceDirNode(PSIDE ide, FSFile dir) {
		super(ide, dir);
	}

	@Override
	public int getIconResourceID() {
		return RESID;
	}
}
