package ctrmap.scriptformats.gen5;

import ctrmap.pokescript.instructions.gen5.VOpCode;
import ctrmap.pokescript.instructions.ntr.NTRInstructionCall;
import ctrmap.pokescript.instructions.ntr.NTRInstructionLink;
import ctrmap.pokescript.instructions.ntr.NTRInstructionLinkSetup;
import ctrmap.scriptformats.gen5.disasm.VDisassembler;
import ctrmap.stdlib.fs.FSFile;
import ctrmap.stdlib.io.base.impl.ext.data.DataIOStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VScriptFile {

	public static final NTRInstructionLinkSetup[] GEN_V_LINK_SETUP = new NTRInstructionLinkSetup[]{
		new NTRInstructionLinkSetup(VOpCode.Jump, 0),
		new NTRInstructionLinkSetup(VOpCode.JumpOnCmp, 1),
		new NTRInstructionLinkSetup(VOpCode.Call, 0),
		new NTRInstructionLinkSetup(VOpCode.CallOnCmp, 1),
		new NTRInstructionLinkSetup(0x64, 1) //ApplyMovement
	};

	public static final int V_SCR_MAGIC = 0xFD13;

	private FSFile source;

	public List<NTRInstructionLink> publics = new ArrayList<>();

	public List<NTRInstructionCall> instructions = new ArrayList<>();
	public List<NTRInstructionCall> movements = new ArrayList<>();

	public VScriptFile() {

	}

	public VScriptFile(FSFile fsf) {
		this.source = fsf;

		if (source.exists()) {
			//Force copy to OvFS
			try {
				source.getInputStream().close();
			} catch (IOException ex) {
				Logger.getLogger(VScriptFile.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}

	public void disassemble(VCommandDataBase commandDB) {
		if (source != null) {
			new VDisassembler(this, commandDB).disassemble();
		}
	}

	public FSFile getSourceFile() {
		return source;
	}

	public void replaceFrom(VScriptFile scr) {
		publics = new ArrayList<>(scr.publics);
		instructions = new ArrayList<>(scr.instructions);
		movements = new ArrayList<>(scr.movements);
	}

	private List<NTRInstructionCall> getAllInstructions() {
		List<NTRInstructionCall> l = new ArrayList<>();
		l.addAll(instructions);
		l.addAll(movements);
		return l;
	}

	public void saveToFile() {
		source.setBytes(getBinaryData());
	}

	public void updatePtrs() {
		int ptr = publics.size() * Integer.BYTES + Short.BYTES;
		for (NTRInstructionCall call : getAllInstructions()) {
			call.pointer = ptr;
			ptr += call.getSize();
		}
	}

	public void updateLinks() {
		updatePtrs();
		updateLinksWithoutPtrs();
	}

	private void updateLinksWithoutPtrs() {
		for (NTRInstructionCall call : instructions) {
			if (call.link != null) {
				call.link.updateSourceArg();
			}
		}
	}

	public void setUpLinks() {
		updatePtrs();
		for (NTRInstructionCall call : instructions) {
			call.setupLink(this, GEN_V_LINK_SETUP);
		}
	}

	public void addPublic(NTRInstructionCall firstInstruction) {
		publics.add(new NTRInstructionLink(null, firstInstruction, -1));
	}

	public NTRInstructionCall getInstructionByPtr(int ptr) {
		for (NTRInstructionCall ins : getAllInstructions()) {
			if (ins.pointer == ptr) {
				return ins;
			}
		}
		return null;
	}

	public void write() {
		if (source != null) {
			source.setBytes(getBinaryData());
		}
	}

	public byte[] getBinaryData() {
		try {
			updatePtrs();

			List<NTRInstructionCall> alignedInstructions = new ArrayList<>(getAllInstructions());

			int ptr = publics.size() * Integer.BYTES + Short.BYTES;
			for (NTRInstructionCall call : alignedInstructions) {
				if (call.definition.opCode == 0x64) {
					if (call.link != null && call.link.target != null) {
						if ((call.link.target.pointer & 1) != 0) {
							call.link.target.pointer = -1;
						}
					}
				}

				if (call.pointer == -1) {
					call.pointer++; //pointer requires word-alignment
					ptr = call.pointer;
				}

				call.pointer = ptr;
				ptr += call.getSize();
			}

			updateLinksWithoutPtrs();

			DataIOStream dos = new DataIOStream();

			for (NTRInstructionLink publicMethod : publics) {
				dos.writeInt(publicMethod.target.pointer - (dos.getPosition() + 4));
			}

			dos.writeShort(V_SCR_MAGIC);

			int pos = dos.getPosition();
			for (NTRInstructionCall call : getAllInstructions()) {
				if (pos != call.pointer){
					dos.seek(call.pointer);
				}
				call.write(dos);
				pos += call.getSize();
			}

			dos.close();
			return dos.toByteArray();
		} catch (IOException ex) {
			Logger.getLogger(VScriptFile.class.getName()).log(Level.SEVERE, null, ex);
		}
		return null;
	}

	public String getASCII() {
		StringBuilder sb = new StringBuilder();

		for (NTRInstructionCall call : instructions) {
			sb.append(call.toString());
			sb.append("\n");
		}

		return sb.toString();
	}
}
