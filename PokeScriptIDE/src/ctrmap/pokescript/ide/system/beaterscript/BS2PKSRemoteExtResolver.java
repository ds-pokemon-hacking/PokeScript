package ctrmap.pokescript.ide.system.beaterscript;

import ctrmap.pokescript.LangPlatform;
import ctrmap.pokescript.ide.system.project.IDEContext;
import ctrmap.pokescript.ide.system.project.remoteext.IRemoteExtResolver;
import ctrmap.scriptformats.pkslib.LibraryManifest;
import ctrmap.stdlib.formats.yaml.Yaml;
import ctrmap.stdlib.formats.zip.ZipArchive;
import ctrmap.stdlib.fs.FSFile;
import ctrmap.stdlib.fs.accessors.MemoryFile;
import ctrmap.stdlib.gui.DialogUtils;
import ctrmap.stdlib.gui.LoadingDialog;
import ctrmap.stdlib.net.FileDownloader;
import javax.swing.SwingWorker;

public class BS2PKSRemoteExtResolver implements IRemoteExtResolver {

	public static final String REPOSITORY_NAME = "PokeScriptSDK5";
	public static final String BRANCH_NAME = "master";

	@Override
	public String getName() {
		return "BS2PKS";
	}

	@Override
	public FSFile resolvePath(String path, IDEContext context, FSFile workDir) {
		try {
			String[] params = path.split("\\|");

			String zipUrl = params[0];
			String ymlPath = params[1];

			String friendlyName = REPOSITORY_NAME + "-" + BRANCH_NAME;
			FSFile libraryRoot = workDir.getChild(friendlyName);
			if (libraryRoot.exists()) {
				return libraryRoot;
			}

			LoadingDialog dlg = new LoadingDialog(null, true);
			dlg.setAlwaysOnTop(true);
			dlg.setProgressTitle("Resolving " + friendlyName + "...");

			SwingWorker worker = new SwingWorker() {
				@Override
				protected Object doInBackground() throws Exception {
					dlg.setProgressSubTitle("Downloading...");

					MemoryFile zip = FileDownloader.downloadToMemory(zipUrl);

					dlg.setProgressPercentage(33);

					if (zip != null) {
						dlg.setProgressSubTitle("Extracting...");
						//Initialize a dummy library in the workdir
						ZipArchive.extractZipToFile(workDir, zip);
						dlg.setProgressPercentage(66);

						dlg.setProgressSubTitle("Generating...");

						LibraryManifest mf = new LibraryManifest(libraryRoot.getChild(LibraryManifest.LIBRARY_MANIFEST_NAME));

						BS2PKS.makePKSIncludes(new Yaml(libraryRoot.getChild(ymlPath)), libraryRoot.getChild(mf.getMultireleaseTargetForPlatform(LangPlatform.EV_SWAN).path));
						dlg.setProgressPercentage(100);
						dlg.setVisible(false);

						return libraryRoot;
					} else {
						return null;
					}
				}
			};

			worker.execute();
			dlg.setVisible(true);
			
			return (FSFile)worker.get();
		} catch (Exception ex) {
			DialogUtils.showExceptionTraceDialog(ex);
		}
		return null;
	}

}
