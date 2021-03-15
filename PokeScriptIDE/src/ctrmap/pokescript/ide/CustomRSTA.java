package ctrmap.pokescript.ide;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

public class CustomRSTA extends RSyntaxTextArea{
	
	private List<CustomHighLight> customHLs = new ArrayList<>();
	
	public void clearCustomHighlights(){
		customHLs.clear();
	}
	
	public void addCustomHighlight(TextAreaMarkManager.Mark beginChar, TextAreaMarkManager.Mark endChar, Color color){
		customHLs.add(new CustomHighLight(beginChar, endChar, color));
	}
	
	public void addCustomHighlight(CustomHighLight hl){
		customHLs.add(hl);
	}
	
	public void addAllCustomHighlights(Collection<CustomHighLight> c){
		customHLs.addAll(c);
	}
	
	public void removeAllCustomHighlights(Collection<CustomHighLight> c){
		customHLs.removeAll(c);
	}
	
	public void removeCustomHighlight(CustomHighLight hl){
		customHLs.remove(hl);
	}
	
	@Override
	public void paintComponent(Graphics g){
		super.paintComponent(g);
		for (CustomHighLight hl : customHLs){
			try {
				Rectangle start = modelToView(hl.start.getPosition());
				Rectangle end = modelToView(hl.end.getPosition());
				g.setColor(hl.color);
				g.drawRect(start.x, start.y, end.x - start.x, start.height - 2);
			} catch (BadLocationException ex) {
				Logger.getLogger(CustomRSTA.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}
	
	public static class CustomHighLight{
		public final Color color;
		public final TextAreaMarkManager.Mark start;
		public final TextAreaMarkManager.Mark end;
		
		public CustomHighLight(TextAreaMarkManager.Mark beginChar, TextAreaMarkManager.Mark endChar, Color color){
			start = beginChar;
			end = endChar;
			this.color = color;
		}
	}
}
