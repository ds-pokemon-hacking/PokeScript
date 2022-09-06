package ctrmap.pokescript.instructions.ntr;

import ctrmap.pokescript.instructions.abstractcommands.ACompiledInstruction;
import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.instructions.gen5.VConstants;
import ctrmap.pokescript.instructions.gen5.VOpCode;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.scriptformats.gen5.VScriptFile;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NTRInstructionCall {
	
	public int pointer;

	public NTRInstructionPrototype definition;

	public int[] args;

	public NTRInstructionLink link;

	public NTRInstructionCall(NTRInstructionPrototype definition, int... arguments) {
		this.definition = definition;
		int argSrcReloc = 0;
		args = new int[definition.parameters.length];
		for (int i = 0; i < definition.parameters.length; i++) {
			if (definition.parameters[i].returnCallBackIndex >= 0) {
				args[i] = VConstants.GP_REG_PRI + this.definition.parameters[i].returnCallBackIndex;
				argSrcReloc++;
			} else {
				int av = 0;
				int ai = i - argSrcReloc;
				if (ai < arguments.length) {
					av = arguments[ai];
				}
				args[i] = av;
			}
		}
	}

	protected NTRInstructionCall() {

	}

	public void setupLink(VScriptFile scr, NTRInstructionLinkSetup... linkableOpCodes) {
		for (int i = 0; i < linkableOpCodes.length; i++) {
			if (linkableOpCodes[i].opCode == definition.opCode) {
				int desiredArgNo = linkableOpCodes[i].argNo;
				if (desiredArgNo >= args.length) {
					throw new ArrayIndexOutOfBoundsException("Could not set up link for opcode " + definition.opCode + " - argument " + desiredArgNo + " out of range ! - " + definition.debugName);
				}
				link = new NTRInstructionLink(this, scr.getInstructionByPtr(pointer + getSize() + args[desiredArgNo]), linkableOpCodes[i].argNo);
				if (link.target != null) {
					//System.out.println("Linked from " + pointer + " to " + link.target.pointer + " - " + link.target);
				}
				break;
			}
		}
	}

	public int getSize() {
		if (definition.parameters.length != args.length) {
			throw new RuntimeException("Call size does not match definition!!");
		}
		return definition.getSize();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(definition.debugName == null ? definition.opCode : definition.debugName);
		sb.append("(");
		for (int ai = 0; ai < args.length; ai++) {
			if (ai != 0) {
				sb.append(", ");
			}
			if (definition.opCode == VOpCode.Call.ordinal()) {
				sb.append(Integer.toHexString(args[ai] + (pointer + getSize())));
			} else {
				sb.append(args[ai]);
			}
		}
		sb.append(");");
		return sb.toString();
	}

	public void write(DataOutput out) throws IOException {
		out.writeShort(definition.opCode);

		for (int ai = 0; ai < definition.parameters.length; ai++) {
			int argV = 0;
			if (ai < args.length) {
				argV = args[ai];
			}
			switch (definition.parameters[ai].dataType) {
				case FLEX:
				case U16:
				case VAR:
				case BOOL:
				case FX16:
					out.writeShort(argV);
					break;
				case FX32:
				case S32:
					out.writeInt(argV);
					break;
				case U8:
					out.write(argV);
					break;
				default:
					throw new RuntimeException("Unimplemented data type: " + definition.parameters[ai].dataType);
			}
		}
	}
}
