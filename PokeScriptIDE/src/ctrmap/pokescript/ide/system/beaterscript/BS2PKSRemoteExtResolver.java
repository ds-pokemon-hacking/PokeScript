package ctrmap.pokescript.ide.system.beaterscript;

import ctrmap.pokescript.LangPlatform;
import ctrmap.pokescript.ide.system.project.IDEContext;
import ctrmap.pokescript.ide.system.project.remoteext.IRemoteExtResolver;
import ctrmap.scriptformats.pkslib.LibraryManifest;
import xstandard.formats.yaml.Yaml;
import xstandard.formats.zip.ZipArchive;
import xstandard.fs.FSFile;
import xstandard.fs.accessors.MemoryFile;
import xstandard.gui.DialogUtils;
import xstandard.gui.LoadingDialog;
import xstandard.net.FileDownloader;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.List;
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

			LoadingDialog dlg = new LoadingDialog((Frame)null, true);
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
						
						List<Yaml> ymls = new ArrayList<>();
						for (FSFile child : libraryRoot.getChild(ymlPath).listFiles()) {
							if (child.getName().endsWith(Yaml.EXTENSION_FILTER.getPrimaryExtension())) {
								ymls.add(new Yaml(child));
							}
						}
						
						FSFile dest = libraryRoot.getChild(mf.getMultireleaseTargetForPlatform(LangPlatform.EV_SWAN).path);
						dest.delete();
						dest.mkdirs();

						BS2PKS.makePKSIncludes(
							dest,
							ymls.toArray(new Yaml[ymls.size()])
						);
						
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
