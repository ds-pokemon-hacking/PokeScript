package ctrmap.pokescript.ide.system.project.tree.nodes;

import ctrmap.pokescript.LangConstants;
import ctrmap.scriptformats.pkslib.LibraryFile;
import ctrmap.scriptformats.pkslib.PlatformSourceTarget;
import ctrmap.stdlib.fs.FSFile;

public class LibraryNode extends PackageNode {

	public static int RESID = 1;

	private LibraryFile lib;

	public LibraryNode(LibraryFile library) {
		lib = library;

		if (library.getManifest().isMultirelease()) {
			for (PlatformSourceTarget tgt : library.getManifest().getMultireleaseTargets()) {
				FSFile file = lib.getChild(tgt.path);
				add(new SourceDirNode(file));
			}
		} else {
			addChildrenFromDir(library);
		}
	}

	@Override
	public int getIconResourceID() {
		return RESID;
	}

	@Override
	public String getNodeName() {
		return lib.getManifest().getProductName();
	}
}
