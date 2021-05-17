package ctrmap.pokescript.ide.system.project.tree;

import ctrmap.pokescript.ide.system.project.tree.nodes.IDENodeBase;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

public class IDEProjectTreePopupMenu extends JPopupMenu{
		
	private IDENodeBase node;
	
	public IDEProjectTreePopupMenu(IDENodeBase n){
		node = n;
		
		ActionListener al = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				node.callNodeAction(e.getActionCommand());
				setVisible(false);
			}
		};
		
		for (String action : node.getNodeActions()){
			JMenuItem itm = new JMenuItem(action);
			itm.setRolloverEnabled(true);
			itm.setEnabled(true);
			itm.addActionListener(al);
			add(itm);
		}
	}
	
	public boolean makesSense(){
		return node != null && getComponentCount() > 0;
	}
}
