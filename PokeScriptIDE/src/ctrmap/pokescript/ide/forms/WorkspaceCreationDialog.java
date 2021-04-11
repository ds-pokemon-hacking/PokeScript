package ctrmap.pokescript.ide.forms;

import ctrmap.pokescript.ide.system.savedata.IDEWorkspace;
import ctrmap.stdlib.gui.DialogUtils;
import ctrmap.stdlib.gui.file.CMFileDialog;
import java.awt.Component;
import java.io.File;

public class WorkspaceCreationDialog extends javax.swing.JDialog {

	private IDEWorkspace result;

	public WorkspaceCreationDialog(java.awt.Frame parent, boolean modal) {
		super(parent, modal);
		init(parent);
	}
	
	public WorkspaceCreationDialog(java.awt.Dialog parent, boolean modal) {
		super(parent, modal);
		init(parent);
	}

	private void init(Component parent) {	
		initComponents();

		setLocationRelativeTo(parent);
	}

	public String getTargetPath() {
		return workspaceDirField.getText();
	}

	public IDEWorkspace getResult() {
		return result;
	}

	@SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        separator = new javax.swing.JSeparator();
        headingLabel = new javax.swing.JLabel();
        btnCreateWorkspace = new javax.swing.JButton();
        workspaceDirLabel = new javax.swing.JLabel();
        workspaceDirField = new javax.swing.JTextField();
        btnBrowse = new javax.swing.JButton();
        btnCancel = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        headingLabel.setFont(new java.awt.Font("Consolas", 0, 24)); // NOI18N
        headingLabel.setForeground(new java.awt.Color(51, 51, 51));
        headingLabel.setText("Create a Workspace");

        btnCreateWorkspace.setText("Create workspace");
        btnCreateWorkspace.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCreateWorkspaceActionPerformed(evt);
            }
        });

        workspaceDirLabel.setText("Workspace directory");

        btnBrowse.setText("Browse");
        btnBrowse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBrowseActionPerformed(evt);
            }
        });

        btnCancel.setText("Cancel");
        btnCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(separator)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(headingLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 322, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(workspaceDirLabel))
                        .addGap(0, 145, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(btnCancel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnCreateWorkspace))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(workspaceDirField)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnBrowse)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(headingLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(separator, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(workspaceDirLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(workspaceDirField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnBrowse))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 56, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnCreateWorkspace)
                    .addComponent(btnCancel))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnBrowseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBrowseActionPerformed
		String currentPath = getTargetPath();
		File initDir = null;
		if (currentPath != null) {
			initDir = new File(currentPath);
			if (!initDir.exists() || !initDir.isDirectory()) {
				initDir = null;
			}
		}

		File target = CMFileDialog.openDirectoryDialog(initDir);

		if (target != null) {
			workspaceDirField.setText(target.getAbsolutePath());
		}
    }//GEN-LAST:event_btnBrowseActionPerformed

    private void btnCreateWorkspaceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCreateWorkspaceActionPerformed
		String target = getTargetPath();
		if (target == null || target.isEmpty()) {
			DialogUtils.showErrorMessage(this, "Invalid path", "The workspace path is empty");
		} else {
			File targetFile = new File(target);
			if (!targetFile.exists()) {
				boolean createNew = DialogUtils.showYesNoDialog(this, "Directory not found", "The selected directory does not exist. Do you want to create it?");
				if (createNew) {
					targetFile.mkdirs();
				} else {
					return;
				}
			}
			if (!targetFile.exists()) {
				DialogUtils.showErrorMessage(this, "Directory not found", "Could not create directory. (Unknown error)");
			} else {
				if (!targetFile.isDirectory()) {
					DialogUtils.showErrorMessage(this, "Not a directory", "The selected file is not a directory.");
				} else {
					File dotIde = new File(targetFile + "/" + IDEWorkspace.IDE_DIR_FILE_NAME);
					if (dotIde.exists() && !dotIde.isDirectory()) {
						DialogUtils.showErrorMessage(this, "Bad file structure", "The selected directory contains a reserved \".ide\" file that is not a directory.");
					} else {
						if (dotIde.exists()){
							DialogUtils.showInfoMessage(this, "Existing workspace found", "A workspace has already been initialized in this directory. It will be opened instead.");
						}
						result = new IDEWorkspace(targetFile);

						dispose();
					}
				}
			}
		}
    }//GEN-LAST:event_btnCreateWorkspaceActionPerformed

    private void btnCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelActionPerformed
		dispose();
    }//GEN-LAST:event_btnCancelActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBrowse;
    private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnCreateWorkspace;
    private javax.swing.JLabel headingLabel;
    private javax.swing.JSeparator separator;
    private javax.swing.JTextField workspaceDirField;
    private javax.swing.JLabel workspaceDirLabel;
    // End of variables declaration//GEN-END:variables
}
