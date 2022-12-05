package ctrmap.pokescript.ide;

import ctrmap.pokescript.CompilerExceptionData;
import ctrmap.pokescript.LangConstants;
import ctrmap.pokescript.stage0.Preprocessor;
import ctrmap.pokescript.ide.autocomplete.AutoComplete;
import ctrmap.pokescript.ide.forms.InitialLaunchDialog;
import xstandard.fs.accessors.DiskFile;
import xstandard.gui.file.XFileDialog;
import ctrmap.pokescript.ide.forms.ProjectCreationDialog;
import ctrmap.pokescript.ide.forms.settings.ide.IDESettings;
import ctrmap.pokescript.ide.system.IDEResourceReference;
import ctrmap.pokescript.ide.system.IDESetupFile;
import ctrmap.pokescript.ide.system.ResourcePathType;
import ctrmap.pokescript.ide.system.beaterscript.BS2PKSRemoteExtResolver;
import ctrmap.pokescript.ide.system.project.IDEContext;
import ctrmap.pokescript.ide.system.project.IDEFile;
import ctrmap.pokescript.ide.system.project.IDEProject;
import ctrmap.pokescript.ide.system.project.tree.IDEProjectTree;
import ctrmap.pokescript.ide.system.project.tree.nodes.ProjectNode;
import ctrmap.pokescript.ide.system.savedata.IDESaveData;
import ctrmap.pokescript.ide.system.savedata.IDEWorkspace;
import xstandard.fs.FSFile;
import xstandard.gui.DialogUtils;
import xstandard.gui.components.ComponentUtils;
import xstandard.gui.components.tabbedpane.JTabbedPaneEx;
import xstandard.gui.components.tabbedpane.TabbedPaneTab;
import xstandard.gui.components.tree.CustomJTreeNode;
import xstandard.res.ResourceAccess;
import xstandard.util.ArraysEx;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.TreePath;
import org.fife.ui.rtextarea.RTextScrollPane;

public class PSIDE extends javax.swing.JFrame {

	public static final IDEResourceReference DEFAULT_SETUP_RESOURCE
		= new IDEResourceReference(ResourcePathType.INTERNAL, "scripting/IDE.yml");

	public IDESetupFile setup;

	public IDEContext context;

	private AutoComplete ac;

	private IDESettings settingsFrame;

	private List<FileEditorRSTA> fileEditors = new ArrayList<>();
	private List<PSIDEListener> listeners = new ArrayList<>();

	private boolean standalone = false;

	public static final String PSIDE_ERR_COLUMN_ID_FILE = "File";
	public static final String PSIDE_ERR_COLUMN_ID_LINE = "Line";
	public static final String PSIDE_ERR_COLUMN_ID_CAUSE = "Cause";

	public PSIDE() {
		this(DEFAULT_SETUP_RESOURCE);
		standalone = true;
	}

