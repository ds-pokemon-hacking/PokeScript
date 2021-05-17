package ctrmap.pokescript.ide.system.project.tree.nodes;

import ctrmap.pokescript.ide.PSIDE;
import ctrmap.pokescript.ide.system.project.IDEFile;
import ctrmap.stdlib.fs.FSFile;
import ctrmap.stdlib.fs.FSUtil;

public class ClassNode extends IDENodeBase {
	public static int RESID = 3;
	
	private FSFile classFile;
	
	public ClassNode(PSIDE ide, FSFile classFile){
		super(ide);
		this.classFile = classFile;
	}
	
	@Override
	public int getIconResourceID() {
		return RESID;
	}
	
	@Override
	public void onNodeSelected(){
		ide.openFile(new IDEFile(ascendToProjectNode().getProject(), classFile));
	}

	@Override
	public String getNodeName() {
		return FSUtil.getFileNameWithoutExtension(classFile.getName());
	}
}
