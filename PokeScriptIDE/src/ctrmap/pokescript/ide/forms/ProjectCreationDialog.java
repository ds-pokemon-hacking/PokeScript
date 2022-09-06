package ctrmap.pokescript.ide.forms;

import ctrmap.pokescript.LangConstants;
import ctrmap.pokescript.LangPlatform;
import ctrmap.pokescript.ide.PSIDE;
import ctrmap.pokescript.ide.PSIDETemplateVar;
import ctrmap.pokescript.ide.system.IDEResourceReference;
import ctrmap.pokescript.ide.system.PlatformInfoFile;
import ctrmap.pokescript.ide.system.ResourcePathType;
import ctrmap.pokescript.ide.system.SDKInfoFile;
import ctrmap.pokescript.ide.system.project.IDEProject;
import xstandard.fs.FSFile;
import xstandard.fs.FSUtil;
import xstandard.fs.accessors.DiskFile;
import xstandard.gui.components.NoSpaceFirstDocument;
import xstandard.gui.components.listeners.DocumentAdapterEx;
import xstandard.text.StringEx;
import java.awt.Color;
import java.io.File;
import javax.swing.DefaultListModel;
import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.text.DefaultCaret;

public class ProjectCreationDialog extends javax.swing.JDialog {

	private PSIDE ide;

	private DefaultListModel<PlatformInfoFile.PlatformInfo> archModel = new DefaultListModel<>();
	private DefaultListModel<SDKInfoFile.SDKInfo> sdkModel = new DefaultListModel<>();

	private static final SDKInfoFile.SDKInfo NO_SDK = new SDKInfoFile.SDKInfo("No SDK", null, null);

	private static final int TABIDX_ARCH = 0;
	private static final int TABIDX_SDK = 1;
	private static final int TABIDX_NAMELOC = 2;
	private static final int TABIDX_MAX = TABIDX_NAMELOC;

	private boolean loaded = false;

	private IDEProject result;

