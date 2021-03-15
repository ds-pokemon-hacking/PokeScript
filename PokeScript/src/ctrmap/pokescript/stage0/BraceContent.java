package ctrmap.pokescript.stage0;

public class BraceContent {
	public boolean hasIntegrity = false;
	public int endIndex;
	public String content;
	
	public String getContentInBraces(){
		return content.substring(1, content.length() - 1);
	}
}
