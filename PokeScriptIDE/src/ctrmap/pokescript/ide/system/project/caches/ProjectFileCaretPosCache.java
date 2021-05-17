package ctrmap.pokescript.ide.system.project.caches;

import ctrmap.pokescript.ide.FileEditorRSTA;
import ctrmap.pokescript.ide.system.project.IDEProject;
import ctrmap.stdlib.formats.yaml.Yaml;
import ctrmap.stdlib.formats.yaml.YamlNode;
import java.util.HashMap;
import java.util.Map;

public class ProjectFileCaretPosCache extends Yaml {

	private Map<String, Integer> caretPositions = new HashMap<>();

	public ProjectFileCaretPosCache(IDEProject prj) {
		super(prj.getCacheDir().getChild("CaretPositions.cache"));

		for (YamlNode n : root.children) {
			caretPositions.put(n.getKey(), n.getValueInt());
		}
	}

	public void setStoredCaretPositionToEditor(FileEditorRSTA editor) {
		int pos = caretPositions.getOrDefault(editor.getEditedFile().getPathInProject(), 0);
		
		if (pos < 0) {
			pos = 0;
		} else if (pos > editor.getDocument().getLength()) {
			pos = editor.getDocument().getLength();
		}
		
		editor.setCaretPosition(pos);
	}
	
	public void storeCaretPositionOfEditor(FileEditorRSTA editor) {
		String key = editor.getEditedFile().getPathInProject();
		caretPositions.put(key, editor.getCaretPosition());
	}
	
	@Override
	public void write(){
		root.removeAllChildren();
		
		for (Map.Entry<String, Integer> e : caretPositions.entrySet()){
			root.addChild(e.getKey(), e.getValue());
		}
		
		super.write();
	}
}
