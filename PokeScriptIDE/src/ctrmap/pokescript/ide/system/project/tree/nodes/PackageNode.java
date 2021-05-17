package ctrmap.pokescript.ide.system.project.tree.nodes;

import ctrmap.pokescript.LangConstants;
import ctrmap.pokescript.ide.PSIDE;
import ctrmap.pokescript.ide.system.project.IDEProject;
import ctrmap.stdlib.fs.FSFile;

public class PackageNode extends IDENodeBase {

	public static int RESID = 2;

	private FSFile dir;

	public PackageNode(PSIDE ide, FSFile dir) {
		this(ide);
		this.dir = dir;

		addChildrenFromDir(dir);
	}
	
	protected PackageNode(PSIDE ide){
		super(ide);
	}
	
	protected void addChildrenFromDir(FSFile directory){
		for (FSFile child : directory.listFiles()) {
			if (child.isDirectory()) {
				add(new PackageNode(ide, child));
			} else {
				if (LangConstants.isLangFile(child.getName())) {
					add(new ClassNode(ide, child));
				}
			}
		}
	}

	@Override
	public int getIconResourceID() {
		return RESID;
	}

	@Override
	public String getNodeName() {
		return dir.getName();
	}
}
