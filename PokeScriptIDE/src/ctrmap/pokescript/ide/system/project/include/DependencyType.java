/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ctrmap.pokescript.ide.system.project.include;

import xstandard.text.FormattingUtils;

/**
 *
 */
public enum DependencyType {
	PROJECT, 
	LIBRARY, 
	DIRECTORY,
	
	INVALID;

	public static DependencyType fromName(String name) {
		for (DependencyType t : DependencyType.values()) {
			if (t.toString().equalsIgnoreCase(name)) {
				return t;
			}
		}
		return null;
	}

	public String getYamlName() {
		return FormattingUtils.getFriendlyEnum(this);
	}
	
}
