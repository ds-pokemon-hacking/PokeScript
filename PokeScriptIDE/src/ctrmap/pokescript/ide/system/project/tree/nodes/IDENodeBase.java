/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ctrmap.pokescript.ide.system.project.tree.nodes;

import ctrmap.pokescript.ide.PSIDE;
import ctrmap.stdlib.gui.components.tree.CustomJTreeNode;
import javax.swing.tree.TreeNode;

public abstract class IDENodeBase extends CustomJTreeNode {
	protected PSIDE ide;
	
	public IDENodeBase(PSIDE ide){
		super();
		this.ide = ide;
	}
	
	public ProjectNode ascendToProjectNode(){
		TreeNode node = this;
		while (node != null && !(node instanceof ProjectNode)){
			node = node.getParent();
		}
		return (ProjectNode)node;
	}
	
	public void callNodeAction(String action){
		
	}
	
	public String[] getNodeActions(){
		return new String[0];
	}
}
