
package ctrmap.pokescript.instructions.ntr;

import ctrmap.pokescript.instructions.abstractcommands.ACompiledInstruction;
import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.instructions.gen5.VConstants;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.scriptformats.gen5.VScriptFile;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NTRInstructionCall extends ACompiledInstruction {
	public NTRInstructionPrototype definition;
	
	public int[] args;
	
	public NTRInstructionLink link;
	
	public NTRInstructionCall(NTRInstructionPrototype definition, int... arguments){
		this.definition = definition;
		int argSrcReloc = 0;
		args = new int[definition.parameters.length];
		for (int i = 0; i < definition.parameters.length; i++){
			if (definition.parameters[i].returnCallBackIndex >= 0){
				args[i] = VConstants.GP_REG_PRI + this.definition.parameters[i].returnCallBackIndex;
				argSrcReloc++;
			}
			else {
				int av = 0;
				int ai = i - argSrcReloc;
				if (ai < arguments.length){
					av = arguments[ai];
				}
				args[i] = av;
			}
		}
	}
	
	public void setupLink(VScriptFile scr, NTRInstructionLinkSetup... linkableOpCodes){
		for (int i = 0; i < linkableOpCodes.length; i++){
			if (linkableOpCodes[i].opCode == definition.opCode){
				link = new NTRInstructionLink(this, scr.getInstructionByPtr(args[linkableOpCodes[i].argNo]), linkableOpCodes[i].argNo);
				break;
			}
		}
	}

	@Override
	public int getSize() {
		return definition.getSize();
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(definition.debugName == null ? definition.opCode : definition.debugName);
		sb.append("(");
		for (int ai = 0; ai < args.length; ai++){
			if (ai != 0){
				sb.append(", ");
			}
			sb.append(args[ai]);
		}
		sb.append(");");
		return sb.toString();
	}
	
	public void write(DataOutput out) throws IOException {
		out.writeShort(definition.opCode);

		for (int ai = 0; ai < definition.parameters.length; ai++){
			int argV = 0;
			if (ai < args.length){
				argV = args[ai];
			}
			switch (definition.parameters[ai].dataType){
				case FLEX:
				case U16:
				case VAR:
					out.writeShort(argV);
					break;
				case S32:
					out.writeInt(argV);
					break;
				case U8:
					out.write(argV);
					break;
			}
		}
	}
	
	public static List<NTRInstructionCall> compileIL(List<AInstruction> il, NCompileGraph g){
		List<NTRInstructionCall> r = new ArrayList<>();
		for (AInstruction i : il){
			List<? extends ACompiledInstruction> compiled = i.compile(g);
			for (ACompiledInstruction ci : compiled){
				if (ci instanceof NTRInstructionCall){
					r.add((NTRInstructionCall)ci);
				}
				else {
					throw new UnsupportedOperationException("Attempted to compile a non-NTR instruction in a NTR context.");
				}
			}
		}
		return r;
	}
}
