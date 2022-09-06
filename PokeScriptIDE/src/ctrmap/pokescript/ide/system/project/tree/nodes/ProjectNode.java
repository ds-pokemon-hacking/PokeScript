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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JMenuItem;

public class ProjectNode extends IDENodeBase {

	public static int RESID = 0;

	private IDEProject project;

	private Map<IInclude, IDENodeBase> depNodes = new HashMap<>();
	private final ContainerNode libs = new ContainerNode(ide, ContainerNode.Type.LIBRARIES);

	public ProjectNode(PSIDE ide, IDEProject proj) {
		super(ide);
		project = proj;

		for (IDEFile sourceDir : proj.getSourceDirs()) {
			add(new SourceDirNode(ide, sourceDir));
		}

		updateDependencyNodes();

		add(libs);
	}

	public void updateDependencyNodes() {
		Collection<IInclude> includes = project.includes.values();
		for (IInclude inc : includes) {
			if (!depNodes.containsKey(inc)) {
				IDENodeBase n = null;
				switch (inc.getDepType()) {
					case LIBRARY:
						n = new LibraryReferenceNode(ide, ((LibraryInclude) inc).getLibrary());
						break;
					case DIRECTORY:
						n = new SourceDirNode(ide, new IDEFile(project, ((SimpleInclude) inc).getDir()));
						break;
					case PROJECT:
						n = new ProjectReferenceNode(ide, ((ProjectInclude) inc).getProject());
						break;
					case INVALID:
						n = new InvalidReferenceNode(ide, ((InvalidInclude) inc).invalidPath);
						break;
				}
				if (n != null) {
					depNodes.put(inc, n);
					libs.add(n);
				}
			}
		}
		List<IInclude> childrenToRemove = new ArrayList<>();
		for (IInclude i : depNodes.keySet()) {
			if (!includes.contains(i)) {
				childrenToRemove.add(i);
			}
		}
		for (IInclude i : childrenToRemove) {
			libs.remove(depNodes.remove(i));
		}
	}

	@Override
	public void onNodePopupInvoke(MouseEvent evt) {
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

	public IDEProject getProject() {
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
