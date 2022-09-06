/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ctrmap.pokescript.ide.forms.settings.project;

import xstandard.gui.components.tree.CustomJTreeNode;

/**
 *
 */
public class ProjectSettingsNode extends CustomJTreeNode {
	public static final int RESID = 0;
	
	public final ProjectSettingsPane pane;
	private final String overrideName;
	
	public ProjectSettingsNode(ProjectSettingsPane pane){
		this(null, pane);
	}
	
	public ProjectSettingsNode(String name, ProjectSettingsPane pane){
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
