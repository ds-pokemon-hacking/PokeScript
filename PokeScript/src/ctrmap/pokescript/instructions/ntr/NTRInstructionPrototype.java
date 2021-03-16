
package ctrmap.pokescript.instructions.ntr;

public class NTRInstructionPrototype {
	public int opCode = -1;
	
	public String debugName;
	public NTRArgument[] parameters;
	
	public NTRInstructionPrototype(int opCode, NTRArgument... args){
		this.opCode = opCode;
		parameters = args;
	}
	
	public int getSize(){
		int size = 2;
		for (NTRArgument arg : parameters){
			size += arg.dataType.sizeof;
		}
		return size;
	}
}
