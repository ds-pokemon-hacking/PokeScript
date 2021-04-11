
package ctrmap.pokescript.instructions.ntr;

public class NTRInstructionLink {
	private NTRInstructionCall source;
	public int argIdx;
	public NTRInstructionCall target;
	
	public NTRInstructionLink(NTRInstructionCall ins, NTRInstructionCall target, int argIdx){
		this.source = ins;
		this.target = target;
		this.argIdx = argIdx;
	}
	
	public void updateSourceArg(){
		if (target == null || source == null){
			return;
		}
		source.args[argIdx] = target.pointer - (source.pointer + source.getSize());
	}
}
