package ctrmap.scriptformats.pkslib;

import ctrmap.pokescript.LangPlatform;
import xstandard.fs.FSFile;
import xstandard.formats.zip.ZipArchive;
import xstandard.fs.accessors.FSFileAdapter;

public class LibraryFile extends FSFileAdapter {

	private LibraryManifest manifest;

	public LibraryFile(FSFile source) {
		super(source);

		if (ZipArchive.isZip(source)) {
			source = new ZipArchive(source);
		}
		this.source = source;

		FSFile mf = source.getChild(LibraryManifest.LIBRARY_MANIFEST_NAME);
		if (mf.isFile()) {
			manifest = new LibraryManifest(mf);
		} else {
			throw new UnsupportedOperationException("Library does not have a manifest!");
		}
	}

	public boolean isValid() {
		return manifest != null;
	}

	public LibraryManifest getManifest() {
		return manifest;
	}

	public FSFile getSourceDirForPlatform(LangPlatform plaf) {
		if (isValid()) {
			PlatformSourceTarget tgt = manifest.getMultireleaseTargetForPlatform(plaf);
			if (tgt != null) {
				return source.getChild(tgt.path);
			}
		}
		return null;
	}

	public boolean containsClassPath(String classPath) {
		if (classPath == null) {
			return false;
		}
		FSFile child = getChildByClassPath(classPath);
		return child != null;
	}

	public FSFile getChildByClassPath(String classPath) {
		if (!isValid()) {
			return null;
		}
		return source.getChild(classPath.replace('.', '/'));
	}
}
