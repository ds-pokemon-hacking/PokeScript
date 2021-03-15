package ctrmap.pokescript.ide.autocomplete;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class AutoCompleteKeyListener implements KeyListener {

	private AutoComplete ac;

	public AutoCompleteKeyListener(AutoComplete ac) {
		this.ac = ac;
	}

	@Override
	public void keyTyped(KeyEvent e) {

	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (ac.isVisible() || ac.isHandlingResult()) {
			if (processKeyCode(e.getKeyCode())){
				e.consume();
			}
		}
	}
	
	private boolean processKeyCode(int keyCode){
		switch (keyCode){
			case KeyEvent.VK_DOWN:
				ac.getMainWindow().incrementSelection();
				return true;
			case KeyEvent.VK_UP:
				ac.getMainWindow().decrementSelection();
				return true;
			case KeyEvent.VK_ENTER:
				return ac.processEnterKeyAction();
			case KeyEvent.VK_PERIOD:
			case KeyEvent.VK_SEMICOLON:
				if (ac.isVisible()){
					return ac.processEnterKeyAction();
				}
				break;
		}
		return false;
	}

	@Override
	public void keyReleased(KeyEvent e) {

	}
}
