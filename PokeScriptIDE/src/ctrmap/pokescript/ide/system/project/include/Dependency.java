/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ctrmap.pokescript.ide.system.project.include;

import ctrmap.pokescript.ide.system.IDEResourceReference;
import ctrmap.pokescript.ide.system.project.ProjectAttributes;
import xstandard.formats.yaml.YamlListElement;
import xstandard.formats.yaml.YamlNode;
import java.util.Objects;

/**
 *
 */
public class Dependency {
	public DependencyType type;
	public IDEResourceReference ref;
	
	public Dependency(DependencyType type){
		this.type = type;
	}
	
	public Dependency(YamlNode node){
		type = DependencyType.fromName(node.getChildByName(ProjectAttributes.AVK_PD_REFTYPE).getValue());
		ref = new IDEResourceReference(node);
	}
	
	public YamlNode createNode(){
		YamlNode n = new YamlNode();
		n.content = new YamlListElement(n);
		
		n.addChild(new YamlNode(ProjectAttributes.AVK_PD_REFTYPE, type.toString()));
		ref.addToNode(n);
		return n;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj != null && obj instanceof Dependency){
			Dependency d = (Dependency)obj;
			return Objects.equals(ref, d.ref) && type == d.type;
		}
		return false;
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 83 * hash + Objects.hashCode(this.type);
		hash = 83 * hash + Objects.hashCode(this.ref);
		return hash;
	}

}
