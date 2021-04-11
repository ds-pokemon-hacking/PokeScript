package ctrmap.pokescript.ide.system.project.tree.nodes;

import ctrmap.pokescript.LangConstants;
import ctrmap.stdlib.fs.FSFile;
import ctrmap.stdlib.gui.components.tree.CustomJTreeNode;

public class PackageNode extends CustomJTreeNode {

	public static int RESID = 2;

	private FSFile dir;

	public PackageNode(FSFile dir) {
		this.dir = dir;

		addChildrenFromDir(dir);
	}
	
	public PackageNode(){
		
	}
	
	protected void addChildrenFromDir(FSFile directory){
		for (FSFile child : directory.listFiles()) {
			if (child.isDirectory()) {
				add(new PackageNode(child));
			} else {
				if (LangConstants.isLangFile(child.getName())) {
					add(new ClassNode(child));
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
