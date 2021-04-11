package ctrmap.pokescript.ide.system.project.tree.nodes;

import ctrmap.pokescript.ide.system.project.IDEProject;
import ctrmap.pokescript.ide.system.project.include.IInclude;
import ctrmap.pokescript.ide.system.project.include.InvalidInclude;
import ctrmap.pokescript.ide.system.project.include.LibraryInclude;
import ctrmap.pokescript.ide.system.project.include.ProjectInclude;
import ctrmap.pokescript.ide.system.project.include.SimpleInclude;
import ctrmap.stdlib.fs.FSFile;
import ctrmap.stdlib.gui.components.tree.CustomJTreeNode;

public class ProjectNode extends CustomJTreeNode {

	public static int RESID = 0;

	private IDEProject project;

	public ProjectNode(IDEProject proj) {
		project = proj;

		for (FSFile sourceDir : proj.getSourceDirs()) {
			add(new SourceDirNode(sourceDir));
		}

		ContainerNode libs = new ContainerNode(ContainerNode.Type.LIBRARIES);

		for (IInclude inc : project.includes) {
			switch (inc.getDepType()) {
				case LIBRARY:
					libs.add(new LibraryNode(((LibraryInclude) inc).getLibrary()));
					break;
				case DIRECTORY:
					libs.add(new SourceDirNode(((SimpleInclude)inc).getDir()));
					break;
				case PROJECT:
					libs.add(new ProjectReferenceNode(((ProjectInclude)inc).getProject()));
					break;
				case INVALID:
					libs.add(new InvalidReferenceNode(((InvalidInclude)inc).invalidPath));
					break;
			}
		}
		
		add(libs);
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
