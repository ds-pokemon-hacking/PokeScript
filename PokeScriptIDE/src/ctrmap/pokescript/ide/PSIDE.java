package ctrmap.pokescript.ide;

import ctrmap.pokescript.CompilerExceptionData;
import ctrmap.pokescript.LangCompiler;
import ctrmap.pokescript.LangConstants;
import ctrmap.pokescript.stage0.Preprocessor;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.pokescript.ide.autocomplete.AutoComplete;
import ctrmap.pokescript.ide.autocomplete.AutoCompleteKeyListener;
import ctrmap.stdlib.fs.accessors.DiskFile;
import ctrmap.stdlib.gui.file.CMFileDialog;
import ctrmap.pokescript.LangPlatform;
import ctrmap.pokescript.ide.forms.ProjectCreationDialog;
import ctrmap.pokescript.ide.settings.IDESettings;
import ctrmap.pokescript.ide.system.IDEResourceReference;
import ctrmap.pokescript.ide.system.IDESetupFile;
import ctrmap.pokescript.ide.system.ResourcePathType;
import ctrmap.pokescript.ide.system.project.IDEContext;
import ctrmap.pokescript.ide.system.project.IDEProject;
import ctrmap.pokescript.ide.system.project.tree.nodes.ProjectNode;
import ctrmap.pokescript.ide.system.savedata.IDEWorkspace;
import ctrmap.pokescript.stage2.VScriptHeaderGen;
import ctrmap.scriptformats.gen5.VScriptFile;
import ctrmap.stdlib.fs.FSFile;
import ctrmap.stdlib.fs.FSUtil;
import ctrmap.stdlib.gui.DialogUtils;
import ctrmap.stdlib.gui.components.CaretMotion;
import ctrmap.stdlib.gui.file.ExtensionFilter;
import ctrmap.stdlib.res.ResourceAccess;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.BadLocationException;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import static org.fife.ui.rsyntaxtextarea.SyntaxConstants.SYNTAX_STYLE_PP;
import org.fife.ui.rsyntaxtextarea.parser.AbstractParser;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParseResult;
import org.fife.ui.rsyntaxtextarea.parser.ParseResult;

public class PSIDE extends javax.swing.JFrame {

	public static final IDEResourceReference PSIDE_SETUP_RESOURCE
			= new IDEResourceReference(ResourcePathType.INTERNAL, "scripting/IDE.yml");

	public IDESetupFile setup;

	public IDEContext context;
	public IDEWorkspace ws;

	private Preprocessor localDoc = null;

	public LangCompiler.CompilerArguments compilerCfg;
	private TextAreaMarkManager marks = new TextAreaMarkManager();

	private AutoComplete ac;

	private IDESettings settingsFrame;

	public static final String PSIDE_ERR_COLUMN_ID_FILE = "File";
	public static final String PSIDE_ERR_COLUMN_ID_LINE = "Line";
	public static final String PSIDE_ERR_COLUMN_ID_CAUSE = "Cause";

