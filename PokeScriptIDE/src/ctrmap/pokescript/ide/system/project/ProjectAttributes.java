package ctrmap.pokescript.ide.system.project;

import ctrmap.stdlib.gui.FormattingUtils;

public class ProjectAttributes {

	/**
	 * Contains an optional list of boolean keys for the PokeScript preprocessor.
	 */
	public static final String AK_COMPILE_DEFS = "Definitions";
	
	/**
	 * Supports specifying global compiler pragmata in a project.
	 */
	public static final String AK_COMPILE_PRAGMA = "Pragmata";
	
	/**
	 * A list of references to LIBs, projects or source directories to satisfy the Dependencies of the project.
	 */
	public static final String AK_PROJECT_DEPS = "DepPaths";
	
	/**
	 * Specifies how a dependency should be included. See DependencyType.
	 */
	public static final String AVK_PD_REFTYPE = "Type";
	
	/**
	 * Specifies how a dependency should be resolved. See ResourcePathType.
	 */
	public static final String AVK_PD_PATHTYPE = "PathType";
	
	/**
	 * If PathType is REMOTE_EXT, specifies the RemoteExtResolver.
	 */
	public static final String AVK_PD_REMOTEEXTTYPE = "RemoteExtType";
	
	/**
	 * Points to a relative or absolute filesystem location of a project dependency.
	 */
	public static final String AVK_PD_PATH = "Path";
	
}
