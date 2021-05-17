package ctrmap.pokescript.ide.autocomplete.nodes;

import ctrmap.pokescript.ide.FileEditorRSTA;
import ctrmap.pokescript.ide.TextAreaMarkManager;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class NodeResult {
	private String data;
	private List<Link> links = new ArrayList<>();
	
	public String getText(){
		return data;
	}
	
	public void setText(String data){
		this.data = data;
	}
	
	public void putLink(Link link){
		links.add(link);
	}
	
	public Handler createHandler(){
		return new Handler(this);
	}
	
	public void freezeLinks(){
		for (Link l : links){
			l.getOffsetMark().freeze();
		}
	}
	
	public void defrostLinks(){
		for (Link l : links){
			l.getOffsetMark().defrost();
		}
	}
	
	public static class Link{
		private Object source;
		private String text;
		private TextAreaMarkManager.Mark offset;
		private TextAreaMarkManager.Mark last;
		
		public Link(Object source, String text, int offset, TextAreaMarkManager marks){
			this.source = source;
			this.text = text;
			this.offset = marks.createSensitiveMark(offset, offset + text.length());
			this.last = this.offset.getLast();
		}
		
		public TextAreaMarkManager.Mark getOffsetMark(){
			return offset;
		}
		
		public void setFocused(boolean v){
			if (v){
				offset.focus();
			}
			else {
				offset.loseFocus();
			}
		}
		
		public int getOffset(){
			return offset.getPosition();
		}
		
		public int getLength(){
			return text.length();
		}
		
		public Object getSource(){
			return source;
		}
		
		private FileEditorRSTA.CustomHighLight highLight = null;
		
		public FileEditorRSTA.CustomHighLight getHighLight(){
			if (highLight == null){
				highLight = new FileEditorRSTA.CustomHighLight(offset, last, Color.RED);
			}
			return highLight;
		}
	}
	
	public static class Handler{
		private NodeResult nr;
		private int currentLink = -1;
		public int tpCommonLength = -1;
		
		public Handler(NodeResult nr){
			this.nr = nr;
		}
		
		public boolean hasLinks(){
			return !nr.links.isEmpty();
		}
		
		public NodeResult getNR(){
			return nr;
		}
		
		public String getFullTextMinusCommonLength(){
			return nr.data.substring(tpCommonLength);
		}
		
		public boolean isLinkChainBroken(){
			for (Link l : nr.links){
				if (!l.getOffsetMark().getIsValid()){
					return true;
				}
			}
			return false;
		}
		
		public Link nextLink(){
			setCurrentLinkFocus(false);
			currentLink++;
			if (currentLink >= nr.links.size()){
				currentLink = -1;
			}
			setCurrentLinkFocus(true);
			return getCurrentLink();
		}
		
		private void setCurrentLinkFocus(boolean v){
			Link l = getCurrentLink();
			if (l != null){
				l.setFocused(v);
			}
		}
		
		public Link getCurrentLink(){
			if (currentLink < 0 || currentLink >= nr.links.size()){
				return null;
			}
			return nr.links.get(currentLink);
		}
		
		public List<FileEditorRSTA.CustomHighLight> getHighLights(){
			List<FileEditorRSTA.CustomHighLight> l = new ArrayList<>();
			for (Link link : nr.links){
				l.add(link.getHighLight());
			}
			return l;
		}
		
		public void transpose(String context, int contextPosition){
			int commonLength = 0;
			String positionedContext = context.substring(0, contextPosition).toLowerCase();
			String nrDataLC = nr.data.toLowerCase();
			for (int cl = 0; cl <= nr.data.length(); cl++){
				if (positionedContext.endsWith(nrDataLC.substring(0, cl))){
					commonLength = cl;
				}
			}
			
			tpCommonLength = commonLength;
			int actualStart = contextPosition - commonLength;
			
			for (Link l : nr.links){
				l.offset.forceTranspose(actualStart);
			}
		}
	}
}
