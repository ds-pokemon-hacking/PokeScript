package ctrmap.pokescript.ide.system.project;

import ctrmap.pokescript.LangConstants;
import ctrmap.pokescript.ide.FileEditorRSTA;
import ctrmap.pokescript.stage0.Preprocessor;
import xstandard.fs.FSFile;
import xstandard.fs.FSUtil;
import xstandard.fs.accessors.FSFileAdapter;
import xstandard.io.base.iface.ReadableStream;
import xstandard.util.ArraysEx;
import java.util.ArrayList;
import java.util.List;

public class IDEFile extends FSFileAdapter {

	private IDEProject project;

	private Preprocessor compiler;

	boolean readOnly = false;

	private List<IDEFileListener> listeners = new ArrayList<>();

	public IDEFile(IDEProject proj, FSFile file) {
		this(proj, file, false);
	}

	public IDEFile(IDEProject proj, FSFile file, boolean forceNonWriteable) {
		super(file);
		readOnly = forceNonWriteable;
		project = proj;
	}

	public void setReadOnly(boolean bln) {
		readOnly = bln;
	}

	public void addIDEFileListener(IDEFileListener l) {
		ArraysEx.addIfNotNullOrContains(listeners, l);
	}

	public void removeIDEFileListener(IDEFileListener l) {
		listeners.remove(l);
	}

	public void transferListenersTo(IDEFile f) {
		for (IDEFileListener l : listeners) {
			f.addIDEFileListener(l);
		}
	}

	private void fillCompiler() {
		compiler.setArgs(project.getCompilerArguments());
		compiler.read(source);
	}

	public void fillCompiler(ReadableStream stream) {
		if (compiler != null) {
			compiler.setArgs(project.getCompilerArguments());
			compiler.read(stream);
		}
	}

	public void saveNotify(FileEditorRSTA.SaveResult result) {
		if (source.canRead()) {
			if (compiler != null) {
				fillCompiler();
				for (IDEFileListener l : listeners) {
					l.onSaved(this, result);
				}
			}
		}
	}

	public void closeNotify() {
		if (source.canRead()) {
			fillCompiler();
			for (IDEFileListener l : listeners) {
				l.onClosed(this);
			}
		}
	}

	@Override
	public IDEFile getChild(String forName) {
		FSFile child = super.getChild(forName);
		if (child == null) {
			return null;
		}
		return new IDEFile(project, child);
	}

	@Override
	public List<IDEFile> listFiles() {
		List<IDEFile> ideFiles = new ArrayList<>();
		for (FSFile fsf : super.listFiles()) {
			ideFiles.add(new IDEFile(project, fsf));
		}
		return ideFiles;
	}

	@Override
	public int getPermissions() {
		int perms = super.getPermissions();
		if (readOnly) {
			perms &= ~FSF_ATT_WRITE;
		}
		return perms;
	}

	public FSFile getFsFile() {
		return source;
	}

	public IDEProject getProject() {
		return project;
	}

	public String getPathInProject() {
		if (project == null) {
			return getPath();
		}
		return getPathRelativeTo(project.getSourceDir());
	}

	public String getClasspathInProject() {
		return FSUtil.getFilePathWithoutExtension(getPathInProject().replace('/', LangConstants.CH_PATH_SEPARATOR));
	}

	public Preprocessor getCompiler() {
		if (compiler == null) {
			compiler = new Preprocessor(source, project.getCompilerArguments());
		}

		return compiler;
	}

	@Override
	public boolean equals(Object o) {
		if (o != null && o instanceof IDEFile) {
			IDEFile f = (IDEFile) o;
			return f.project == project && source.equals(f.source);
		}
		return false;
	}
}
