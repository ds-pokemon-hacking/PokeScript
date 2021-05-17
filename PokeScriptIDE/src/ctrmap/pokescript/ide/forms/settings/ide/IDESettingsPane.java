/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ctrmap.pokescript.ide.forms.settings.ide;

import ctrmap.pokescript.ide.PSIDE;
import javax.swing.JPanel;

public abstract class IDESettingsPane extends JPanel {
	protected PSIDE ide;
	
	public IDESettingsPane(PSIDE ide){
		super();
		this.ide = ide;
	}
	
	public abstract String getMenuName();
}
