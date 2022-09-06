package ctrmap.pokescript.ide.system.savedata;

import ctrmap.pokescript.ide.system.project.tree.IDEProjectTree;
import ctrmap.pokescript.ide.system.project.tree.nodes.IDENodeBase;
import ctrmap.pokescript.ide.system.project.tree.nodes.ProjectNode;
import xstandard.formats.yaml.YamlNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

public class ProjectTreeExpansionState extends YamlNode {

	private ProjectTreeExpansionState(){
		setKey(IDESaveData.PS_KEY_TREE_STATE);
	}
	
	public ProjectTreeExpansionState(YamlNode node) {
		this();
		children.addAll(node.children);
	}

	public ProjectTreeExpansionState(IDEProjectTree tree) {
		this();
		for (int i = 0; i < tree.getRowCount(); i++) {
			if (tree.isExpanded(i)) {
				TreePath path = tree.getPathForRow(i);
				if (path.getLastPathComponent() instanceof IDENodeBase) {
					addChildListElem().addChildValue(((IDENodeBase) path.getLastPathComponent()).getUniquePath());
				}
			}
		}
	}

	public void loadTreeExpansionState(IDEProjectTree tree) {
		HashSet<String> expandedNodes = new HashSet<>();

		for (YamlNode ch : children) {
			expandedNodes.add(ch.getValue());
		}

		for (ProjectNode pn : tree.getProjectNodes()){
			tryExpandNodeRecursive(pn, expandedNodes, tree);
		}
	}

	private void tryExpandNodeRecursive(IDENodeBase node, HashSet<String> expandedNodes, IDEProjectTree tree) {
		if (expandedNodes.contains(node.getUniquePath())) {
			TreePath treePath = createPath(node);
			tree.expandPath(treePath);

			for (int i = 0; i < node.getChildCount(); i++) {
				TreeNode ch = node.getChildAt(i);
				if (ch instanceof IDENodeBase) {
					tryExpandNodeRecursive((IDENodeBase) ch, expandedNodes, tree);
				}
			}
		}
	}

	private static TreePath createPath(TreeNode node) {
		List<Object> elems = new ArrayList<>();

		while (node != null) {
			elems.add(0, node);
			node = node.getParent();
		}
		TreePath path = new TreePath(elems.toArray(new Object[elems.size()]));
		return path;
	}
}
