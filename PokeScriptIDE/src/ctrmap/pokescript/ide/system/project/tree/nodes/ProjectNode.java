package ctrmap.pokescript.ide.system.project.tree.nodes;

import ctrmap.pokescript.ide.PSIDE;
import ctrmap.pokescript.ide.system.project.IDEProject;
import ctrmap.pokescript.ide.system.project.include.IInclude;
import ctrmap.pokescript.ide.system.project.include.InvalidInclude;
import ctrmap.pokescript.ide.system.project.include.LibraryInclude;
import ctrmap.pokescript.ide.system.project.include.ProjectInclude;
import ctrmap.pokescript.ide.system.project.include.SimpleInclude;
import ctrmap.stdlib.fs.FSFile;

public class ProjectNode extends IDENodeBase {
	
	public static final String NACT_PROJECT_PROPERTIES = "ProjectProperties";

	public static int RESID = 0;

	private IDEProject project;

	public ProjectNode(PSIDE ide, IDEProject proj) {
		super(ide);
		project = proj;

		for (FSFile sourceDir : proj.getSourceDirs()) {
			add(new SourceDirNode(ide, sourceDir));
		}

		ContainerNode libs = new ContainerNode(ContainerNode.Type.LIBRARIES);

		for (IInclude inc : project.includes) {
			switch (inc.getDepType()) {
				case LIBRARY:
					libs.add(new LibraryReferenceNode(ide, ((LibraryInclude) inc).getLibrary()));
					break;
				case DIRECTORY:
					libs.add(new SourceDirNode(ide, ((SimpleInclude)inc).getDir()));
					break;
				case PROJECT:
					libs.add(new ProjectReferenceNode(ide, ((ProjectInclude)inc).getProject()));
					break;
				case INVALID:
					libs.add(new InvalidReferenceNode(ide, ((InvalidInclude)inc).invalidPath));
					break;
			}
		}
		
		add(libs);
	}
	
	public IDEProject getProject(){
		return project;
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
