package ctrmap.pokescript.stage1;

public class PendingLabel {
	public String shortName;
	public CompileBlock parentBlock;
	public boolean isUserLabel;
	
	public PendingLabel(String name, CompileBlock parentBlock){
		shortName = name;
		this.parentBlock = parentBlock;
	}
	
	public String getFullLabel(){
		if (parentBlock == null){
			return shortName;
		}
		return parentBlock.fullBlockName + "_" + shortName;
	}
	
	@Override
	public String toString(){
		if (parentBlock == null){
			return shortName;
		}
		return shortName + "@" + parentBlock.fullBlockName;
	}
}
