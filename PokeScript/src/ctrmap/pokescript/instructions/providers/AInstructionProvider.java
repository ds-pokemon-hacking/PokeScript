
package ctrmap.pokescript.instructions.providers;

import ctrmap.pokescript.OutboundDefinition;
import ctrmap.pokescript.instructions.abstractcommands.AAccessGlobal;
import ctrmap.pokescript.instructions.abstractcommands.ACaseTable;
import ctrmap.pokescript.instructions.abstractcommands.AConditionJump;
import ctrmap.pokescript.instructions.abstractcommands.ALocalCall;
import ctrmap.pokescript.instructions.abstractcommands.ANativeCall;
import ctrmap.pokescript.instructions.abstractcommands.APlainInstruction;
import ctrmap.pokescript.instructions.abstractcommands.APlainOpCode;

public interface AInstructionProvider {	
	public abstract APlainInstruction getPlainInstruction(APlainOpCode opCode, int[] args);
	
	public abstract AConditionJump getConditionJump(APlainOpCode jump, String targetLabel);
	public abstract ACaseTable getCaseTable();
	public abstract ALocalCall getMethodCall(OutboundDefinition out);
	public abstract ANativeCall getNativeCall(OutboundDefinition out);
	public abstract AAccessGlobal getGlobalRead(String globalName);
	public abstract AAccessGlobal getGlobalWrite(String globalName);
	
	public abstract MemoryInfo getMemoryInfo();
}
