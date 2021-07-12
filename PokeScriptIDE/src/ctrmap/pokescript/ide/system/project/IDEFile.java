package ctrmap.pokescript.ide.system.project;

import ctrmap.pokescript.stage0.Preprocessor;
import ctrmap.stdlib.fs.FSFile;
import ctrmap.stdlib.fs.accessors.FSFileAdapter;
import ctrmap.stdlib.util.ArraysEx;
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

	public void addIDEFileListener(IDEFileListener l) {
		ArraysEx.addIfNotNullOrContains(listeners, l);
	}
	
	public void removeIDEFileListener(IDEFileListener l){
		listeners.remove(l);
	}

	public void transferListenersTo(IDEFile f) {
		for (IDEFileListener l : listeners) {
			f.addIDEFileListener(l);
		}
	}

	public void saveNotify() {
		for (IDEFileListener l : listeners) {
			l.onSaved(this);
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
