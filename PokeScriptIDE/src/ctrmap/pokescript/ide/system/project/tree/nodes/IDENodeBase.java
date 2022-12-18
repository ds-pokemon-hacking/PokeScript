package ctrmap.pokescript.ide.system.project.tree.nodes;

import ctrmap.pokescript.ide.PSIDE;
import xstandard.gui.DialogUtils;
import xstandard.gui.components.tree.CustomJTreeNode;
import java.awt.event.MouseEvent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

public abstract class IDENodeBase extends CustomJTreeNode {

	protected PSIDE ide;

	public IDENodeBase(PSIDE ide) {
		super();
		this.ide = ide;
	}
	
	public abstract String getUniqueName();
	
	public String getUniquePath(){
		TreeNode parent = getParent();
		if (parent != null && parent instanceof IDENodeBase){
			return ((IDENodeBase)parent).getUniquePath() + "/" + getUniqueName();
		}
		return getUniqueName();
	}

	public ProjectNode ascendToProjectNode() {
		return ascendToNode(ProjectNode.class);
	}
	
	public LibraryReferenceNode ascendToLibRefNode() {
		return ascendToNode(LibraryReferenceNode.class);
	}
	
	public <T extends IDENodeBase> T ascendToNode(Class<T> cls) {
		TreeNode node = this;
		while (node != null && node.getClass() != cls) {
			node = node.getParent();
		}
		if (node != null && cls != node.getClass()){
			return null;
		}
		return (T)node;
	}

	public void callNodeAction(String action) {

	}

	public String[] getNodeActions() {
		return new String[0];
	}

	protected void showPopupMenu(MouseEvent evt, JMenuItem... items) {
		JPopupMenu menu = new JPopupMenu();
		for (JMenuItem i : items) {
			if (i != null) {
				menu.add(i);
			}
		}
		menu.show(evt.getComponent(), evt.getX(), evt.getY());
	}

	protected JMenuItem createDeleteMenuItem(String subject, Runnable onDelete) {
		JMenuItem delete = new JMenuItem("Delete");
		delete.addActionListener(((e) -> {
			if (DialogUtils.showYesNoDialog(ide, "Are you sure?", subject + " will be permanently removed from your hard drive.")) {
				onDelete.run();
			}
		}));
		return delete;
	}
}
