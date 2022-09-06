/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ctrmap.pokescript.ide.system.project.tree;

import ctrmap.pokescript.ide.PSIDE;
import ctrmap.pokescript.ide.system.project.IDEProject;
import ctrmap.pokescript.ide.system.project.tree.nodes.ClassNode;
import ctrmap.pokescript.ide.system.project.tree.nodes.ContainerNode;
import ctrmap.pokescript.ide.system.project.tree.nodes.IDENodeBase;
import ctrmap.pokescript.ide.system.project.tree.nodes.InvalidReferenceNode;
import ctrmap.pokescript.ide.system.project.tree.nodes.LibraryReferenceNode;
import ctrmap.pokescript.ide.system.project.tree.nodes.PackageNode;
import ctrmap.pokescript.ide.system.project.tree.nodes.ProjectNode;
import ctrmap.pokescript.ide.system.project.tree.nodes.ProjectReferenceNode;
import ctrmap.pokescript.ide.system.project.tree.nodes.SourceDirNode;
import xstandard.gui.components.tree.CustomJTree;
import xstandard.res.ResourceAccess;
import java.util.ArrayList;
import java.util.List;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

public class IDEProjectTree extends CustomJTree {

	private PSIDE ide;

	public IDEProjectTree() {
		super();

		registerIconResource(SourceDirNode.RESID, "sourcefolder");
		registerIconResource(LibraryReferenceNode.RESID, "library");
		registerIconResource(PackageNode.RESID, "package");
		registerIconResource(ClassNode.RESID, "sourcefile");
		registerIconResource(ContainerNode.Type.LIBRARIES.getResId(), "librarydir");
		registerIconResource(ProjectNode.RESID, "project");
		registerIconResource(ProjectReferenceNode.RESID, "project");
		registerIconResource(InvalidReferenceNode.RESID, "invalid");
	}

	public void attachIDE(PSIDE ide) {
		this.ide = ide;
	}

	public void addProject(IDEProject project) {
		model.insertNodeInto(new ProjectNode(ide, project), root, root.getChildCount());
		if (root.getChildCount() == 1){
			reload();
		}
	}

	public void removeProject(IDEProject project) {
		for (int i = 0; i < root.getChildCount(); i++) {
			TreeNode ch = root.getChildAt(i);
			if (ch instanceof ProjectNode) {
				if (((ProjectNode) ch).getProject() == project) {
					model.removeNodeFromParent((MutableTreeNode) ch);
					return;
				}
			}
		}
	}

	public List<ProjectNode> getProjectNodes() {
		List<ProjectNode> nodes = new ArrayList<>();
		for (int i = 0; i < root.getChildCount(); i++) {
			TreeNode ch = root.getChildAt(i);
			if (ch instanceof ProjectNode) {
				nodes.add((ProjectNode)ch);
			}
		}
		return nodes;
	}

	public ProjectNode findProjectNode(IDEProject proj) {
		for (int i = 0; i < root.getChildCount(); i++) {
			TreeNode ch = root.getChildAt(i);
			if (ch instanceof ProjectNode) {
				ProjectNode pn = (ProjectNode) ch;
				if (pn.getProject() == proj) {
					return pn;
				}
			}
		}
		return null;
	}
	
	public void addFileByNode(IDENodeBase node, IDENodeBase parent, int index) {
		model.insertNodeInto(node, parent, index);
	}

	public void removeFileByNode(IDENodeBase node) {
		model.removeNodeFromParent(node);
	}

	private void registerIconResource(int resID, String name) {
		registerIconResourceImpl(resID, "ctrmap/resources/scripting/ui/tree/" + name + ".png");
	}
}
