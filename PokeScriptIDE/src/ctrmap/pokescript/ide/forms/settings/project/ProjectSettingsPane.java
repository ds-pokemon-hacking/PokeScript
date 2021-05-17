/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ctrmap.pokescript.ide.forms.settings.project;

import ctrmap.pokescript.ide.PSIDE;
import ctrmap.pokescript.ide.system.project.IDEProject;
import javax.swing.JPanel;

public abstract class ProjectSettingsPane extends JPanel {
	protected PSIDE ide;
	protected IDEProject project;
	
	public ProjectSettingsPane(PSIDE ide, IDEProject project){
		super();
		this.ide = ide;
		this.project = project;
	}
	
	public abstract String getMenuName();
	public abstract void save();
}
