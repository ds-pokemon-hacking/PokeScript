/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ctrmap.pokescript.ide.system.project.tree.nodes;

import ctrmap.stdlib.gui.components.tree.CustomJTreeNode;

/**
 *
 */
public class InvalidReferenceNode extends CustomJTreeNode {

	public static final int RESID = -1;
	
	private String name;
	
	public InvalidReferenceNode(String name){
		this.name = name;
	}
	
	@Override
	public int getIconResourceID() {
		return RESID;
	}

	@Override
	public String getNodeName() {
		return name;
	}

}
