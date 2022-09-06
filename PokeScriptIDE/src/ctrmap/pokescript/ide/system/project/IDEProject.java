package ctrmap.pokescript.ide.system.project;

import ctrmap.pokescript.LangCompiler;
import ctrmap.pokescript.LangConstants;
import ctrmap.pokescript.LangPlatform;
import ctrmap.pokescript.ide.FileEditorRSTA;
import ctrmap.pokescript.ide.system.project.caches.ProjectFileCaretPosCache;
import ctrmap.pokescript.ide.system.project.include.Dependency;
import ctrmap.pokescript.ide.system.project.include.IInclude;
import ctrmap.pokescript.ide.system.project.include.InvalidInclude;
import ctrmap.pokescript.ide.system.project.include.LibraryInclude;
import ctrmap.pokescript.ide.system.project.include.ProjectInclude;
import ctrmap.pokescript.ide.system.project.include.SimpleInclude;
import ctrmap.pokescript.ide.system.savedata.IDESaveData;
import ctrmap.scriptformats.pkslib.LibraryFile;
import xstandard.fs.FSFile;
import xstandard.fs.accessors.DiskFile;
import xstandard.gui.file.ExtensionFilter;
import xstandard.util.ArraysEx;
import xstandard.util.ListenableList;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IDEProject {

	public static final ExtensionFilter IDE_PROJECT_EXTENSION_FILTER = new ExtensionFilter("PokéScript IDE Project", "*.pksproj");

	private FSFile projectRootFs;

	private IDEFile projectRoot;
	private IDEProjectManifest manifest;

	private FSFile cacheDir;
	private FSFile libDir;

	public Map<Dependency, IInclude> includes = new HashMap<>();

	public ProjectFileCaretPosCache caretPosCache;

	private final List<IDEProjectListener> listeners = new ArrayList<>();

	public IDEProject(FSFile projectFile) {
		projectRootFs = projectFile.getParent();
		projectRoot = new IDEFile(this, projectRootFs);
		manifest = new IDEProjectManifest(projectFile);

		loadSetup();
	}

	public IDEProject(FSFile projectRoot, String projectName, String productId, LangPlatform plaf) {
		this.projectRoot = new IDEFile(this, projectRoot);

		manifest = new IDEProjectManifest(projectRoot.getChild(projectName + IDE_PROJECT_EXTENSION_FILTER.getPrimaryExtension()), projectName, productId, plaf);

		loadSetup();

		getSourceDir().mkdirs();
	}

	public void addListener(IDEProjectListener l) {
		ArraysEx.addIfNotNullOrContains(listeners, l);
	}

	public void removeListener(IDEProjectListener l) {
		listeners.remove(l);
	}

	public void callSaveListeners(FileEditorRSTA.SaveResult result) {
		for (IDEProjectListener l : listeners) {
			l.onSaved(result);
		}
	}

	private void loadSetup() {
		cacheDir = projectRoot.getChild("cache");
		cacheDir.mkdir();
		libDir = projectRoot.getChild("lib");
		libDir.mkdir();

		if (manifest.isMultirelease()) {
			throw new UnsupportedOperationException("Projects can not be multi-release!");
		}

		caretPosCache = new ProjectFileCaretPosCache(this);
	}

	public void saveCacheData() {
		caretPosCache.write();
	}

	public IDEFile getExistingFile(IDESaveData.IDEFileReference ref) {
		IDEFile f = getExistingFile(ref.path);
		f.readOnly = !ref.openRW;
		return f;
	}

	public IDEFile getClassFile(String classpath) {
		classpath = classpath.replace('.', '/');
		return getFile(classpath + LangConstants.LANG_SOURCE_FILE_EXTENSION);
	}

	public IDEFile getFile(String path) {
		return new IDEFile(this, getSourceDir().getChild(path));
	}

	public IDEFile getExistingFile(String path) {
		FSFile fsf = getSourceDir().getChild(path);
		if (!fsf.exists()) {
			DiskFile df = new DiskFile(path);
			if (df.exists()) {
				fsf = df;
			}
		}
		IDEFile f = new IDEFile(this, fsf);
		return f;
	}

	public String getProjectPath() {
		return manifest.getManifestPath();
	}

	public void setMainClass(IDEFile file) {
		manifest.setMainClass(file.getClasspathInProject());
		for (IDEProjectListener l : listeners) {
			l.onMainClassChanged(file);
		}
	}

	public static boolean isIDEProjectManifest(File f) {
		return f != null && f.exists() && !f.isDirectory();
	}
	
	public void addDependency(IDEContext ctx, Dependency dep) {
		if (getManifest().addDependency(dep)) {
			loadInclude(ctx, dep);
		}
	}

	public void loadInclude(IDEContext ctx, Dependency dep) {
		if (includes.containsKey(dep)) {
			return;
		}
		FSFile file = null;
		IInclude inc = null;
		try {
			file = resolvePath(dep, ctx);

			if (file == null) {
				inc = new InvalidInclude(dep.ref.path);
			} else {
				switch (dep.type) {
					case DIRECTORY:
						inc = new SimpleInclude(file);
						break;
					case LIBRARY:
						inc = new LibraryInclude(new LibraryFile(file));
						break;
					case PROJECT:
						inc = new ProjectInclude(ctx.getLoadedProject(file));
						break;
				}
			}
		} catch (Exception ex) {
			inc = new InvalidInclude(dep.ref.path);
		}
		includes.put(dep, inc);
	}

	public final void loadIncludes(IDEContext ctx) {
		List<Dependency> l = manifest.getProjectDependencies();
		includes.clear();
		for (Dependency d : l) {
			loadInclude(ctx, d);
		}
	}

	public FSFile getFSRoot() {
		return projectRootFs;
	}

	public IDEFile getIDERoot() {
		return projectRoot;
	}

	public FSFile getCacheDir() {
		return cacheDir;
	}

	public List<FSFile> getAllIncludeFiles() {
		List<FSFile> l = new ArrayList<>();
		LangPlatform plaf = manifest.getSinglereleaseTargetPlatform();
		for (IInclude inc : includes.values()) {
			l.addAll(inc.getIncludeSources(plaf));
		}
		l.add(getSourceDirForPlatform(plaf));
		return l;
	}

	private FSFile resolvePath(Dependency dep, IDEContext ctx) {
		return dep.ref.resolve(projectRoot, ctx, libDir);
	}

	public IDEProjectManifest getManifest() {
		return manifest;
	}

	public IDEFile getMainClass() {
		String mcPath = manifest.getMainClass();
		if (mcPath != null) {
			IDEFile cls = getClassFile(mcPath);
			if (cls.isFile()) {
				return cls;
			} else {
				System.err.println("Could not get main class IDEFile: 'cls' is not a file! (exists: " + cls.exists() + ", isDirectory: " + cls.isDirectory() + ")");
			}
		}
		return null;
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

	public List<IDEFile> getSourceDirs() {
		return ArraysEx.asList(projectRoot.getChild(IDEProjectManifest.DEFAULT_SOURCE_DIR));
	}

	public LangCompiler.CompilerArguments getCompilerArguments() {
		LangCompiler.CompilerArguments args = new LangCompiler.CompilerArguments();
		args.includeRoots = getAllIncludeFiles();
		args.preprocessorDefinitions = manifest.getCompilerDefinitions();
		args.setPlatform(getManifest().getSinglereleaseTargetPlatform());
		return args;
	}
}
