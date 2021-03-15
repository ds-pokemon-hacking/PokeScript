
package ctrmap.pokescript.instructions.ntr;

public class NTRArgument {
	public NTRDataType dataType;
	public boolean isReturnCallBack;
	
	public NTRArgument(NTRDataType type){
		this.dataType = type;
	}
	
	public NTRArgument(NTRDataType type, boolean isReturnCallBack){
		this(type);
		this.isReturnCallBack = isReturnCallBack;
	}
}
