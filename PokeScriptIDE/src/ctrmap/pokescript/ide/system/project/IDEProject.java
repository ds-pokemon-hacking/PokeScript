package ctrmap.pokescript.ide.system.project;

import ctrmap.pokescript.LangCompiler;
import ctrmap.pokescript.LangPlatform;
import ctrmap.pokescript.ide.system.project.caches.ProjectFileCaretPosCache;
import ctrmap.pokescript.ide.system.project.include.Dependency;
import ctrmap.pokescript.ide.system.project.include.IInclude;
import ctrmap.pokescript.ide.system.project.include.InvalidInclude;
import ctrmap.pokescript.ide.system.project.include.LibraryInclude;
import ctrmap.pokescript.ide.system.project.include.ProjectInclude;
import ctrmap.pokescript.ide.system.project.include.SimpleInclude;
import ctrmap.scriptformats.pkslib.LibraryFile;
import ctrmap.stdlib.fs.FSFile;
import ctrmap.stdlib.gui.file.ExtensionFilter;
import ctrmap.stdlib.util.ArraysEx;
import ctrmap.stdlib.util.ListenableList;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class IDEProject {

	public static final ExtensionFilter IDE_PROJECT_EXTENSION_FILTER = new ExtensionFilter("PokéScript IDE Project", "*.pksproj");

	private FSFile projectRoot;
	private IDEProjectManifest manifest;

	private FSFile cacheDir;

	public ListenableList<IInclude> includes = new ListenableList<>();
	
	public ProjectFileCaretPosCache caretPosCache;

	public IDEProject(FSFile projectFile) {
		projectRoot = projectFile.getParent();
		manifest = new IDEProjectManifest(projectFile);

		loadSetup();
	}

	public IDEProject(FSFile projectRoot, String projectName, String productId, LangPlatform plaf) {
		this.projectRoot = projectRoot;

		manifest = new IDEProjectManifest(projectRoot.getChild(projectName + IDE_PROJECT_EXTENSION_FILTER.getPrimaryExtension()), projectName, productId, plaf);

		loadSetup();

		getSourceDir().mkdirs();
	}

	private void loadSetup() {
		cacheDir = projectRoot.getChild("cache");
		cacheDir.mkdir();

		if (manifest.isMultirelease()) {
			throw new UnsupportedOperationException("Projects can not be multi-release!");
		}
		
		caretPosCache = new ProjectFileCaretPosCache(this);
	}
	
	public void saveCacheData(){
		caretPosCache.write();
	}

	public IDEFile getFile(String path) {
		FSFile fsf = getSourceDir().getChild(path);
		IDEFile f = new IDEFile(this, fsf);
		return f;
	}

	public String getProjectPath() {
		return manifest.getManifestPath();
	}

	public static boolean isIDEProject(File f) {
		return f != null && f.exists() && !f.isDirectory();
	}

	public final void loadIncludes(IDEContext ctx) {
		List<Dependency> l = manifest.getProjectDependencies();
		includes.clear();
		for (Dependency d : l) {
			FSFile file = resolvePath(d, ctx);

			if (file == null) {
				includes.add(new InvalidInclude(d.ref.path));
			} else {
				switch (d.type) {
					case DIRECTORY:
						includes.add(new SimpleInclude(file));
						break;
					case LIBRARY:
						includes.add(new LibraryInclude(new LibraryFile(file)));
						break;
					case PROJECT:
						includes.add(new ProjectInclude(ctx.getLoadedProject(file)));
						break;
				}
			}
		}
	}

	public FSFile getRoot() {
		return projectRoot;
	}

	public FSFile getCacheDir(){
		return cacheDir;
	}
	
	public List<FSFile> getAllIncludeFiles() {
		List<FSFile> l = new ArrayList<>();
		LangPlatform plaf = manifest.getSinglereleaseTargetPlatform();
		for (IInclude inc : includes) {
			l.addAll(inc.getIncludeSources(plaf));
		}
		return l;
	}

	private FSFile resolvePath(Dependency dep, IDEContext ctx) {
		return dep.ref.resolve(projectRoot, ctx);
	}

	public IDEProjectManifest getManifest() {
		return manifest;
	}

	public FSFile getSourceDirForPlatform(LangPlatform plaf) {
		if (plaf == manifest.getSinglereleaseTargetPlatform()) {
			return projectRoot.getChild(IDEProjectManifest.DEFAULT_SOURCE_DIR);
		}
		return null;
	}

	public FSFile getSourceDir() {
		return getSourceDirForPlatform(manifest.getSinglereleaseTargetPlatform());
	}

	public List<FSFile> getSourceDirs() {
		return ArraysEx.asList(projectRoot.getChild(IDEProjectManifest.DEFAULT_SOURCE_DIR));
	}

	public LangCompiler.CompilerArguments getCompilerArguments() {
		LangCompiler.CompilerArguments args = new LangCompiler.CompilerArguments();
		args.includeRoots = getAllIncludeFiles();
		args.setPlatform(getManifest().getSinglereleaseTargetPlatform());
		return args;
	}
}