	public PSIDE(IDEResourceReference setup) {
		IDEResources.load();
		standalone = false;

		initComponents();
		projectTree.attachIDE(this);

		fileTabs.setCloseIconsFromDirectory(ResourceAccess.getResourceFile("scripting/ui/tabs/btn_close"));

		ComponentUtils.maximize(this);

		initContext();

		loadSetup(DEFAULT_SETUP_RESOURCE);

		errorTable.getColumn(PSIDE_ERR_COLUMN_ID_FILE).setWidth(200);
		errorTable.getColumn(PSIDE_ERR_COLUMN_ID_FILE).setMaxWidth(400);
		errorTable.getColumn(PSIDE_ERR_COLUMN_ID_LINE).setMaxWidth(35);

		ac = new AutoComplete();

		settingsFrame = new IDESettings(this);

		projectTree.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e)) {
					int selRow = projectTree.getRowForLocation(e.getX(), e.getY());
					TreePath selPath = projectTree.getPathForLocation(e.getX(), e.getY());
					projectTree.setSelectionPath(selPath);
					if (selRow > -1) {
						projectTree.setSelectionRow(selRow);
					}

					Object obj = projectTree.getLastSelectedPathComponent();
					if (obj instanceof CustomJTreeNode) {
						((CustomJTreeNode) obj).onNodePopupInvoke(e);
					}
				} else if (e.getClickCount() == 2) {
					Object obj = projectTree.getLastSelectedPathComponent();
					if (obj instanceof CustomJTreeNode) {
						((CustomJTreeNode) obj).onNodeSelected();
					}
				}
			}
		});

		fileTabs.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				changeToSelectedEditor();
			}
		});

		fileTabs.addTabRemovedListener(new JTabbedPaneEx.TabRemovedListener() {
			@Override
			public boolean onTabIsRemoving(TabbedPaneTab tab) {
				boolean willRemove = false;
				FileEditorRSTA rsta = (FileEditorRSTA) ((RTextScrollPane) tab.getComponent()).getTextArea();
				if (!rsta.getEditedFile().exists() || !rsta.getEditedFile().canWrite()) {
					willRemove = true;
				} else {
					rsta.getEditedFile().getProject().caretPosCache.storeCaretPositionOfEditor(rsta);
					willRemove = saveFile(rsta, true);
				}
				if (willRemove) {
					//Moving this here instead of onTabRemoved because ChangeListeners fire first
					FileEditorRSTA editor = fileEditors.remove(tab.getIndex());
					if (context.getWorkspace() != null) {
						IDEFile f = editor.getEditedFile();
						if (f != null) {
							context.getWorkspace().saveData.removeOpenFile(f);
							f.closeNotify();
						}
					}
				}
				return willRemove;
			}

			@Override
			public void onTabRemoved(TabbedPaneTab tab) {
				
			}
		});
	}

	public void addIDEListener(PSIDEListener listener) {
		ArraysEx.addIfNotNullOrContains(listeners, listener);
	}

	public void removeIDEListener(PSIDEListener listener) {
		listeners.remove(listener);
	}

	public void closeFileTab(FileEditorRSTA editor) {
		int index = fileEditors.indexOf(editor);
		if (index >= 0 && index < fileTabs.getTabCount()) {
			fileTabs.remove(index);
		}
	}

	public void changeToSelectedEditor() {
		if (context.isAvailable()) {
			FileEditorRSTA edt = getSelectedFileEditor();
			if (edt != null) {
				System.out.println("Change to " + edt.getEditedFile());
				ac.attachTextArea(edt);
				edt.forceReparsing();
				context.getWorkspace().saveData.setLastOpenedFile(edt.getEditedFile());
				edt.publishErrorTable();
			}
		} else {
			System.out.println("Requested editor change, but context not available!");
		}
	}

	public IDEProjectTree getProjectTree() {
		return projectTree;
	}

	public void initContext() {
		context = new IDEContext();
		context.registerRemoteExtResolver(new BS2PKSRemoteExtResolver());
	}

	public void loadSetup(IDEResourceReference setupPath) {
		FSFile setupFile = setupPath.resolve(new DiskFile(System.getProperty("user.dir")), context, null);
		if (setupFile == null) {
			DialogUtils.showErrorMessage(this, "Internal error", "The setup file " + setupPath.path + " could not be found! Can not continue.");
			System.exit(0);
		}
		setup = new IDESetupFile(setupFile);
	}

	public FileEditorRSTA getSelectedFileEditor() {
		int idx = fileTabs.getSelectedIndex();
		if (idx != -1) {
			FileEditorRSTA editor = fileEditors.get(fileTabs.getSelectedIndex());
			return editor;
		}
		return null;
	}

	public IDEProject findLoadedProjectByProdId(String prodId) {
		return context.getProjectByProdId(prodId);
	}

	public void loadWorkspace(IDEWorkspace ws) {
		context.loadWorkspace(ws);
		context.lock();

		projectTree.removeAllChildren();

		for (int i = 0; i < ws.saveData.openedProjectPaths.size(); i++) {
			String projectPath = ws.saveData.openedProjectPaths.get(i);
			File projectFile = new File(projectPath);

			if (IDEProject.isIDEProjectManifest(projectFile)) {
				openProject(new IDEProject(new DiskFile(projectFile)), false);
			} else {
				ws.saveData.openedProjectPaths.remove(i);
				i--;
			}
		}

		for (int i = 0; i < ws.saveData.openedFilePaths.size(); i++) {
			IDESaveData.IDEFileReference ref = ws.saveData.openedFilePaths.get(i);

			IDEFile file = resolveIDEFileReference(ref);

			if (file == null) {
				ws.saveData.openedFilePaths.remove(i);
				i--;
			} else {
				openFile(file);
			}
		}

		if (ws.saveData.lastOpenedFile != null) {
			IDEFile lastOpenedFile = resolveIDEFileReference(ws.saveData.lastOpenedFile);
			if (lastOpenedFile != null) {
				openFile(lastOpenedFile);
			}
		}

		context.releaseLock();
		ws.saveData.write();

		makeTree();
		ws.saveData.loadTreeExpansionState(projectTree);

		changeToSelectedEditor();
	}

	private IDEFile resolveIDEFileReference(IDESaveData.IDEFileReference ref) {
		IDEProject prj = findLoadedProjectByProdId(ref.projectProdId);

		if (prj != null) {
			IDEFile file = prj.getExistingFile(ref);
			if (file != null && file.isFile()) {
				return file;
			}
		}
		return null;
	}

	public boolean saveWorkspace(boolean dlg) {
		if (!saveAllFiles(dlg)) {
			return false;
		}

		syncCaretPositionsWithCache();

		context.saveCacheData();
		if (context.getWorkspace() != null) {
			context.getWorkspace().saveData.saveTreeExpansionState(projectTree);
		}
		return true;
	}

	public boolean closeWorkspace(boolean dlg) {
		if (!saveWorkspace(dlg)) {
			return false;
		}

		context.loadWorkspace(null);

		fileTabs.removeAll();
		projectTree.removeAllChildren();

		return true;
	}

	public boolean isIDEFileOpened(IDEFile f) {
		return getOpenedFiles().contains(f);
	}

	public IDEFile getAlreadyOpenedFile(IDEFile f) {
		List<IDEFile> opened = getOpenedFiles();
		int index = opened.indexOf(f);
		if (index != -1) {
			return opened.get(index);
		}
		return null;
	}

	public FileEditorRSTA getOpenedFileTabEditor(IDEFile f) {
		for (FileEditorRSTA e : fileEditors) {
			if (e.getEditedFile().equals(f)) {
				return e;
			}
		}
		return null;
	}

	public void openFile(IDEFile f) {
		openFile(f, false);
	}

	public void openFile(IDEFile f, boolean forceReload) {
		if (!isIDEFileOpened(f)) {
			FileEditorRSTA editor = new FileEditorRSTA(this, f);
			f.getProject().caretPosCache.setStoredCaretPositionToEditor(editor);
			fileEditors.add(editor);
			ensureHasTab(editor);
			context.getWorkspace().saveData.putOpenFile(f, true);
			editor.requestFocus();
			fileTabs.setSelectedComponent(getOpenedFileTabEditor(f).getScrollPane());
		} else {
			f.transferListenersTo(getAlreadyOpenedFile(f));
			FileEditorRSTA edt = getOpenedFileTabEditor(f);
			ensureHasTab(edt);
			if (forceReload) {
				edt.reloadFromFile();
			}
			fileTabs.setSelectedComponent(edt.getScrollPane());
			changeToSelectedEditor();
		}
	}

	private void ensureHasTab(FileEditorRSTA editor) {
		if (fileTabs.indexOfComponent(editor.getScrollPane()) == -1) {
			IDEFile f = editor.getEditedFile();
			fileTabs.addTab(f.getName(), getFileIcon(f), editor.getScrollPane(), f.getPath());
		}
	}

	public void setFileTabModified(FileEditorRSTA editor, boolean bln) {
		int index = fileEditors.indexOf(editor);
		if (index != -1) {
			TabbedPaneTab tab = fileTabs.getTab(index);
			Font f = tab.getHeaderFont();
			tab.setHeaderFont(f.deriveFont(bln ? f.getStyle() | Font.BOLD : f.getStyle() & ~Font.BOLD));
		}
	}

	private static final ImageIcon DEFAULT_FILE_ICON = new ImageIcon(IDEResources.ACCESSOR.getByteArray("scripting/ui/tabs/sourcefile.png"));

	public Icon getFileIcon(IDEFile f) {
		//currently it's static
		return DEFAULT_FILE_ICON;
	}

	public void openTemplate(String templateName, PSIDETemplateVar... vars) {
		//TODO
		/*byte[] data = ResourceAccess.getByteArray("scripting/template/" + templateName);
		String str = new String(data, StandardCharsets.UTF_8);
		for (PSIDETemplateVar var : vars) {
			str = str.replace("%" + var.name + "%", var.replValue);
		}
		textArea.setText(str);
		textArea.setCaretPosition(0);*/
	}

	public void createNewFileInProjectByTemplateAndOpen(IDEProject prj, String filePath, String templateName, PSIDETemplateVar... vars) {
		IDEFile file = prj.getFile(filePath);
		file.setBytes(getTemplateData(templateName, vars));
		openFile(file);
	}

	public static String getTemplateDataStr(String templateName, PSIDETemplateVar... vars) {
		return new String(getTemplateData(templateName, vars));
	}

	public static byte[] getTemplateData(String templateName, PSIDETemplateVar... vars) {
		byte[] data = IDEResources.ACCESSOR.getByteArray("scripting/template/" + templateName);
		String str = new String(data, StandardCharsets.UTF_8);
		for (PSIDETemplateVar var : vars) {
			str = str.replace("%" + var.name + "%", var.replValue);
		}
		return str.getBytes(StandardCharsets.UTF_8);
	}

	public void openProject(IDEProject prj) {
		openProject(prj, true);
	}

	public void openProject(IDEProject prj, boolean makeTree) {
		if (context.hasOpenedProjectByPath(prj)) {
			return;
		}
		if (findLoadedProjectByProdId(prj.getManifest().getProductId()) != null) {
			DialogUtils.showErrorMessage(this, "Could not open project", "A project with Product ID \"" + prj.getManifest().getProductId() + "\" is already opened.");
			return;
		}
		if (context.setUpProjectToContext(prj)) {
			if (makeTree) {
				projectTree.addProject(prj);
			}
			String projectPath = prj.getProjectPath();
			context.getWorkspace().saveData.putOpenedProjectPath(projectPath);
		}
	}
	
	public void resyncProject(IDEProject prj) {
		ProjectNode n = getProjectTree().findProjectNode(prj);
		if (n != null) {
			n.updateDependencyNodes();
			for (FileEditorRSTA editor : fileEditors) {
				IDEFile f = editor.getEditedFile();
				if (f.getProject() == prj) {
					editor.resync();
				}
			}
		}
	}

	public void closeProject(IDEProject prj) {
		for (int i = 0; i < fileEditors.size(); i++) {
			FileEditorRSTA e = fileEditors.get(i);
			IDEFile f = e.getEditedFile();
			if (f.getProject() == prj) {
				fileTabs.removeTabAt(i);
				i--;
			}
		}
		context.closeProject(prj);
		projectTree.removeProject(prj);
		context.getWorkspace().saveData.write();
	}

	public void deleteProject(IDEProject prj) {
		closeProject(prj);
		prj.getIDERoot().delete();
	}

	public void deleteFile(FSFile fsf) {
		fsf.delete();
		for (int i = 0; i < fileEditors.size(); i++) {
			FileEditorRSTA e = fileEditors.get(i);
			IDEFile f = e.getEditedFile();
			if (f.equals(fsf)) {
				fileTabs.removeTabAt(i);
				i--;
			}
		}
	}

	public List<IDEFile> getOpenedFiles() {
		List<IDEFile> files = new ArrayList<>();
		for (FileEditorRSTA e : fileEditors) {
			files.add(e.getEditedFile());
		}
		return files;
	}

	public void syncOpenedFilesWithSaveData() {
		IDEWorkspace ws = context.getWorkspace();
		if (ws != null) {
			ws.saveData.clearOpenedFiles();

			for (IDEFile of : getOpenedFiles()) {
				ws.saveData.putOpenFile(of, false);
			}

			ws.saveData.write();
		}
	}

	public boolean saveFile(FileEditorRSTA editor, boolean dlg) {
		if (editor != null) {
			FileEditorRSTA.SaveResult rsl = editor.saveTextToFile(dlg);
			editor.getEditedFile().getProject().callSaveListeners(rsl);
			return rsl != FileEditorRSTA.SaveResult.CANCELLED;
		}
		return true;
	}

	public boolean saveAllFiles(boolean dlg) {
		List<IDEProject> savedProjects = new ArrayList<>();
		boolean retval = true;
		for (FileEditorRSTA editor : fileEditors) {
			FileEditorRSTA.SaveResult result = editor.saveTextToFile(dlg);
			if (result == FileEditorRSTA.SaveResult.CANCELLED) {
				retval = false; //let the project listeners be called, but return false
				break;
			}
			else if (result == FileEditorRSTA.SaveResult.SAVED) {
				ArraysEx.addIfNotNullOrContains(savedProjects, editor.getEditedFile().getProject());
			}
		}
		for (IDEProject proj : savedProjects) {
			proj.callSaveListeners(FileEditorRSTA.SaveResult.SAVED);
		}
		return retval;
	}

	public void syncCaretPositionsWithCache() {
		for (IDEFile f : getOpenedFiles()) {
			FileEditorRSTA rsta = getOpenedFileTabEditor(f);
			f.getProject().caretPosCache.storeCaretPositionOfEditor(rsta);
		}
	}

	public AutoComplete getAutoCompletionEngine() {
		return ac;
	}

	public void makeTree() {
		projectTree.getRootNode().removeAllChildren();
		for (IDEProject op : context.openedProjects) {
			projectTree.addProject(op);
		}
		projectTree.reload();
	}

	public void buildErrorTable(Preprocessor pp) {
		DefaultTableModel tblModel = (DefaultTableModel) errorTable.getModel();
		tblModel.setRowCount(0);
		if (pp != null) {
			for (CompilerExceptionData d : pp.collectExceptions()) {
				tblModel.addRow(new String[]{d.fileName, String.valueOf(d.lineNumberStart), d.text});
			}
		}
	}

	/**
	 * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        ideSplitPane = new javax.swing.JSplitPane();
        errorTableScrollPane = new javax.swing.JScrollPane();
        errorTable = new javax.swing.JTable();
        topSplitPane = new javax.swing.JSplitPane();
        projectTreeSP = new javax.swing.JScrollPane();
        projectTree = new ctrmap.pokescript.ide.system.project.tree.IDEProjectTree();
        fileTabs = new xstandard.gui.components.tabbedpane.JTabbedPaneEx();
        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        btnNewProject = new javax.swing.JMenuItem();
        btnOpenProject = new javax.swing.JMenuItem();
        projFileSep = new javax.swing.JPopupMenu.Separator();
        btnOpen = new javax.swing.JMenuItem();
        btnSave = new javax.swing.JMenuItem();
        btnSaveAll = new javax.swing.JMenuItem();
        fileWSSep = new javax.swing.JPopupMenu.Separator();
        btnChangeWorkspace = new javax.swing.JMenuItem();
        optionsMenu = new javax.swing.JMenu();
        btnOpenSettings = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("PokéScript IDE");
        setLocationByPlatform(true);
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentMoved(java.awt.event.ComponentEvent evt) {
                formComponentMoved(evt);
            }
        });
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        ideSplitPane.setDividerLocation(300);
        ideSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        ideSplitPane.setResizeWeight(0.9);

        errorTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "File", "Line", "Cause"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Integer.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        errorTableScrollPane.setViewportView(errorTable);

        ideSplitPane.setRightComponent(errorTableScrollPane);

        topSplitPane.setDividerLocation(210);
        topSplitPane.setResizeWeight(0.22);

        projectTree.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 3, 3, 1));
        projectTreeSP.setViewportView(projectTree);

        topSplitPane.setLeftComponent(projectTreeSP);

        fileTabs.setCloseable(true);
        topSplitPane.setRightComponent(fileTabs);

        ideSplitPane.setLeftComponent(topSplitPane);

        fileMenu.setText("File");

        btnNewProject.setText("New Project");
        btnNewProject.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNewProjectActionPerformed(evt);
            }
        });
        fileMenu.add(btnNewProject);

        btnOpenProject.setText("Open Project");
        btnOpenProject.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOpenProjectActionPerformed(evt);
            }
        });
        fileMenu.add(btnOpenProject);
        fileMenu.add(projFileSep);

        btnOpen.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        btnOpen.setText("Open");
        btnOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOpenActionPerformed(evt);
            }
        });
        fileMenu.add(btnOpen);

        btnSave.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        btnSave.setText("Save");
        btnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveActionPerformed(evt);
            }
        });
        fileMenu.add(btnSave);

        btnSaveAll.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.SHIFT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK));
        btnSaveAll.setText("Save All");
        btnSaveAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveAllActionPerformed(evt);
            }
        });
        fileMenu.add(btnSaveAll);
        fileMenu.add(fileWSSep);

        btnChangeWorkspace.setText("Change Workspace");
        btnChangeWorkspace.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnChangeWorkspaceActionPerformed(evt);
            }
        });
        fileMenu.add(btnChangeWorkspace);

        menuBar.add(fileMenu);

        optionsMenu.setText("Options");

        btnOpenSettings.setText("Settings");
        btnOpenSettings.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOpenSettingsActionPerformed(evt);
            }
        });
        optionsMenu.add(btnOpenSettings);

        menuBar.add(optionsMenu);

        setJMenuBar(menuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(ideSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 745, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(ideSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 495, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOpenActionPerformed
		FSFile f = XFileDialog.openFileDialog(LangConstants.LANG_SOURCE_FILE_EXTENSION_FILTER, LangConstants.LANG_HEADER_EXTENSION_FILTER);
		if (f != null) {
		}
    }//GEN-LAST:event_btnOpenActionPerformed

    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
		FileEditorRSTA editor = getSelectedFileEditor();

		saveFile(editor, false);
    }//GEN-LAST:event_btnSaveActionPerformed

    private void formComponentMoved(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentMoved
		ac.close();
    }//GEN-LAST:event_formComponentMoved

    private void btnOpenSettingsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOpenSettingsActionPerformed
		settingsFrame.setLocationRelativeTo(this);
		settingsFrame.setVisible(true);
    }//GEN-LAST:event_btnOpenSettingsActionPerformed

    private void btnNewProjectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNewProjectActionPerformed
		ProjectCreationDialog makeProjDlg = new ProjectCreationDialog(this, true);
		makeProjDlg.setVisible(true);

		IDEProject prj = makeProjDlg.getResult();
		if (prj != null) {
			openProject(prj);
		}
    }//GEN-LAST:event_btnNewProjectActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
		if (saveWorkspace(true)) {
			if (standalone) {
				System.exit(0);
			} else {
				setVisible(false);
			}
		}
    }//GEN-LAST:event_formWindowClosing

    private void btnChangeWorkspaceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnChangeWorkspaceActionPerformed
		if (closeWorkspace(true)) {
			setVisible(false);

			InitialLaunchDialog dlg = new InitialLaunchDialog(this, true);
			dlg.setVisible(true);

			IDEWorkspace ws = dlg.getResult();

			if (ws != null) {
				loadWorkspace(ws);
			}
			setVisible(true);
		}
    }//GEN-LAST:event_btnChangeWorkspaceActionPerformed

    private void btnOpenProjectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOpenProjectActionPerformed
		FSFile projectFile = XFileDialog.openFileDialog(IDEProject.IDE_PROJECT_EXTENSION_FILTER);

		if (projectFile != null) {
			IDEProject proj = new IDEProject(projectFile);
			openProject(proj, true);
		}
    }//GEN-LAST:event_btnOpenProjectActionPerformed

    private void btnSaveAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveAllActionPerformed
		saveAllFiles(false);
    }//GEN-LAST:event_btnSaveAllActionPerformed

	public String getCode() {
		//TODO
		//return textArea.getText();
		return null;
	}

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem btnChangeWorkspace;
    private javax.swing.JMenuItem btnNewProject;
    private javax.swing.JMenuItem btnOpen;
    private javax.swing.JMenuItem btnOpenProject;
    private javax.swing.JMenuItem btnOpenSettings;
    private javax.swing.JMenuItem btnSave;
    private javax.swing.JMenuItem btnSaveAll;
    private javax.swing.JTable errorTable;
    private javax.swing.JScrollPane errorTableScrollPane;
    private javax.swing.JMenu fileMenu;
    private xstandard.gui.components.tabbedpane.JTabbedPaneEx fileTabs;
    private javax.swing.JPopupMenu.Separator fileWSSep;
    private javax.swing.JSplitPane ideSplitPane;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenu optionsMenu;
    private javax.swing.JPopupMenu.Separator projFileSep;
    private ctrmap.pokescript.ide.system.project.tree.IDEProjectTree projectTree;
    private javax.swing.JScrollPane projectTreeSP;
    private javax.swing.JSplitPane topSplitPane;
    // End of variables declaration//GEN-END:variables
}
