package ctrmap.pokescript.ide.system.project.tree.nodes;

import ctrmap.pokescript.ide.PSIDE;
import ctrmap.pokescript.ide.system.project.IDEFile;
import ctrmap.stdlib.fs.FSUtil;
import ctrmap.stdlib.gui.DialogUtils;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import javax.swing.JMenuItem;

public class ClassNode extends IDENodeBase {

	public static int RESID = 3;

	private IDEFile classFile;

	public ClassNode(PSIDE ide, IDEFile classFile) {
		super(ide);
		this.classFile = classFile;
	}

	public IDEFile getClassFile() {
		return classFile;
	}

	@Override
	public int getIconResourceID() {
		return RESID;
	}

	private boolean isMainClass() {
		String projectMainClass = ascendToProjectNode().getProject().getManifest().getMainClass();
		if (classFile.getPathInProject().equals(projectMainClass)) {
			return true;
		}
		return false;
	}

	@Override
	public void onNodePopupInvoke(MouseEvent evt) {
		if (classFile.canWrite()) {
			JMenuItem setMainClass = new JMenuItem("Set Main class");
			setMainClass.addActionListener((ActionEvent e) -> {
				ascendToProjectNode().getProject().getManifest().setMainClass(classFile.getPathInProject());
				ide.getProjectTree().repaint();
			});
			if (isMainClass()){
				setMainClass.setEnabled(false);
			}
			
			JMenuItem delete = createDeleteMenuItem("\"" + classFile.getNameWithoutExtension() + "\"", (() -> {
				if (isMainClass()) {
					DialogUtils.showErrorMessage(ide, "Forbidden operation", "The main class of a project can not be deleted.");
				} else {
					ide.deleteFile(classFile);
					ide.getProjectTree().removeFileByNode(this);
				}
			}));
			showPopupMenu(evt, setMainClass, delete);
		}
	}

	@Override
	public void onNodeSelected() {
		ide.openFile(new IDEFile(ascendToProjectNode().getProject(), classFile, ascendToLibRefNode() != null));
	}

	@Override
	public String getNodeName() {
		return FSUtil.getFileNameWithoutExtension(classFile.getName()) + (isMainClass() ? " (main class)" : "");
	}

	@Override
	public String getUniqueName() {
		return getNodeName();
	}
}
