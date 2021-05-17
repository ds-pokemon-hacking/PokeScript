package ctrmap.pokescript.ide.forms.settings.project.panes;

import ctrmap.pokescript.ide.PSIDE;
import ctrmap.pokescript.ide.forms.settings.project.ProjectSettingsPane;
import ctrmap.pokescript.ide.system.project.IDEProject;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultListModel;

public class ProjectDefinitionsPane extends ProjectSettingsPane {

	private DefaultListModel<String> defListModel = new DefaultListModel<>();
	
	public ProjectDefinitionsPane(PSIDE ide, IDEProject proj) {
		super(ide, proj);
		initComponents();
		
		defList.setModel(defListModel);
		
		for (String def : proj.getManifest().getCompilerDefinitions()){
			defListModel.addElement(def);
		}
	}	
	
	@SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        defListSP = new javax.swing.JScrollPane();
        defList = new javax.swing.JList<>();
        defField = new javax.swing.JTextField();
        btnAddDef = new javax.swing.JButton();
        btnRemoveDef = new javax.swing.JButton();

        defList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        defListSP.setViewportView(defList);

        btnAddDef.setText("Add");
        btnAddDef.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddDefActionPerformed(evt);
            }
        });

        btnRemoveDef.setText("Remove");
        btnRemoveDef.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRemoveDefActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(defListSP, javax.swing.GroupLayout.DEFAULT_SIZE, 350, Short.MAX_VALUE)
                    .addComponent(defField)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(btnAddDef)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnRemoveDef)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(defListSP)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(defField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnAddDef)
                    .addComponent(btnRemoveDef))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btnAddDefActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddDefActionPerformed
		int idx = defListModel.getSize();
		defListModel.addElement("EMPTY");
		defList.setSelectedIndex(idx);
		defList.ensureIndexIsVisible(idx);
    }//GEN-LAST:event_btnAddDefActionPerformed

    private void btnRemoveDefActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRemoveDefActionPerformed
		int idx = defList.getSelectedIndex();
		defListModel.removeElementAt(idx);
    }//GEN-LAST:event_btnRemoveDefActionPerformed

	@Override
	public String getMenuName() {
		return "Definitions";
	}


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAddDef;
    private javax.swing.JButton btnRemoveDef;
    private javax.swing.JTextField defField;
    private javax.swing.JList<String> defList;
    private javax.swing.JScrollPane defListSP;
    // End of variables declaration//GEN-END:variables

	@Override
	public void save() {
		List<String> defs = new ArrayList<>();
		for (int i = 0; i < defListModel.getSize(); i++){
			String def = defListModel.elementAt(i);
			if (def != null && !def.isEmpty() && !defs.contains(def)){
				defs.add(def);
			}
		}
		project.getManifest().setCompilerDefinitions(defs);
	}
}
