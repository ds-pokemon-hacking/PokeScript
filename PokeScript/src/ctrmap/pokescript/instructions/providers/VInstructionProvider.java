
package ctrmap.pokescript.instructions.providers;

import ctrmap.pokescript.OutboundDefinition;
import ctrmap.pokescript.instructions.abstractcommands.AAccessGlobal;
import ctrmap.pokescript.instructions.abstractcommands.ACaseTable;
import ctrmap.pokescript.instructions.abstractcommands.AConditionJump;
import ctrmap.pokescript.instructions.abstractcommands.ALocalCall;
import ctrmap.pokescript.instructions.abstractcommands.ANativeCall;
import ctrmap.pokescript.instructions.abstractcommands.APlainInstruction;
import ctrmap.pokescript.instructions.abstractcommands.APlainOpCode;
import ctrmap.pokescript.instructions.gen5.PokeScriptToV;
import ctrmap.pokescript.instructions.gen5.instructions.VAccessGlobal;
import ctrmap.pokescript.instructions.gen5.instructions.VCaseTable;
import ctrmap.pokescript.instructions.gen5.instructions.VConditionJump;
import ctrmap.pokescript.instructions.gen5.instructions.VLocalCall;
import ctrmap.pokescript.instructions.gen5.instructions.VNativeCall;
import ctrmap.pokescript.instructions.gen5.metahandlers.VMovementFuncHandler;
import ctrmap.pokescript.instructions.providers.floatlib.FXFloatHandler;
import ctrmap.pokescript.instructions.providers.floatlib.IFloatHandler;

public class VInstructionProvider implements AInstructionProvider {

	public static final MemoryInfo V_MEMORY_INFO = new MemoryInfo() {
		@Override
		public int getStackIndexingStep() {
			return 1;
		}

		@Override
		public int getFuncArgOffset() {
			return 0;
		}

		@Override
		public boolean isArgsUnderStackFrame() {
			return false;
		}

		@Override
		public int getGlobalsIndexingStep() {
			return -1;
		}
		
		@Override
		public boolean isStackOrderNatural() {
			return false;
		}
	};
	
	public static final MachineInfo V_MACHINE_INFO = new MachineInfo() {
		@Override
		public boolean getAllowsGotoStatement() {
			return true;
		}
	};
	
	@Override
	public APlainInstruction getPlainInstruction(APlainOpCode opCode, int[] args) {
		return PokeScriptToV.getPlainNTRForOpCode(opCode, args);
	}

	@Override
	public AConditionJump getConditionJump(APlainOpCode jump, String targetLabel) {
		return new VConditionJump(jump, targetLabel);
	}

	@Override
	public ACaseTable getCaseTable() {
		return new VCaseTable();
	}

	@Override
	public ALocalCall getMethodCall(OutboundDefinition out) {
		return new VLocalCall(out);
	}

	@Override
	public ANativeCall getNativeCall(OutboundDefinition out) {
		return new VNativeCall(out);
	}

	@Override
	public AAccessGlobal getGlobalRead(String globalName) {
		return new VAccessGlobal.Read(globalName);
	}

	@Override
	public AAccessGlobal getGlobalWrite(String globalName) {
		return new VAccessGlobal.Write(globalName);
	}

	@Override
	public MemoryInfo getMemoryInfo() {
		return V_MEMORY_INFO;
	}

	@Override
	public MetaFunctionHandler getMetaFuncHandler(String handlerName) {
		switch (handlerName){
			case "VMovementFunc":
				return new VMovementFuncHandler();
		}
		return null;
	}

	@Override
	public IFloatHandler getFloatingPointHandler() {
		return FXFloatHandler.INSTANCE;
	}

	@Override
	public MachineInfo getMachineInfo() {
		return V_MACHINE_INFO;
	}

}
