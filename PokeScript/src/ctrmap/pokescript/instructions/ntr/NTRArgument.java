
package ctrmap.pokescript.instructions.ntr;

public class NTRArgument {
	public NTRDataType dataType;
	public int returnCallBackIndex;
	
	public NTRArgument(NTRDataType type){
		this(type, -1);
	}
	
	public NTRArgument(NTRDataType type, int returnCallBackIndex){
		dataType = type;
		this.returnCallBackIndex = returnCallBackIndex;
	}
	
	public boolean isReturnCallback(){
		return returnCallBackIndex > -1;
	}
}
