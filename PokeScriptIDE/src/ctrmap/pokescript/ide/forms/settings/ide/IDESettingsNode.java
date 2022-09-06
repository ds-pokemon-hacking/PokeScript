/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ctrmap.pokescript.ide.forms.settings.ide;

import xstandard.gui.components.tree.CustomJTreeNode;

/**
 *
 */
public class IDESettingsNode extends CustomJTreeNode {
	public static final int RESID = 0;
	
	public final IDESettingsPane pane;
	private final String overrideName;
	
	public IDESettingsNode(IDESettingsPane pane){
		this(null, pane);
	}
	
	public IDESettingsNode(String name, IDESettingsPane pane){
		this.pane = pane;
		overrideName = name == null ? pane.getMenuName() : name;
	}
	
	@Override
	public int getIconResourceID() {
		return RESID;
	}

	@Override
	public String getNodeName() {
		return overrideName;
	}

}