	public ProjectCreationDialog(PSIDE parent, boolean modal) {
		super(parent, modal);
		ide = parent;
		initComponents();

		setLocationRelativeTo(ide);

		tabs.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				setupButtonsAndTabEnableStates();
			}
		});

		projectName.setDocument(new NoSpaceFirstDocument());
		prodID.setDocument(new NoSpaceFirstDocument());
		mainClass.setDocument(new NoSpaceFirstDocument());

		architectureList.setModel(archModel);
		architectureList.addListSelectionListener((ListSelectionEvent e) -> {
			if (loaded) {
				setUpUIBySelectedArch();
			}
		});

		((DefaultCaret) archDesc.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
		archDesc.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);

		sdkList.setModel(sdkModel);
		sdkList.addListSelectionListener((ListSelectionEvent e) -> {
			if (loaded) {
				setUpUIBySelectedSDK();
			}
		});

		projectName.getDocument().addDocumentListener(new DocumentAdapterEx() {
			@Override
			public void textChangedUpdate(DocumentEvent e) {
				SwingUtilities.invokeLater(() -> {
					checkProjectNameValidSetWarn();
					setupButtonsAndTabEnableStates();
				});
			}
		});

		prodID.getDocument().addDocumentListener(new DocumentAdapterEx() {
			@Override
			public void textChangedUpdate(DocumentEvent e) {
				SwingUtilities.invokeLater(() -> {
					checkProductIDValidSetWarn();
					setupButtonsAndTabEnableStates();
				});
			}
		});

		mainClass.getDocument().addDocumentListener(new DocumentAdapterEx() {
			@Override
			public void textChangedUpdate(DocumentEvent e) {
				SwingUtilities.invokeLater(() -> {
					checkMainClassValidSetWarn();
					setupButtonsAndTabEnableStates();
				});
			}
		});

		loadArchs();
		setUpUIBySelectedArch();
		btnIsCreateMainClassActionPerformed(null);
		checkProductIDValidSetWarn();
		checkProjectNameValidSetWarn();
	}

	public IDEProject getResult() {
		return result;
	}

	public final void loadArchs() {
		loaded = false;
		archModel.removeAllElements();
		for (PlatformInfoFile pif : ide.setup.getPlatformInfoFiles()) {
			for (PlatformInfoFile.PlatformInfo pi : pif.info) {
				archModel.addElement(pi);
			}
		}
		loaded = true;
	}

	public final void loadSDKs() {
		loaded = false;
		sdkModel.removeAllElements();
		if (getSelectedArch() != null) {
			SDKInfoFile sif = getSelectedArch().getSDKInfoFile();
			if (sif != null) {
				sdkModel.addElement(NO_SDK);
				for (SDKInfoFile.SDKInfo i : sif.info) {
					sdkModel.addElement(i);
				}
			}
		}
		loaded = true;
	}

	public void setUpUIBySelectedArch() {
		setupButtonsAndTabEnableStates();

		PlatformInfoFile.PlatformInfo arch = getSelectedArch();
		if (arch != null) {
			archDesc.setText(arch.getHTMLDescContent());
			sdkList.clearSelection();
		} else {
			archDesc.setText("(No architecture selected)");
		}
		loadSDKs();
		setUpUIBySelectedSDK();
	}

	public void setUpUIBySelectedSDK() {
		setupButtonsAndTabEnableStates();

		SDKInfoFile.SDKInfo sdk = getSelectedSDK();
		if (sdk != null) {
			sdkName.setText(sdk.toString());
			sdkLocation.setText(sdk.ref == null ? "-" : sdk.ref.typeToString());
			compilerDefs.setText(String.join(", ", sdk.compilerDefinitions));
		} else {
			sdkName.setText("(No SDK selected)");
			sdkLocation.setText("-");
			compilerDefs.setText("-");
		}
	}

	public PlatformInfoFile.PlatformInfo getSelectedArch() {
		return architectureList.getSelectedValue();
	}

	public SDKInfoFile.SDKInfo getSelectedSDK() {
		return sdkList.getSelectedValue();
	}

	public boolean getSelectedArchHasSDKs() {
		PlatformInfoFile.PlatformInfo a = getSelectedArch();
		return a != null && a.SDKInfoPath != null;
	}

	public void finishAndClose() {
		FSFile projectFile = ide.context.getWorkspace().getProjectDir(projectName.getText());
		projectFile.mkdir();

		result = new IDEProject(projectFile, projectName.getText(), prodID.getText(), getSelectedArch().langPlatform);

		SDKInfoFile.SDKInfo sdk = getSelectedSDK();
		if (sdk.ref != null) {
			result.getManifest().addDependency(sdk.resolveToDependency());
			result.getManifest().setCompilerDefinitions(sdk.compilerDefinitions);
		}

		if (btnIsCreateMainClass.isSelected()) {
			FSUtil.writeBytesToFile(
					result.getClassFile(mainClass.getText()),
					PSIDE.getTemplateData("MainClass.pks", 
						new PSIDETemplateVar("CLASSNAME", mainClass.getText())
					)
			);
			result.getManifest().setMainClass(mainClass.getText());
		}

		dispose();
	}

	public void setupButtonsAndTabEnableStates() {
		int selectedTab = tabs.getSelectedIndex();
		btnPrevious.setEnabled(selectedTab > 0);

		boolean nextTabsEnable = getTabAllowsNext(selectedTab);

		boolean prevTabEnable = true;
		for (int i = 0; i < tabs.getTabCount() - 1; i++) {
			prevTabEnable &= getTabAllowsNext(i);
			if (prevTabEnable) {
				tabs.setEnabledAt(i + 1, true);
			} else {
				for (i++; i < tabs.getTabCount(); i++) {
					tabs.setEnabledAt(i, false);
				}
				break;
			}
		}

		btnNext.setEnabled(nextTabsEnable);

		if (tabs.getSelectedIndex() == TABIDX_MAX) {
			btnNext.setText("Finish");
		} else {
			btnNext.setText("Next >");
		}
	}

	public boolean checkProjectNameValidSetWarn() {
		String n = projectName.getText();
		projectNameAlert.setForeground(Color.BLACK);
		if (n == null || n.isEmpty()) {
			projectNameAlert.setText("Project name can not be empty");
			return false;
		} else {
			char firstNonLetterOrDigit = StringEx.findFirstNonLetterOrDigit(n, '_', ' ', '-');
			if (firstNonLetterOrDigit > 0) {
				projectNameAlert.setText("Project name can not contain the character \"" + firstNonLetterOrDigit + "\"");
				return false;
			}
			FSFile pf = ide.context.getWorkspace().getProjectDir(n);
			if (pf.exists()) {
				projectNameAlert.setText("Directory already exists in workspace.");
				return false;
			}

			projectNameAlert.setForeground(Color.DARK_GRAY);
			if (n.contains(" ")) {
				projectNameAlert.setText("Warning: It is recommended that project names do not contain spaces.");
			} else {
				projectNameAlert.setText("");
			}
			return true;
		}
	}

	public boolean checkProductIDValidSetWarn() {
		String n = prodID.getText();
		prodIdAlert.setForeground(Color.BLACK);
		if (n == null || n.isEmpty()) {
			prodIdAlert.setText("Product ID can not be empty");
			return false;
		} else {
			char firstNonLetterOrDigit = StringEx.findFirstNonLetterOrDigit(n, '_', '.');
			if (firstNonLetterOrDigit > 0) {
				prodIdAlert.setText("Product ID can not contain the character \"" + firstNonLetterOrDigit + "\"");
				return false;
			} else if (n.endsWith(".")) {
				prodIdAlert.setText("Product ID can not end with a \".\"");
				return false;
			} else if (ide.findLoadedProjectByProdId(n) != null){
				prodIdAlert.setText("There is already a project with this Product ID in the workspace.");
				return false;
			}

			prodIdAlert.setForeground(Color.DARK_GRAY);
			if (StringEx.containsUppercase(n)) {
				prodIdAlert.setText("Warning: Product ID should not use uppercase characters.");
			} else if (n.length() < 4) {
				prodIdAlert.setText("Warning: Product ID should be longer.");
			} else if (!n.contains(".")) {
				prodIdAlert.setText("Warning: Product ID should contain a \".\"");
			} else {
				prodIdAlert.setText("");
			}
			return true;
		}
	}

	public boolean checkMainClassValidSetWarn() {
		String n = mainClass.getText();
		mainClassAlert.setForeground(Color.BLACK);
		if (btnIsCreateMainClass.isSelected()) {
			if (n == null || n.isEmpty()) {
				mainClassAlert.setText("Main class can not be empty");
				return false;
			} else {
				char firstNonLetterOrDigit = StringEx.findFirstNonLetterOrDigit(n, '_');
				if (firstNonLetterOrDigit > 0) {
					mainClassAlert.setText("Main class name can not contain the character \"" + firstNonLetterOrDigit + "\"");
					return false;
				}

				mainClassAlert.setForeground(Color.DARK_GRAY);
				if (!Character.isUpperCase(n.charAt(0))) {
					mainClassAlert.setText("Warning: Class names should start with uppercase letters.");
				} else {
					mainClassAlert.setText("");
				}
				return true;
			}
		} else {
			mainClassAlert.setText("");
			return true;
		}
	}

	public boolean getTabAllowsNext(int tabIndex) {
		boolean nextTabsEnable = false;
		switch (tabIndex) {
			case TABIDX_ARCH:
				nextTabsEnable = getSelectedArchHasSDKs();
				break;
			case TABIDX_SDK:
				nextTabsEnable = getSelectedSDK() != null;
				break;
			case TABIDX_NAMELOC:
				nextTabsEnable = checkProjectNameValidSetWarn() && checkProductIDValidSetWarn() && checkMainClassValidSetWarn();
				break;
		}
		return nextTabsEnable;
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        tabs = new javax.swing.JTabbedPane();
        archPanel = new javax.swing.JPanel();
        archSP = new javax.swing.JScrollPane();
        architectureList = new javax.swing.JList<>();
        archChooseText = new javax.swing.JLabel();
        archDescPanel = new javax.swing.JPanel();
        archDescSP = new javax.swing.JScrollPane();
        archDesc = new javax.swing.JTextPane();
        sdkPanel = new javax.swing.JPanel();
        sdkChooseText = new javax.swing.JLabel();
        sdkSP = new javax.swing.JScrollPane();
        sdkList = new javax.swing.JList<>();
        detailsPanel = new javax.swing.JPanel();
        sdkNameLabel = new javax.swing.JLabel();
        sdkLocLabel = new javax.swing.JLabel();
        compilerDefLabel = new javax.swing.JLabel();
        compilerDefs = new javax.swing.JLabel();
        sdkName = new javax.swing.JLabel();
        sdkLocation = new javax.swing.JLabel();
        nameAndLocPanel = new javax.swing.JPanel();
        projectDetailsPanel = new javax.swing.JPanel();
        projectName = new javax.swing.JTextField();
        projectNameAlert = new javax.swing.JLabel();
        projectNameLabel = new javax.swing.JLabel();
        prodIdLabel = new javax.swing.JLabel();
        prodID = new javax.swing.JTextField();
        prodIdAlert = new javax.swing.JLabel();
        projectSetupPanel = new javax.swing.JPanel();
        btnIsCreateMainClass = new javax.swing.JCheckBox();
        mainClassNameLabel = new javax.swing.JLabel();
        mainClass = new javax.swing.JTextField();
        mainClassAlert = new javax.swing.JLabel();
        headingLabel = new javax.swing.JLabel();
        separator = new javax.swing.JSeparator();
        btnNext = new javax.swing.JButton();
        btnPrevious = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        tabs.setAlignmentX(0.0F);

        architectureList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        archSP.setViewportView(architectureList);

        archChooseText.setText("Choose an architecture");

        archDescPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Description"));

        archDescSP.setBorder(null);
        archDescSP.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        archDesc.setEditable(false);
        archDesc.setContentType("text/html"); // NOI18N
        archDesc.setOpaque(false);
        archDescSP.setViewportView(archDesc);

        javax.swing.GroupLayout archDescPanelLayout = new javax.swing.GroupLayout(archDescPanel);
        archDescPanel.setLayout(archDescPanelLayout);
        archDescPanelLayout.setHorizontalGroup(
            archDescPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(archDescPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(archDescSP, javax.swing.GroupLayout.DEFAULT_SIZE, 332, Short.MAX_VALUE))
        );
        archDescPanelLayout.setVerticalGroup(
            archDescPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, archDescPanelLayout.createSequentialGroup()
                .addComponent(archDescSP)
                .addContainerGap())
        );

        javax.swing.GroupLayout archPanelLayout = new javax.swing.GroupLayout(archPanel);
        archPanel.setLayout(archPanelLayout);
        archPanelLayout.setHorizontalGroup(
            archPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(archPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(archPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(archSP, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(archChooseText, javax.swing.GroupLayout.DEFAULT_SIZE, 183, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(archDescPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        archPanelLayout.setVerticalGroup(
            archPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(archPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(archChooseText)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(archPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(archDescPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(archSP, javax.swing.GroupLayout.DEFAULT_SIZE, 257, Short.MAX_VALUE))
                .addContainerGap())
        );

        tabs.addTab("Architecture", archPanel);

        sdkChooseText.setText("Choose an SDK");

        sdkList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        sdkSP.setViewportView(sdkList);

        detailsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Details"));

        sdkNameLabel.setText("SDK Name: ");

        sdkLocLabel.setText("SDK Location:");

        compilerDefLabel.setText("Compiler definitions:");

        compilerDefs.setText("-");

        sdkName.setText("-");

        sdkLocation.setText("-");

        javax.swing.GroupLayout detailsPanelLayout = new javax.swing.GroupLayout(detailsPanel);
        detailsPanel.setLayout(detailsPanelLayout);
        detailsPanelLayout.setHorizontalGroup(
            detailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(detailsPanelLayout.createSequentialGroup()
                .addGroup(detailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(detailsPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(detailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(detailsPanelLayout.createSequentialGroup()
                                .addComponent(compilerDefLabel)
                                .addGap(0, 225, Short.MAX_VALUE))
                            .addGroup(detailsPanelLayout.createSequentialGroup()
                                .addGroup(detailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(sdkLocLabel)
                                    .addComponent(sdkNameLabel))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(detailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(sdkName, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(sdkLocation, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))))
                    .addGroup(detailsPanelLayout.createSequentialGroup()
                        .addGap(20, 20, 20)
                        .addComponent(compilerDefs, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        detailsPanelLayout.setVerticalGroup(
            detailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(detailsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(detailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sdkNameLabel)
                    .addComponent(sdkName))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(detailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sdkLocLabel)
                    .addComponent(sdkLocation))
                .addGap(18, 18, 18)
                .addComponent(compilerDefLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(compilerDefs)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout sdkPanelLayout = new javax.swing.GroupLayout(sdkPanel);
        sdkPanel.setLayout(sdkPanelLayout);
        sdkPanelLayout.setHorizontalGroup(
            sdkPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sdkPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(sdkPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(sdkSP, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(sdkChooseText, javax.swing.GroupLayout.DEFAULT_SIZE, 183, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(detailsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        sdkPanelLayout.setVerticalGroup(
            sdkPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sdkPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(sdkChooseText)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(sdkPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(detailsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(sdkSP, javax.swing.GroupLayout.DEFAULT_SIZE, 257, Short.MAX_VALUE))
                .addContainerGap())
        );

        tabs.addTab("SDKs", sdkPanel);

        projectDetailsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Project details"));

        projectNameLabel.setText("Project name");

        prodIdLabel.setText("Product ID");

        javax.swing.GroupLayout projectDetailsPanelLayout = new javax.swing.GroupLayout(projectDetailsPanel);
        projectDetailsPanel.setLayout(projectDetailsPanelLayout);
        projectDetailsPanelLayout.setHorizontalGroup(
            projectDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(projectDetailsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(projectDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(prodIdLabel)
                    .addComponent(projectNameLabel))
                .addGap(16, 16, 16)
                .addGroup(projectDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(projectName, javax.swing.GroupLayout.DEFAULT_SIZE, 350, Short.MAX_VALUE)
                    .addComponent(prodID)
                    .addComponent(projectNameAlert, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(prodIdAlert, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        projectDetailsPanelLayout.setVerticalGroup(
            projectDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(projectDetailsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(projectDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(projectName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(projectNameLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(projectNameAlert, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(projectDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(prodIdLabel)
                    .addComponent(prodID, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(prodIdAlert, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        projectSetupPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Project setup"));

        btnIsCreateMainClass.setText("Create main class");
        btnIsCreateMainClass.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnIsCreateMainClassActionPerformed(evt);
            }
        });

        mainClassNameLabel.setText("Name:");

        javax.swing.GroupLayout projectSetupPanelLayout = new javax.swing.GroupLayout(projectSetupPanel);
        projectSetupPanel.setLayout(projectSetupPanelLayout);
        projectSetupPanelLayout.setHorizontalGroup(
            projectSetupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(projectSetupPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(projectSetupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnIsCreateMainClass)
                    .addGroup(projectSetupPanelLayout.createSequentialGroup()
                        .addGap(21, 21, 21)
                        .addComponent(mainClassNameLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(projectSetupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(mainClassAlert, javax.swing.GroupLayout.DEFAULT_SIZE, 348, Short.MAX_VALUE)
                            .addComponent(mainClass, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGap(119, 119, 119))
        );
        projectSetupPanelLayout.setVerticalGroup(
            projectSetupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(projectSetupPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btnIsCreateMainClass)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(projectSetupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(mainClassNameLabel)
                    .addComponent(mainClass, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(mainClassAlert, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout nameAndLocPanelLayout = new javax.swing.GroupLayout(nameAndLocPanel);
        nameAndLocPanel.setLayout(nameAndLocPanelLayout);
        nameAndLocPanelLayout.setHorizontalGroup(
            nameAndLocPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(nameAndLocPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(nameAndLocPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(projectDetailsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(projectSetupPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        nameAndLocPanelLayout.setVerticalGroup(
            nameAndLocPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(nameAndLocPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(projectDetailsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(projectSetupPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(35, Short.MAX_VALUE))
        );

        tabs.addTab("Project", nameAndLocPanel);

        headingLabel.setFont(new java.awt.Font("Consolas", 0, 24)); // NOI18N
        headingLabel.setForeground(new java.awt.Color(51, 51, 51));
        headingLabel.setText("Create a Project");

        btnNext.setText("Next >");
        btnNext.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNextActionPerformed(evt);
            }
        });

        btnPrevious.setText("< Previous");
        btnPrevious.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPreviousActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(separator)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(headingLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 322, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(tabs, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btnPrevious, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnNext, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(headingLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(separator, javax.swing.GroupLayout.PREFERRED_SIZE, 5, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tabs)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnNext)
                    .addComponent(btnPrevious))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnPreviousActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPreviousActionPerformed
		tabs.setSelectedIndex(tabs.getSelectedIndex() - 1);
    }//GEN-LAST:event_btnPreviousActionPerformed

    private void btnNextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNextActionPerformed
		int idx = tabs.getSelectedIndex() + 1;
		if (idx < tabs.getTabCount()) {
			tabs.setSelectedIndex(idx);
		} else {
			finishAndClose();
		}
    }//GEN-LAST:event_btnNextActionPerformed

    private void btnIsCreateMainClassActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnIsCreateMainClassActionPerformed
		mainClass.setEnabled(btnIsCreateMainClass.isSelected());
		checkMainClassValidSetWarn();
		setupButtonsAndTabEnableStates();
    }//GEN-LAST:event_btnIsCreateMainClassActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel archChooseText;
    private javax.swing.JTextPane archDesc;
    private javax.swing.JPanel archDescPanel;
    private javax.swing.JScrollPane archDescSP;
    private javax.swing.JPanel archPanel;
    private javax.swing.JScrollPane archSP;
    private javax.swing.JList<PlatformInfoFile.PlatformInfo> architectureList;
    private javax.swing.JCheckBox btnIsCreateMainClass;
    private javax.swing.JButton btnNext;
    private javax.swing.JButton btnPrevious;
    private javax.swing.JLabel compilerDefLabel;
    private javax.swing.JLabel compilerDefs;
    private javax.swing.JPanel detailsPanel;
    private javax.swing.JLabel headingLabel;
    private javax.swing.JTextField mainClass;
    private javax.swing.JLabel mainClassAlert;
    private javax.swing.JLabel mainClassNameLabel;
    private javax.swing.JPanel nameAndLocPanel;
    private javax.swing.JTextField prodID;
    private javax.swing.JLabel prodIdAlert;
    private javax.swing.JLabel prodIdLabel;
    private javax.swing.JPanel projectDetailsPanel;
    private javax.swing.JTextField projectName;
    private javax.swing.JLabel projectNameAlert;
    private javax.swing.JLabel projectNameLabel;
    private javax.swing.JPanel projectSetupPanel;
    private javax.swing.JLabel sdkChooseText;
    private javax.swing.JList<SDKInfoFile.SDKInfo> sdkList;
    private javax.swing.JLabel sdkLocLabel;
    private javax.swing.JLabel sdkLocation;
    private javax.swing.JLabel sdkName;
    private javax.swing.JLabel sdkNameLabel;
    private javax.swing.JPanel sdkPanel;
    private javax.swing.JScrollPane sdkSP;
    private javax.swing.JSeparator separator;
    private javax.swing.JTabbedPane tabs;
    // End of variables declaration//GEN-END:variables
}
