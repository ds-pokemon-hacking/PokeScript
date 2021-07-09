package ctrmap.pokescript.ide.system.project.tree.nodes;

import ctrmap.pokescript.ide.PSIDE;
import ctrmap.pokescript.ide.system.project.IDEFile;
import ctrmap.scriptformats.pkslib.LibraryFile;
import ctrmap.scriptformats.pkslib.PlatformSourceTarget;

public class LibraryReferenceNode extends PackageNode {

	public static int RESID = 1;

	private LibraryFile lib;

	public LibraryReferenceNode(PSIDE ide, LibraryFile library) {
		super(ide);
		IDEFile ideFileDummy = new IDEFile(null, library);
		lib = library;
		dir = ideFileDummy;

		for (PlatformSourceTarget tgt : library.getManifest().getMultireleaseTargets()) {
			IDEFile file = ideFileDummy.getChild(tgt.path);
			if (file == null){
				throw new NullPointerException("Source directory " + tgt.path + " not present in library " + library + "(" + library.getSource().getClass() + ")");
			}
			add(new SourceDirNode(ide, file));
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
