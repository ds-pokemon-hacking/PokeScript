
package ctrmap.pokescript.instructions.providers;

import ctrmap.pokescript.instructions.gen4.PokeScriptToIV;
import ctrmap.pokescript.OutboundDefinition;
import ctrmap.pokescript.instructions.abstractcommands.AAccessGlobal;
import ctrmap.pokescript.instructions.abstractcommands.ACaseTable;
import ctrmap.pokescript.instructions.abstractcommands.AConditionJump;
import ctrmap.pokescript.instructions.abstractcommands.ALocalCall;
import ctrmap.pokescript.instructions.abstractcommands.ANativeCall;
import ctrmap.pokescript.instructions.abstractcommands.APlainInstruction;
import ctrmap.pokescript.instructions.abstractcommands.APlainOpCode;
import ctrmap.pokescript.instructions.providers.floatlib.IFloatHandler;

/**
 *
 */
public class IVInstructionProvider implements AInstructionProvider {

	public MemoryInfo IVMemoryInfo = new MemoryInfo() {
		@Override
		public int getStackIndexingStep() {
			return 1;
		}

		@Override
		public int getFuncArgOffset() {
			return 1;
		}

		@Override
		public boolean isArgsUnderStackFrame() {
			return true;
		}

		@Override
		public int getGlobalsIndexingStep() {
			return 1;
		}

		@Override
		public boolean isStackOrderNatural() {
			return false;
		}
	};
	
	@Override
	public APlainInstruction getPlainInstruction(APlainOpCode opCode, int[] args) {
		return PokeScriptToIV.getPlainNTRForOpCode(opCode, args);
	}

	@Override
	public AConditionJump getConditionJump(APlainOpCode jump, String targetLabel) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public ACaseTable getCaseTable() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public ALocalCall getMethodCall(OutboundDefinition out) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public ANativeCall getNativeCall(OutboundDefinition out) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public AAccessGlobal getGlobalRead(String globalName) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public AAccessGlobal getGlobalWrite(String globalName) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public MemoryInfo getMemoryInfo() {
		return IVMemoryInfo;
	}

	@Override
	public MetaFunctionHandler getMetaFuncHandler(String handlerName) {
		return null;
	}

	@Override
	public IFloatHandler getFloatingPointHandler() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

}
