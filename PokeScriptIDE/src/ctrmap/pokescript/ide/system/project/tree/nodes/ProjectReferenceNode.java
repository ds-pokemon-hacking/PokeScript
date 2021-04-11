package ctrmap.pokescript.ide.system.project.tree.nodes;

import ctrmap.pokescript.ide.system.project.IDEProject;
import ctrmap.stdlib.gui.components.tree.CustomJTreeNode;

public class ProjectReferenceNode extends CustomJTreeNode {

	public static int RESID = 5;

	//we don't need to construct the whole project, just the manifest
	private IDEProject project;
	
	public ProjectReferenceNode(IDEProject proj) {
		this.project = proj;
	}

	@Override
	public int getIconResourceID() {
		return RESID;
	}

	@Override
	public String getNodeName() {
		return project.getManifest().getProductName();
	}
}
