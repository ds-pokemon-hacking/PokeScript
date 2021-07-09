package ctrmap.pokescript.ide.system.project.tree.nodes;

import ctrmap.pokescript.ide.PSIDE;
import ctrmap.pokescript.ide.forms.settings.project.ProjectSettings;
import ctrmap.pokescript.ide.system.project.IDEFile;
import ctrmap.pokescript.ide.system.project.IDEProject;
import ctrmap.pokescript.ide.system.project.include.IInclude;
import ctrmap.pokescript.ide.system.project.include.InvalidInclude;
import ctrmap.pokescript.ide.system.project.include.LibraryInclude;
import ctrmap.pokescript.ide.system.project.include.ProjectInclude;
import ctrmap.pokescript.ide.system.project.include.SimpleInclude;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import javax.swing.JMenuItem;

public class ProjectNode extends IDENodeBase {
	
	public static int RESID = 0;

	private IDEProject project;

	public ProjectNode(PSIDE ide, IDEProject proj) {
		super(ide);
		project = proj;

		for (IDEFile sourceDir : proj.getSourceDirs()) {
			add(new SourceDirNode(ide, sourceDir));
		}

		ContainerNode libs = new ContainerNode(ide, ContainerNode.Type.LIBRARIES);

		for (IInclude inc : project.includes) {
			switch (inc.getDepType()) {
				case LIBRARY:
					libs.add(new LibraryReferenceNode(ide, ((LibraryInclude) inc).getLibrary()));
					break;
				case DIRECTORY:
					libs.add(new SourceDirNode(ide, new IDEFile(proj, ((SimpleInclude)inc).getDir())));
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
	
	@Override
	public void onNodePopupInvoke(MouseEvent evt){
		JMenuItem close = new JMenuItem("Close");
		close.addActionListener((ActionEvent e) -> {
			ide.closeProject(project);
		});
		JMenuItem delete = createDeleteMenuItem("The project", (() -> {
			ide.deleteProject(project);
		}));
		JMenuItem properties = new JMenuItem("Properties");
		properties.addActionListener((ActionEvent e) -> {
			ProjectSettings settings = new ProjectSettings(ide, project);
			settings.setVisible(true);
		});
		showPopupMenu(evt, close, delete, properties);
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

	@Override
	public String getUniqueName() {
		return project.getManifest().getProductId();
	}
}
