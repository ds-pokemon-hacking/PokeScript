
package ctrmap.pokescript.instructions.ntr;

public enum NTRDataType {
	U8(1),
	U16(2),
	S32(4),
	VAR(2),
	FLEX(2),
	
	FX16(2),
	FX32(4),
	
	//return types, virtual
	VOID(0),
	BOOL(2);
	
	public final int sizeof;
	
	private NTRDataType(int sizeof){
		this.sizeof = sizeof;
	}
}
