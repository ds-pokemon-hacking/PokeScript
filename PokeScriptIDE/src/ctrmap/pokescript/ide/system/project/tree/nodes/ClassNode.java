package ctrmap.pokescript.ide.system.project.tree.nodes;

import ctrmap.stdlib.fs.FSFile;
import ctrmap.stdlib.fs.FSUtil;
import ctrmap.stdlib.gui.components.tree.CustomJTreeNode;

public class ClassNode extends CustomJTreeNode {
	public static int RESID = 3;
	
	private FSFile classFile;
	
	public ClassNode(FSFile classFile){
		this.classFile = classFile;
	}
	
	@Override
	public int getIconResourceID() {
		return RESID;
	}

	@Override
	public String getNodeName() {
		return FSUtil.getFileNameWithoutExtension(classFile.getName());
	}
}
