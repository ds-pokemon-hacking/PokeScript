/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ctrmap.pokescript.ide.system.project.tree;

import ctrmap.pokescript.ide.system.project.IDEProject;
import ctrmap.pokescript.ide.system.project.tree.nodes.ClassNode;
import ctrmap.pokescript.ide.system.project.tree.nodes.ContainerNode;
import ctrmap.pokescript.ide.system.project.tree.nodes.InvalidReferenceNode;
import ctrmap.pokescript.ide.system.project.tree.nodes.LibraryNode;
import ctrmap.pokescript.ide.system.project.tree.nodes.PackageNode;
import ctrmap.pokescript.ide.system.project.tree.nodes.ProjectNode;
import ctrmap.pokescript.ide.system.project.tree.nodes.ProjectReferenceNode;
import ctrmap.pokescript.ide.system.project.tree.nodes.SourceDirNode;
import ctrmap.stdlib.gui.components.tree.CustomJTree;
import ctrmap.stdlib.res.ResourceAccess;

/**
 *
 */
public class IDEProjectTree extends CustomJTree {
	public IDEProjectTree(){
		super();
		
		registerIconResource(SourceDirNode.RESID, "sourcefolder");
		registerIconResource(LibraryNode.RESID, "library");
		registerIconResource(PackageNode.RESID, "package");
		registerIconResource(ClassNode.RESID, "sourcefile");
		registerIconResource(ContainerNode.Type.LIBRARIES.getResId(), "librarydir");
		registerIconResource(ProjectNode.RESID, "project");
		registerIconResource(ProjectReferenceNode.RESID, "project");
		registerIconResource(InvalidReferenceNode.RESID, "invalid");
	}
	
	public void addProject(IDEProject project){
		getRootNode().add(new ProjectNode(project));
	}
	
	private void registerIconResource(int resID, String name){
		iconProvider.registerResourceIcon(resID, ResourceAccess.getByteArray("scripting/ui/tree/" + name + ".png"));
	}
}
