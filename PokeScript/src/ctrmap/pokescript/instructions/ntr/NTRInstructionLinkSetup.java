
package ctrmap.pokescript.instructions.ntr;

/**
 *
 */
public class NTRInstructionLinkSetup {
	public final int opCode;
	public final int argNo;
	
	public NTRInstructionLinkSetup(Enum opCode, int argNo){
		this.opCode = opCode.ordinal();
		this.argNo = argNo;
	}
	
	public NTRInstructionLinkSetup(int opCode, int argNo){
		this.opCode = opCode;
		this.argNo = argNo;
	}
}
