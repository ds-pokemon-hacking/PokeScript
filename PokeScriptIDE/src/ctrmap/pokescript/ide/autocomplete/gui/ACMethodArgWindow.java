package ctrmap.pokescript.ide.autocomplete.gui;

import ctrmap.pokescript.ide.CustomRSTA;
import ctrmap.pokescript.ide.autocomplete.AutoComplete;

public class ACMethodArgWindow extends javax.swing.JWindow {

	private CustomRSTA area;

	public ACMethodArgWindow(CustomRSTA area) {
		this.area = area;
		initComponents();
	}

	public void updateFromCaret() {
		if (isVisible()) {
			String text = area.getText();
			int cp = area.getCaretPosition() - 1;
			
			int methodIntegrity = 0;
			for (int i = cp; i < text.length(); i++){
				char c = text.charAt(i);
				switch (c){
					case ';':
						break;
					case '(':
						methodIntegrity++;
						break;
					case ')':
						methodIntegrity--;
						break;
				}
			}
			//Now, if methodIntegrity==0, all methods opened from the caret till EOL have been closed, i.e. caret is not inside a method
			//>0 should not happen in compilable code
			//<0 means there is abs(methodIntegrity) methods opened before the caret
			
			if (methodIntegrity < 0){
				int methodLevel = 0;
				
				int attachOffset = -1;
				
				Loop:
				for (int i = cp - 1; i > 0; i--){
					switch (text.charAt(i)){
						case ')':
							methodLevel++;
							break;
						case '(':
							methodLevel--;
							break;
						case ';':
							break Loop;
					}
					if (methodLevel == methodIntegrity){
						attachOffset = i;
						break;
					}
				}
				//we are now at the method that the caret is inside of. This is also the attach offset for the window
				if (attachOffset != -1){
					String methodName = AutoComplete.doBackwardsScanUntilNonName(text, attachOffset);
					
				}
			}
			else {
				wClose();
			}
		}
	}

	public void wClose() {
		setVisible(false);
	}

	@SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();

        jLabel1.setText("type arg");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    // End of variables declaration//GEN-END:variables

}
