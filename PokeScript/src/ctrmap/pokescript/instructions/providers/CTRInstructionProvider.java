
package ctrmap.pokescript.instructions.providers;

import ctrmap.pokescript.OutboundDefinition;
import ctrmap.pokescript.instructions.ctr.instructions.PAccessGlobal;
import ctrmap.pokescript.instructions.ctr.instructions.PConditionJump;
import ctrmap.pokescript.instructions.ctr.instructions.PLocalCall;
import ctrmap.pokescript.instructions.ctr.instructions.PNativeCall;
import ctrmap.pokescript.instructions.abstractcommands.AAccessGlobal;
import ctrmap.pokescript.instructions.abstractcommands.ACaseTable;
import ctrmap.pokescript.instructions.abstractcommands.AConditionJump;
import ctrmap.pokescript.instructions.abstractcommands.ALocalCall;
import ctrmap.pokescript.instructions.abstractcommands.ANativeCall;
import ctrmap.pokescript.instructions.abstractcommands.APlainInstruction;
import ctrmap.pokescript.instructions.abstractcommands.APlainOpCode;
import ctrmap.pokescript.instructions.ctr.PawnPlainInstruction;
import ctrmap.pokescript.instructions.ctr.instructions.PCaseTable;

/**
 *
 */
public class CTRInstructionProvider implements AInstructionProvider {

	public MemoryInfo CTRMemoryInfo = new MemoryInfo() {
		@Override
		public int getStackIndexingStep() {
			return -4;
		}
		
		@Override
		public int getGlobalsIndexingStep() {
			return 4;
		}

		@Override
		public int getFuncArgOffset() {
			return 3;
		}

		@Override
		public boolean isArgsUnderStackFrame() {
			return true;
		}	

		@Override
		public boolean isStackOrderNatural() {
			return true;
		}
	};
	
	@Override
	public APlainInstruction getPlainInstruction(APlainOpCode opCode, int[] args) {
		return new PawnPlainInstruction(opCode, args);
	}

	@Override
	public AConditionJump getConditionJump(APlainOpCode jump, String targetLabel) {
		return new PConditionJump(jump, targetLabel);
	}

	@Override
	public ALocalCall getMethodCall(OutboundDefinition out) {
		return new PLocalCall(out);
	}

	@Override
	public ANativeCall getNativeCall(OutboundDefinition out) {
		return new PNativeCall(out);
	}

	@Override
	public AAccessGlobal getGlobalRead(String globalName) {
		return new PAccessGlobal.Read(globalName);
	}

	@Override
	public AAccessGlobal getGlobalWrite(String globalName) {
		return new PAccessGlobal.Write(globalName);
	}

	@Override
	public ACaseTable getCaseTable() {
		return new PCaseTable();
	}

	@Override
	public MemoryInfo getMemoryInfo() {
		return CTRMemoryInfo;
	}

	@Override
	public MetaFunctionHandler getMetaFuncHandler(String handlerName) {
		return null;
	}

}
