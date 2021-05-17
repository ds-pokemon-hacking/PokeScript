package ctrmap.scriptformats.gen5.disasm;

import ctrmap.pokescript.instructions.ntr.NTRInstructionCall;
import ctrmap.pokescript.instructions.ntr.NTRInstructionLink;
import ctrmap.pokescript.instructions.ntr.NTRInstructionPrototype;
import ctrmap.scriptformats.gen5.VCommandDataBase;
import java.util.ArrayList;
import java.util.List;

public class DisassembledCall extends NTRInstructionCall {
	public boolean doNotDisassemble = false;
	
	public VCommandDataBase.VCommand command;
	
	public String label;
	
	public List<NTRInstructionLink> ignoredLinks = new ArrayList<>();

	public DisassembledCall(int ptr, NTRInstructionPrototype definition, int... arguments) {
		this.definition = definition;
		this.args = arguments;
		pointer = ptr;
	}
	
	public void setupLinkToCall(DisassembledCall target){
		link = new NTRInstructionLink(this, target, args.length - 1);
	}
}