	public PSIDE(IDEWorkspace workspace) {
		initComponents();

		initContext();

		loadSetup(PSIDE_SETUP_RESOURCE);

		loadWorkspace(workspace);

		/*if (args.getPlatform() == LangPlatform.EV_SWAN) {
			JMenuItem makeHeader = new JMenuItem("Generate script header");
			makeHeader.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					File dest = CMFileDialog.openSaveFileDialog(LangConstants.LANG_SOURCE_FILE_EXTENSION_FILTER);

					if (dest != null) {
						VScriptHeaderGen.writeScrHeaderToFile(dest, localDoc);
					}
				}
			});

			compileMenu.add(makeHeader);
		}*/
		errorTable.getColumn(PSIDE_ERR_COLUMN_ID_FILE).setMaxWidth(250);
		errorTable.getColumn(PSIDE_ERR_COLUMN_ID_LINE).setMaxWidth(35);

		//add basic namespaces
		ac = new AutoComplete(textArea, marks);

		textArea.setSyntaxEditingStyle(SYNTAX_STYLE_PP);
		textArea.addParser(new PPParser());
		textArea.getDocument().addDocumentListener(new AutoCompleteListener());
		textArea.getDocument().addDocumentListener(marks);
		textAreaSP.setLineNumbersEnabled(true); //has to be called here instead of initComponents since netbeans generates this line before the text area is added to the scroll pane which breaks it

		textArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, KeyEvent.CTRL_DOWN_MASK), "AutoCompleteHotkey");
		textArea.getActionMap().put("AutoCompleteHotkey", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				updateAutocomplete();
				ac.attachWindowLayoutToNameAndOpen();
			}
		});

		textArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "EndACHotKey");
		textArea.getActionMap().put("EndACHotKey", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ac.closeAndInvalidate();
			}
		});

		textArea.addCaretListener(new CaretListener() {
			@Override
			public void caretUpdate(CaretEvent e) {
				if (!ac.isCaretInAC()) {
					ac.close();
				}
			}
		});
		textArea.addKeyListener(new AutoCompleteKeyListener(ac));

		settingsFrame = new IDESettings(this);
	}

	public void initContext() {
		context = new IDEContext();
	}

	public void loadSetup(IDEResourceReference setupPath) {
		FSFile setupFile = setupPath.resolve(new DiskFile(System.getProperty("user.dir")), context);
		if (setupFile == null) {
			DialogUtils.showErrorMessage(this, "Internal error", "The setup file " + setupPath.path + " could not be found! Can not continue.");
			System.exit(0);
		}
		setup = new IDESetupFile(setupFile);
	}

	public void loadWorkspace(IDEWorkspace ws) {
		this.ws = ws;

		projectTree.removeAllChildren();

		for (int i = 0; i < ws.saveData.openedProjectPaths.size(); i++) {
			String projectPath = ws.saveData.openedProjectPaths.get(i);
			File projectFile = new File(projectPath);

			if (IDEProject.isIDEProject(projectFile)) {
				openProject(new IDEProject(new DiskFile(projectFile)), false);
			} else {
				ws.saveData.openedProjectPaths.remove(i);
				i--;
			}
		}
		makeTree();
	}

	public void openFile(File f) {
		try {
			source = f;
			byte[] b = Files.readAllBytes(f.toPath());
			textArea.setText(new String(b, "UTF-8"));
			textArea.setCaretPosition(0);
		} catch (UnsupportedEncodingException ex) {
			Logger.getLogger(PSIDE.class.getName()).log(Level.SEVERE, null, ex);
		} catch (IOException ex) {
			Logger.getLogger(PSIDE.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void openTemplate(String templateName, PSIDETemplateVar... vars) {
		byte[] data = ResourceAccess.getByteArray("scripting/template/" + templateName);
		String str = new String(data, StandardCharsets.UTF_8);
		for (PSIDETemplateVar var : vars) {
			str = str.replace("%" + var.name + "%", var.replValue);
		}
		textArea.setText(str);
		textArea.setCaretPosition(0);
	}
	
	public static byte[] getTemplateData(String templateName, PSIDETemplateVar... vars){
		byte[] data = ResourceAccess.getByteArray("scripting/template/" + templateName);
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
		if (context.setUpProjectToContext(prj)) {
			if (makeTree){
				makeTree();
			}
			String projectPath = prj.getProjectPath();
			ws.saveData.putOpenedProjectPath(projectPath);
		}
	}

	public void makeTree() {
		projectTree.getRootNode().removeAllChildren();
		for (IDEProject op : context.openedProjects) {
			projectTree.addProject(op);
		}
		projectTree.reload();
	}

	public void buildErrorTable() {
		if (localDoc != null) {
			DefaultTableModel tblModel = (DefaultTableModel) errorTable.getModel();
			tblModel.setRowCount(0);
			for (CompilerExceptionData d : localDoc.collectExceptions()) {
				tblModel.addRow(new String[]{d.fileName, String.valueOf(d.lineNumberStart), d.text});
			}
		}
	}

	private class AutoCompleteListener implements DocumentListener {

		@Override
		public void insertUpdate(DocumentEvent e) {
			try {
				String text = e.getDocument().getText(e.getOffset(), e.getLength());
				if (text.endsWith(".")) {
					updateAutocomplete(CaretMotion.FORWARD);
					ac.attachWindowLayoutToNameAndOpen();
				}
				if (ac.isVisible()) {
					updateAutocomplete(CaretMotion.FORWARD);
					if (ac.isHintRoot() && ac.getLastQuery().length() == 0) {
						ac.close();
					}
				}
				if (ac.getCurrentRslHandler() != null) {
					ac.cancelResultHandlerIfMarksInvalidated();
				}
			} catch (BadLocationException ex) {
				Logger.getLogger(PSIDE.class.getName()).log(Level.SEVERE, null, ex);
			}
		}

		@Override
		public void removeUpdate(DocumentEvent e) {
			ac.close();
		}

		@Override
		public void changedUpdate(DocumentEvent e) {
		}
	}

	public void updateAutocomplete() {
		updateAutocomplete(CaretMotion.NONE);
	}

	Point lastWinLoc;

	public void updateAutocomplete(CaretMotion motion) {
		int caretPos = textArea.getCaretPosition();
		if (motion == CaretMotion.FORWARD) {
			caretPos++;
		} else if (motion == CaretMotion.BACKWARD) {
			caretPos -= 2;
		}
		if (caretPos < 0) {
			return;
		}

		ac.updateAutocomplete(caretPos);
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        ideSplitPane = new javax.swing.JSplitPane();
        errorTableScrollPane = new javax.swing.JScrollPane();
        errorTable = new javax.swing.JTable();
        topSplitPane = new javax.swing.JSplitPane();
        textAreaSP = new org.fife.ui.rtextarea.RTextScrollPane();
        textArea = new ctrmap.pokescript.ide.CustomRSTA();
        projectTreeSP = new javax.swing.JScrollPane();
        projectTree = new ctrmap.pokescript.ide.system.project.tree.IDEProjectTree();
        menuBar = new javax.swing.JMenuBar();
        projectsMenu = new javax.swing.JMenu();
        btnNewProject = new javax.swing.JMenuItem();
        fileMenu = new javax.swing.JMenu();
        btnOpen = new javax.swing.JMenuItem();
        btnSave = new javax.swing.JMenuItem();
        compileMenu = new javax.swing.JMenu();
        btnCompileToFile = new javax.swing.JMenuItem();
        optionsMenu = new javax.swing.JMenu();
        btnOpenSettings = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("PS6-IDE for CTRMap");
        setLocationByPlatform(true);
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentMoved(java.awt.event.ComponentEvent evt) {
                formComponentMoved(evt);
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

        textArea.setColumns(20);
        textArea.setRows(5);
        textAreaSP.setViewportView(textArea);

        topSplitPane.setRightComponent(textAreaSP);

        projectTree.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 3, 3, 1));
        projectTreeSP.setViewportView(projectTree);

        topSplitPane.setLeftComponent(projectTreeSP);

        ideSplitPane.setLeftComponent(topSplitPane);

        projectsMenu.setText("Projects");

        btnNewProject.setText("New Project");
        btnNewProject.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNewProjectActionPerformed(evt);
            }
        });
        projectsMenu.add(btnNewProject);

        menuBar.add(projectsMenu);

        fileMenu.setText("File");

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

        menuBar.add(fileMenu);

        compileMenu.setText("Compile");

        btnCompileToFile.setText("Compile to File");
        btnCompileToFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCompileToFileActionPerformed(evt);
            }
        });
        compileMenu.add(btnCompileToFile);

        menuBar.add(compileMenu);

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

	private File source;
	private Runnable saveCallback;

	public void setSaveCallback(Runnable r) {
		saveCallback = r;
	}

	public void setSaveTarget(File f) {
		source = f;
	}

    private void btnOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOpenActionPerformed
		File f = CMFileDialog.openFileDialog(LangConstants.LANG_SOURCE_FILE_EXTENSION_FILTER, LangConstants.LANG_NATIVE_DEFINITION_EXTENSION_FILTER);
		if (f != null) {
			openFile(f);
		}
    }//GEN-LAST:event_btnOpenActionPerformed

    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
		if (source == null) {
			source = CMFileDialog.openSaveFileDialog(LangConstants.LANG_SOURCE_FILE_EXTENSION_FILTER, LangConstants.LANG_NATIVE_DEFINITION_EXTENSION_FILTER);
		}
		if (source != null) {
			try {
				source.delete();
				Files.write(source.toPath(), getCode().getBytes("UTF-8"), StandardOpenOption.CREATE_NEW);
			} catch (UnsupportedEncodingException ex) {
				Logger.getLogger(PSIDE.class.getName()).log(Level.SEVERE, null, ex);
			} catch (IOException ex) {
				Logger.getLogger(PSIDE.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
		if (saveCallback != null) {
			saveCallback.run();
		}
    }//GEN-LAST:event_btnSaveActionPerformed

    private void formComponentMoved(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentMoved
		ac.close();
    }//GEN-LAST:event_formComponentMoved

    private void btnCompileToFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCompileToFileActionPerformed
		ExtensionFilter filter = compilerCfg.getPlatform().extensionFilter;
		File f = CMFileDialog.openSaveFileDialog(filter);

		if (f != null) {
			new DiskFile(f).setBytes(LangCompiler.compileStreamToBinary(new ByteArrayInputStream(textArea.getText().getBytes(StandardCharsets.UTF_8)), compilerCfg));

			if (compilerCfg.getPlatform() == LangPlatform.EV_SWAN) {
				DiskFile ascii = new DiskFile(FSUtil.getFileNameWithoutExtension(f.getAbsolutePath()) + ".txt");
				VScriptFile scr = LangCompiler.compileStreamV(new ByteArrayInputStream(textArea.getText().getBytes(StandardCharsets.UTF_8)), compilerCfg);
				ascii.setBytes(scr.getASCII().getBytes(StandardCharsets.UTF_8));
			}
		}
    }//GEN-LAST:event_btnCompileToFileActionPerformed

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

	public String getCode() {
		return textArea.getText();
	}

	public class PPParser extends AbstractParser {

		@Override
		public ParseResult parse(RSyntaxDocument doc, String style) {
			if (compilerCfg == null) {
				return null;
			}
			try {
				String ctxName = "PSIDEScr";
				if (source != null) {
					ctxName = source.getName();
				}
				Preprocessor newPP = new Preprocessor(new ByteArrayInputStream(doc.getText(0, doc.getLength()).getBytes("UTF-8")), ctxName, compilerCfg);
				NCompileGraph cg = newPP.getCompileGraph();
				localDoc = newPP;

				if (cg != null) {
					ac.rebuildNodeTree(localDoc);
				}
				buildErrorTable();

				return new DefaultParseResult(this);
			} catch (BadLocationException | UnsupportedEncodingException ex) {
				Logger.getLogger(PSIDE.class.getName()).log(Level.SEVERE, null, ex);
			}
			return null;
		}
	}

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem btnCompileToFile;
    private javax.swing.JMenuItem btnNewProject;
    private javax.swing.JMenuItem btnOpen;
    private javax.swing.JMenuItem btnOpenSettings;
    private javax.swing.JMenuItem btnSave;
    private javax.swing.JMenu compileMenu;
    private javax.swing.JTable errorTable;
    private javax.swing.JScrollPane errorTableScrollPane;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JSplitPane ideSplitPane;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenu optionsMenu;
    private ctrmap.pokescript.ide.system.project.tree.IDEProjectTree projectTree;
    private javax.swing.JScrollPane projectTreeSP;
    private javax.swing.JMenu projectsMenu;
    private ctrmap.pokescript.ide.CustomRSTA textArea;
    private org.fife.ui.rtextarea.RTextScrollPane textAreaSP;
    private javax.swing.JSplitPane topSplitPane;
    // End of variables declaration//GEN-END:variables
}
