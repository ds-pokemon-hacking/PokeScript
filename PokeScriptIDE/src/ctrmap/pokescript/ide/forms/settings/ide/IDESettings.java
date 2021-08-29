package ctrmap.pokescript.ide.forms.settings.ide;

import ctrmap.pokescript.ide.PSIDE;
import ctrmap.stdlib.gui.components.tree.CustomJTreeNode;
import ctrmap.stdlib.gui.components.tree.CustomJTreeRootNode;
import ctrmap.stdlib.gui.components.tree.CustomJTreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;

public class IDESettings extends javax.swing.JFrame {

	private PSIDE ide;
	
	public IDESettings(PSIDE ide) {
		initComponents();
		this.ide = ide;
		
		initTreeMenu();

		settingsMenuTree.addListener(new CustomJTreeSelectionListener() {
			@Override
			public void onNodeSelected(CustomJTreeNode node) {
				if (node != null){
					IDESettingsNode n = (IDESettingsNode)node;
					settingsSubPanel.setViewportView(n.pane);
				}
			}
		});
	}
	
	private void initTreeMenu(){
		CustomJTreeRootNode root = settingsMenuTree.getRootNode();
		
		CustomJTreeNode compilerNode = addSettingsPane(root, "Compiler");
		//todo rest of the panes
		
		((DefaultTreeModel)settingsMenuTree.getModel()).reload();
	}
	
	private IDESettingsNode addSettingsPane(CustomJTreeRootNode parent, IDESettingsPane pane){
		return addSettingsPane(parent, null, pane);
	}
	
	private IDESettingsNode addSettingsPane(CustomJTreeRootNode parent, String name){
		return addSettingsPane(parent, name, null);
	}
	
	private IDESettingsNode addSettingsPane(CustomJTreeRootNode parent, String overrideName, IDESettingsPane pane){
		IDESettingsNode node = new IDESettingsNode(overrideName, pane);
		parent.add(node);
		return node;
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        settingsSubPanel = new javax.swing.JScrollPane();
        treeSP = new javax.swing.JScrollPane();
        settingsMenuTree = new ctrmap.stdlib.gui.components.tree.CustomJTree();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("PokéScript IDE Configuration");

        treeSP.setViewportView(settingsMenuTree);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(treeSP, javax.swing.GroupLayout.PREFERRED_SIZE, 181, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(settingsSubPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 374, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(treeSP, javax.swing.GroupLayout.PREFERRED_SIZE, 275, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(settingsSubPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 278, Short.MAX_VALUE))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private ctrmap.stdlib.gui.components.tree.CustomJTree settingsMenuTree;
    private javax.swing.JScrollPane settingsSubPanel;
    private javax.swing.JScrollPane treeSP;
    // End of variables declaration//GEN-END:variables
}