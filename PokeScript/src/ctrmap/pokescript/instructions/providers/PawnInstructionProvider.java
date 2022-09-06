
package ctrmap.pokescript.instructions.providers;
/**
 *
 */
public class PawnInstructionProvider implements AInstructionProvider {
	
	public static final MachineInfo PAWN_MACHINE_INFO = new MachineInfo() {
		@Override
		public boolean getAllowsGotoStatement() {
			return false;
		}
	};

	@Override
	public MetaFunctionHandler getMetaFuncHandler(String handlerName) {
		return null;
	}

	@Override
	public MachineInfo getMachineInfo() {
		return PAWN_MACHINE_INFO;
	}

}
