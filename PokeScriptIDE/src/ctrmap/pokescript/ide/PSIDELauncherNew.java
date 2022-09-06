package ctrmap.pokescript.ide;

import ctrmap.pokescript.ide.forms.InitialLaunchDialog;
import ctrmap.pokescript.ide.system.savedata.IDEWorkspace;
import xstandard.fs.accessors.DiskFile;
import xstandard.gui.DialogUtils;
import xstandard.gui.components.ComponentUtils;
import java.util.prefs.Preferences;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class PSIDELauncherNew {

	private static final Preferences PSIDE_MAIN_PREFS = Preferences.userRoot().node("PokeScriptIDE");

	public static final String KEY_LAST_WORKSPACE = "LastWorkspace";

	public static void main(String[] args) {
		ComponentUtils.setSystemNativeLookAndFeel();

		String lastWorkspace = PSIDE_MAIN_PREFS.get(KEY_LAST_WORKSPACE, null);

		IDEWorkspace ws = null;
		boolean openInitLaunchDialog = false;

		if (lastWorkspace != null) {
			DiskFile wsFile = new DiskFile(lastWorkspace);
			ws = IDEWorkspace.openWorkspaceIfApplicable(wsFile);
			if (ws == null) {
				DialogUtils.showInfoMessage(null, "Invalid workspace", "The last used workspace path is invalid. The initial setup will now open.");
				openInitLaunchDialog = true;
			}
		} else {
			DialogUtils.showInfoMessage("Startup",
					"It seems like this is your first time using the PokéScript IDE.\n"
					+ "We have to set up a few things before you get to work.");

			openInitLaunchDialog = true;
		}

		if (openInitLaunchDialog) {
			InitialLaunchDialog dlg = new InitialLaunchDialog(null, true);
			dlg.setVisible(true);

			ws = dlg.getResult();
		}

		if (ws != null) {
			final IDEWorkspace fWS = ws;
			SwingUtilities.invokeLater(() -> {
				PSIDE_MAIN_PREFS.put(KEY_LAST_WORKSPACE, fWS.getRoot().getPath());
				PSIDE ide = new PSIDE();
				ide.setVisible(true);
				ide.loadWorkspace(fWS);
			});
		} else {
			System.exit(0);
		}
	}
}
