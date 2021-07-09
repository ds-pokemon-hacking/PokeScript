package ctrmap.pokescript.ide.system.project.tree.nodes;

import ctrmap.pokescript.ide.PSIDE;
import ctrmap.pokescript.ide.system.project.IDEFile;

public class SourceDirNode extends PackageNode {

	public static int RESID = 4;

	public SourceDirNode(PSIDE ide, IDEFile dir) {
		super(ide, dir);
	}

	@Override
	protected boolean isDeletable(){
		return false;
	}
	
	@Override
	public int getIconResourceID() {
		return RESID;
	}
}
