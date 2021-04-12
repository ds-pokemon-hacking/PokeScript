package ctrmap.scriptformats.gen5.disasm;

import ctrmap.pokescript.instructions.ntr.NTRArgument;
import ctrmap.pokescript.instructions.ntr.NTRDataType;
import ctrmap.pokescript.instructions.ntr.NTRInstructionCall;
import ctrmap.pokescript.instructions.ntr.NTRInstructionPrototype;
import ctrmap.scriptformats.gen5.VCommandDataBase;
import ctrmap.scriptformats.gen5.VScriptFile;
import ctrmap.stdlib.gui.FormattingUtils;
import ctrmap.stdlib.io.base.IOWrapper;
import ctrmap.stdlib.io.base.LittleEndianIO;
import ctrmap.stdlib.io.util.IndentedPrintStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VDisassembler {

	private VScriptFile scr;
	private VCommandDataBase cdb;

	private List<LinkPrototype> publics = new ArrayList<>();
	private List<LinkPrototype> jumpLinks = new ArrayList<>();
	private List<LinkPrototype> functionCalls = new ArrayList<>();
	private List<LinkPrototype> movementJumps = new ArrayList<>();

	public List<DisassembledMethod> methods = new ArrayList<>();
	public List<DisassembledMethod> movements = new ArrayList<>();

	public VDisassembler(VScriptFile scr, VCommandDataBase cdb) {
		this.scr = scr;
		this.cdb = cdb;
	}

	public void disassemble() {
		if (scr.getSourceFile() != null) {
			try {
				LittleEndianIO dis = scr.getSourceFile().getIO();

				readScriptHeader(dis);

				for (LinkPrototype p : publics) {
					readMethod(dis, p).isPublic = true;
				}

				dis.close();
			} catch (IOException ex) {
				Logger.getLogger(VScriptFile.class.getName()).log(Level.SEVERE, null, ex);
			}
		}

		createLabelsAndLinks();
	}

	public void commitToScript() {
		scr.instructions.clear();
		scr.publics.clear();
		for (DisassembledMethod m : methods) {
			if (m.isPublic) {
				scr.addPublic(m.instructions.get(0));
			}
			scr.instructions.addAll(m.instructions);
		}
		scr.updateLinks();
	}

	public boolean isJumpLabelUsed(int ptr){
		for (LinkPrototype j : jumpLinks){
			if (j.targetOffset == ptr){
				return true;
			}
		}
		return false;
	}
	
	private DisassembledMethod findMethodByPtr(int ptr) {
		for (DisassembledMethod m : methods) {
			if (m.ptr == ptr) {
				return m;
			}
		}
		return null;
	}

	private DisassembledMethod findMovementByPtr(int ptr) {
		for (DisassembledMethod m : movements) {
			if (m.ptr == ptr) {
				return m;
			}
		}
		return null;
	}

	private void createLabelsAndLinks() {
		System.out.println("--Re-linking main methods--");
		createLabelsImpl(publics, "main_", true);
		System.out.println("--Re-linking sub methods--");
		createLabelsImpl(functionCalls, "sub_", false);
		System.out.println("--Re-linking jumps--");
		createLabelsImpl(jumpLinks, "LABEL_", false);
		System.out.println("--Re-linking movement--");
		createLabelsImpl(movementJumps, "PlayMovement_", false);
	}

	private void createLabelsImpl(List<LinkPrototype> links, String prefix, boolean labelByIndex) {
		int index = 0;
		for (LinkPrototype jmp : links) {
			DisassembledCall jumpTarget = findCallOrMovementByPtr(jmp.targetOffset);
			DisassembledCall jumpSrc = findCallByPtr(jmp.sourceOffset);
			if (jumpTarget != null) {
				if (jumpSrc != null) {
					System.out.println("Setting up link from " + Integer.toHexString(jumpSrc.pointer) + " to " + prefix + Integer.toHexString(jumpTarget.pointer));
					jumpSrc.setupLinkToCall(jumpTarget);
				} else {
					System.out.println("SOURCE ABSENT from " + Integer.toHexString(jmp.sourceOffset) + " to " + jmp.targetOffset);
				}
				if (jumpTarget.label == null) {
					if (labelByIndex) {
						jumpTarget.label = prefix + index;
					} else {
						jumpTarget.label = prefix + FormattingUtils.getStrWithLeadingZeros(4, Integer.toHexString(jumpTarget.pointer));
					}
				}
			}
			else {
				System.out.println("Could not link!! from " + Integer.toHexString(jmp.sourceOffset) + " to " + Integer.toHexString(jmp.targetOffset));
			}
			index++;
		}
	}

	private DisassembledCall findCallByPtr(int ptr) {
		return findCall(methods, ptr);
	}
	
	private DisassembledCall findCallOrMovementByPtr(int ptr) {
		List<DisassembledMethod> l = new ArrayList<>(methods);
		l.addAll(movements);
		return findCall(l, ptr);
	}
	
	private static DisassembledCall findCall(List<DisassembledMethod> l, int ptr){
		for (DisassembledMethod m : l) {
			for (DisassembledCall c : m.instructions) {
				int diff = ptr - c.pointer;
				if (diff >= 0 && diff < c.getSize()) {
					return c;
				}
			}
		}
		return null;
	}

	private DisassembledMethod readMethod(IOWrapper dis, LinkPrototype lp) throws IOException {
		System.out.println("Reading method at " + Integer.toHexString(lp.targetOffset));
		int methodPtr = lp.targetOffset;
		dis.seek(methodPtr);
		int max = dis.length() - 2;

		DisassembledMethod m = new DisassembledMethod(methodPtr);

		FuncReader:
		while (dis.getPosition() < max) {
			int insPtr = dis.getPosition();

			int opCode = dis.readUnsignedShort();

			VCommandDataBase.VCommand c = cdb.getCommandProto(opCode);
			if (c == null) {
				System.err.println("WARN: Could not detect command 0x" + Integer.toHexString(opCode) + " at " + Integer.toHexString(insPtr));
				if (!m.instructions.isEmpty()){
					DisassembledCall lastCall = m.instructions.get(m.instructions.size() - 1);
					System.err.println("Last command: " + (lastCall.command != null ? lastCall.command.name : "[UNDETECTED]") + " at " + Integer.toHexString(lastCall.pointer) + "(size " + Integer.toHexString(lastCall.getSize()) + ")");
				}
				if ((opCode & 0xF000) != 0) {
					System.err.println("Suspicious command. Aligning stream.");
					dis.seek(dis.getPosition() - 1);
				}
			} else {
				int[] arguments = new int[c.def.parameters.length];

				for (int i = 0; i < arguments.length; i++) {
					int value = 0;

					switch (c.def.parameters[i].dataType) {
						case FLEX:
						case U16:
						case VAR:
							value = dis.readUnsignedShort();
							break;
						case FX16:
							value = dis.readShort();
							break;
						case FX32:
						case S32:
							value = dis.readInt();
							break;
						case U8:
							value = dis.readUnsignedByte();
							break;
					}

					arguments[i] = value;
				}

				DisassembledCall call = new DisassembledCall(insPtr, c.def, arguments);
				call.command = c;

				m.instructions.add(call);

				switch (c.type) {
					case HALT:
					case RETURN:
						break FuncReader;
					case JUMP:
						jumpLinks.add(new LinkPrototype(dis, call.args[call.args.length - 1]));
						break;
					case MOVEMENT_JUMP:
						movementJumps.add(new LinkPrototype(dis, call.args[call.args.length - 1]));
						break;
					case CALL:
						functionCalls.add(new LinkPrototype(dis, call.args[call.args.length - 1]));
						break;
				}
			}
		}

		methods.add(m);

		for (LinkPrototype movementCall : movementJumps) {
			if (findMovementByPtr(movementCall.targetOffset) == null) {
				readMovement(dis, movementCall);
			}
		}

		List<LinkPrototype> currentFuncCalls = new ArrayList<>(functionCalls);
		for (LinkPrototype funcCall : currentFuncCalls) {
			if (findMethodByPtr(funcCall.targetOffset) == null) {
				readMethod(dis, funcCall);
			}
		}
		return m;
	}

	private void readMovement(IOWrapper dis, LinkPrototype lp) throws IOException {
		System.out.println("Reading movement at " + Integer.toHexString(lp.targetOffset) + "(from " + Integer.toHexString(lp.sourceOffset) + ")");
		int methodPtr = lp.targetOffset;
		dis.seek(methodPtr);
		int max = dis.length() - 4;

		DisassembledMethod m = new DisassembledMethod(methodPtr);

		MovementReader:
		while (dis.getPosition() < max) {
			int insPtr = dis.getPosition();

			int opCode = dis.readUnsignedShort();
			int duration = dis.readUnsignedShort();

			if (opCode != 0xFE) {
				m.instructions.add(new DisassembledCall(insPtr, new NTRInstructionPrototype(opCode, new NTRArgument(NTRDataType.U16)), duration));
			} else {
				break;
			}
		}

		movements.add(m);
	}

	private void readScriptHeader(IOWrapper dis) throws IOException {
		dis.seek(0);
		int max = dis.length() - 4;

		while (dis.getPosition() < max) {
			int offset = dis.readInt();
			if ((offset & 0xFFFF) == VScriptFile.V_SCR_MAGIC) {
				return;
			}
			publics.add(new LinkPrototype(dis, offset));
		}
	}
}
