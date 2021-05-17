package ctrmap.pokescript.ide.system.project;

import ctrmap.pokescript.stage0.Preprocessor;
import ctrmap.stdlib.fs.FSFile;
import ctrmap.stdlib.fs.accessors.FSFileAdapter;

public class IDEFile extends FSFileAdapter {
	private IDEProject project;
	
	private Preprocessor compiler;
	
	public IDEFile(IDEProject proj, FSFile file){
		super(file);
		project = proj;
	}
	
	public FSFile getFsFile(){
		return source;
	}
	
	public IDEProject getProject(){
		return project;
	}
	
	public String getPathInProject(){
		return getPathRelativeTo(project.getSourceDir());
	}
	
	public Preprocessor getCompiler(){
		if (compiler == null){
			compiler = new Preprocessor(source, project.getCompilerArguments());
		}
		
		return compiler;
	}
	
	@Override
	public boolean equals(Object o){
		if (o != null && o instanceof IDEFile){
			IDEFile f = (IDEFile)o;
			return f.project == project && source.equals(f.source);
		}
		return false;
	}
}
