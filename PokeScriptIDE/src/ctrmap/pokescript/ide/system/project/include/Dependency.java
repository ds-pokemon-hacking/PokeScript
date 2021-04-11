/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ctrmap.pokescript.ide.system.project.include;

import ctrmap.pokescript.ide.system.IDEResourceReference;
import ctrmap.pokescript.ide.system.project.ProjectAttributes;
import ctrmap.stdlib.formats.yaml.YamlListElement;
import ctrmap.stdlib.formats.yaml.YamlNode;

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
}
