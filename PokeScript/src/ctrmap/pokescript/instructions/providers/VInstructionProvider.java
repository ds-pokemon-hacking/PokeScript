package ctrmap.pokescript.instructions.providers;

import ctrmap.pokescript.instructions.gen5.metahandlers.VExternFuncHandler;
import ctrmap.pokescript.instructions.gen5.metahandlers.VActionSeqHandler;
import xstandard.util.ParsingUtils;

public class VInstructionProvider implements AInstructionProvider {

	public static final MachineInfo V_MACHINE_INFO = new MachineInfo() {
		@Override
		public boolean getAllowsGotoStatement() {
			return true;
		}
	};

	@Override
	public MetaFunctionHandler getMetaFuncHandler(String handlerName) {
		switch (handlerName) {
			case "VActionSequence":
				return new VActionSeqHandler();
		}
		if (handlerName.startsWith("VGlobalCall")) {
			int idxStart = handlerName.indexOf('[');
			int idxEnd = handlerName.lastIndexOf(']');
			if (idxStart >= 0 && idxEnd >= 0) {
				String sourceText = handlerName.substring(idxStart + 1, idxEnd);
				int scrid = ParsingUtils.parseBasedIntOrDefault(sourceText, -1);
				if (scrid != -1) {
					return new VExternFuncHandler(scrid, handlerName.startsWith("VGlobalCallAsync"));
				}
			}
		}
		return null;
	}

	@Override
	public MachineInfo getMachineInfo() {
		return V_MACHINE_INFO;
	}

}
