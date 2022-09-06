package ctrmap.pokescript.ide.system.project;

import ctrmap.pokescript.ide.FileEditorRSTA;

public interface IDEProjectListener {
	public default void onMainClassChanged(IDEFile newMainClass) {
		
	}
	
	public default void onSaved(FileEditorRSTA.SaveResult result) {
		
	}
}
