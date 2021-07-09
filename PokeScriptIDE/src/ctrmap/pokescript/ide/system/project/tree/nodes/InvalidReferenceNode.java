/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ctrmap.pokescript.ide.system.project.tree.nodes;

import ctrmap.pokescript.ide.PSIDE;
import ctrmap.stdlib.gui.components.tree.CustomJTreeNode;

/**
 *
 */
public class InvalidReferenceNode extends IDENodeBase {

	public static final int RESID = -1;
	
	private String name;
	
	public InvalidReferenceNode(PSIDE ide, String name){
		super(ide);
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

	@Override
	public String getUniqueName() {
		return getNodeName();
	}

}
