package ctrmap.pokescript.ide.system.project.tree.nodes;

import ctrmap.pokescript.ide.PSIDE;
import ctrmap.pokescript.ide.system.project.IDEProject;

public class ProjectReferenceNode extends IDENodeBase {

	public static int RESID = 5;

	//we don't need to construct the whole project, just the manifest
	private IDEProject project;
	
	public ProjectReferenceNode(PSIDE ide, IDEProject proj) {
		super(ide);
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

	@Override
	public String getUniqueName() {
		return project.getManifest().getProductId();
	}
}
