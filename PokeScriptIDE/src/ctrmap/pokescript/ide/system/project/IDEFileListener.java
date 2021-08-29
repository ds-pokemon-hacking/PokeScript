package ctrmap.pokescript.ide.system.project;

import ctrmap.pokescript.ide.FileEditorRSTA;

public interface IDEFileListener {
	public default void onSaved(IDEFile f, FileEditorRSTA.SaveResult result) {
		
	}
	public default void onClosed(IDEFile f) {
		
	}
}
