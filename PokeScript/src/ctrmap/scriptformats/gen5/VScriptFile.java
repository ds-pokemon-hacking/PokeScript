package ctrmap.scriptformats.gen5;

import ctrmap.pokescript.instructions.gen5.VOpCode;
import ctrmap.pokescript.instructions.ntr.NTRInstructionCall;
import ctrmap.pokescript.instructions.ntr.NTRInstructionLink;
import ctrmap.pokescript.instructions.ntr.NTRInstructionLinkSetup;
import ctrmap.scriptformats.gen5.disasm.VDisassembler;
import ctrmap.stdlib.fs.FSFile;
import ctrmap.stdlib.io.LittleEndianDataInputStream;
import ctrmap.stdlib.io.LittleEndianDataOutputStream;
import java.io.ByteArrayOutputStream;
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

	public VScriptFile() {

	}

	public VScriptFile(FSFile fsf) {
		this.source = fsf;

		if (source.exists()) {
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
	
	public FSFile getSourceFile(){
		return source;
	}

	public void replaceFrom(VScriptFile scr) {
		publics = new ArrayList<>(scr.publics);
		instructions = new ArrayList<>(scr.instructions);
	}

	public void saveToFile() {
		source.setBytes(getBinaryData());
	}

	public void updatePtrs() {
		int ptr = publics.size() * Integer.BYTES + Short.BYTES;
		for (NTRInstructionCall call : instructions) {
			call.pointer = ptr;
			ptr += call.getSize();
		}
	}

	public void updateLinks() {
		updatePtrs();
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
		for (NTRInstructionCall ins : instructions) {
			if (ins.pointer == ptr) {
				return ins;
			}
		}
		return null;
	}

	public byte[] getBinaryData() {
		try {
			updateLinks();

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			LittleEndianDataOutputStream dos = new LittleEndianDataOutputStream(out);

			for (NTRInstructionLink publicMethod : publics) {
				dos.writeInt(publicMethod.target.pointer - (dos.getPosition() + 4));
			}

			dos.writeShort(V_SCR_MAGIC);

			for (NTRInstructionCall call : instructions) {
				call.write(dos);
			}

			dos.close();
			return out.toByteArray();
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
