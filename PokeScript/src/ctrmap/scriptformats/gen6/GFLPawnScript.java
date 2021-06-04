package ctrmap.scriptformats.gen6;

import ctrmap.stdlib.fs.FSFile;
import ctrmap.stdlib.io.base.iface.DataInputEx;
import ctrmap.stdlib.io.base.iface.DataOutputEx;
import ctrmap.stdlib.io.base.impl.ext.data.DataIOStream;
import ctrmap.stdlib.io.base.impl.ext.data.DataInStream;
import ctrmap.stdlib.io.util.StringIO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GFLPawnScript {

	public byte[] compCode;
	public int[] decInstructions;
	private boolean decompressed;

	public List<PawnPrefixEntry> publics = new ArrayList<>();
	public List<PawnPrefixEntry> natives = new ArrayList<>();
	public List<PawnPrefixEntry> libraries = new ArrayList<>();
	public List<PawnPrefixEntry> publicVars = new ArrayList<>();
	public List<PawnPrefixEntry> tags = new ArrayList<>();
	public List<PawnPrefixEntry> overlays = new ArrayList<>();

	public short sNAMEMAX;
	public Map<Integer, String> nameTable = new HashMap<>();

	public List<PawnInstruction> instructions = new ArrayList<>();
	public List<PawnInstruction> data = new ArrayList<>();

	Map<Integer, PawnInstruction> publicDummies = new HashMap<>();
	public PawnInstruction mainEntryPointDummy;

	public int len;
	public int magic;
	public int ver;
	public int minCompatVer;
	public int flags;
	public int defsize;
	public int instructionStart;
	public int dataStart;
	public int heapStart;
	public int mainEntryPoint;

	public int publicsOffset;
	public int nativesOffset;
	public int librariesOffset;
	public int publicVarsOffset;
	public int tagsOffset;
	public int overlaysOffset;
	public int nameTableOffset;

	public int stackSize;

	public int compCodeLen;
	public int decCodeLen;

	public GFLPawnScript() {
		decompressed = true;
		magic = 0xF1E0;
		ver = 10;
		minCompatVer = 10;
		flags = 0x1C;
		defsize = 0x8;
		stackSize = 0x1000;
		sNAMEMAX = 63;
	}

	public static GFLPawnScript createExecutableGFLScript() {
		GFLPawnScript s = new GFLPawnScript();

		s.instructions.add(new PawnInstruction(PawnInstruction.Commands.HALT_P));
		s.mainEntryPoint = 0;
		s.setInstructionListeners();

		return s;
	}

	public GFLPawnScript(byte[] b) {
		this(new DataInStream(b));
	}
	
	public GFLPawnScript(FSFile fsf) {
		this(new DataInStream(fsf.getInputStream()));
	}

	public GFLPawnScript(DataInputEx in) {
		try {
			len = in.readInt();
			magic = in.readShort(); //F1 E0 == 32-bit cell, 0x04
			ver = in.readUnsignedByte(); //0x06
			minCompatVer = in.readUnsignedByte(); //0x07
			flags = in.readUnsignedShort(); //0x08
			defsize = in.readUnsignedShort(); //0x0a
			instructionStart = in.readInt(); //0x0c
			dataStart = in.readInt(); //0x10
			heapStart = in.readInt(); //0x14
			stackSize = in.readInt() - heapStart; //0x18
			mainEntryPoint = in.readInt(); //0x1c

			publicsOffset = in.readInt(); //0x20
			nativesOffset = in.readInt(); //0x24
			librariesOffset = in.readInt(); //0x28
			publicVarsOffset = in.readInt(); //0x2c
			tagsOffset = in.readInt(); //0x30
			overlaysOffset = in.readInt(); //0x34
			nameTableOffset = in.readInt(); //0x38

			readEntries(publics, PawnPrefixEntry.Type.PUBLIC, defsize, in, (nativesOffset - publicsOffset) / defsize);
			readEntries(natives, PawnPrefixEntry.Type.NATIVE, defsize, in, (librariesOffset - nativesOffset) / defsize);
			readEntries(libraries, PawnPrefixEntry.Type.LIBRARY, defsize, in, (publicVarsOffset - librariesOffset) / defsize);
			readEntries(publicVars, PawnPrefixEntry.Type.PUBLIC_VAR, defsize, in, (tagsOffset - publicVarsOffset) / defsize);
			readEntries(tags, PawnPrefixEntry.Type.TAG, defsize, in, (overlaysOffset - tagsOffset) / defsize);
			readEntries(overlays, PawnPrefixEntry.Type.OVERLAY, defsize, in, (nameTableOffset - overlaysOffset) / defsize);
			//readEntries(unknowns, PawnPrefixEntry.Type.UNKNOWN, defsize, in, (instructionStart - unknownOffset) / defsize);

			readNameTable(in);

			//align
			in.align(4);

			compCodeLen = len - instructionStart;

			decCodeLen = heapStart - instructionStart;
			compCode = new byte[compCodeLen];
			in.read(compCode);
		} catch (IOException ex) {
			Logger.getLogger(GFLPawnScript.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void readNameTable(DataInputEx dis) throws IOException {
		sNAMEMAX = dis.readShort(); //the name table is completely unused in GFL pawn scripts, but still
		while (dis.getPosition() < instructionStart) {
			int offset = (int) dis.getPosition();
			String value = StringIO.readString(dis);
			if (value.length() > 0) { //the table is padded with zeros, so any strings there will be 0 in length, indicating the end of the table
				//if there is no padding space, dis.position is equal to instructionStart and the loop ends anyway
				nameTable.put(offset, value);
			} else {
				break;
			}
		}
	}

	public void writeNameTable(DataOutputEx dos) throws IOException {
		dos.writeShort(sNAMEMAX);
		for (String s : nameTable.values()) {
			dos.writeString(s);
		}
	}

	public int getNameTableLength() {
		int len = 4;
		for (String name : nameTable.values()) {
			len += name.length();
			len++;
		}
		return len;
	}

	public void write(DataOutputEx dos) throws IOException {
		byte[] instructionsToWrite;
		if (decompressed) {
			publicsOffset = 0x3C; //header length
			nativesOffset = publicsOffset + publics.size() * defsize;
			librariesOffset = nativesOffset + natives.size() * defsize;
			publicVarsOffset = librariesOffset + libraries.size() * defsize;
			tagsOffset = publicVarsOffset + publicVars.size() * defsize;
			overlaysOffset = tagsOffset + tags.size() * defsize;
			nameTableOffset = overlaysOffset + overlays.size() * defsize;

			updateRaw();
			instructionStart = nameTableOffset + getNameTableLength();
			heapStart = instructionStart + decInstructions.length * 4;
			dataStart = heapStart - data.size() * 4;
			if (mainEntryPointDummy != null) {
				mainEntryPoint = mainEntryPointDummy.argumentCells[0];
			} else {
				mainEntryPoint = -1;
			}
			instructionsToWrite = compressScript(decInstructions);
			len = instructionStart + instructionsToWrite.length;

			for (PawnPrefixEntry p : publics) {
				PawnInstruction jump = publicDummies.get(p.data[1]);
				if (jump != null) {
					p.data[0] = jump.argumentCells[0];
				}
			}
		} else {
			instructionsToWrite = compCode;
		}
		dos.writeInt(len);
		dos.writeShort((short) magic);
		dos.write(ver);
		dos.write(minCompatVer);
		dos.writeShort((short) flags);
		dos.writeShort((short) defsize);
		dos.writeInt(instructionStart);
		dos.writeInt(dataStart);
		dos.writeInt(heapStart);
		dos.writeInt(heapStart + stackSize);
		dos.writeInt(mainEntryPoint);
		dos.writeInt(publicsOffset);
		dos.writeInt(nativesOffset);
		dos.writeInt(librariesOffset);
		dos.writeInt(publicVarsOffset);
		dos.writeInt(tagsOffset);
		dos.writeInt(overlaysOffset);
		dos.writeInt(nameTableOffset);

		writeEntries(publics, dos);
		writeEntries(natives, dos);
		writeEntries(libraries, dos);
		writeEntries(publicVars, dos);
		writeEntries(tags, dos);
		writeEntries(overlays, dos);
		//writeEntries(unknowns, dos);

		writeNameTable(dos);

		while (dos.getPosition() % 4 != 0) {
			dos.write(0);
		}

		dos.write(instructionsToWrite);
		while (dos.getPosition() % 4 != 0) { //padding to 4 bytes
			dos.write(0);
		}
	}

	public void replaceAll(GFLPawnScript copy) {
		if (copy == null){
			return;
		}
		publics = copy.publics;
		natives = copy.natives;
		publicVars = copy.publicVars;
		libraries = copy.libraries;
		tags = copy.tags;
		overlays = copy.overlays;
		nameTable = copy.nameTable;
		sNAMEMAX = copy.sNAMEMAX;
		stackSize = copy.stackSize;

		instructions = copy.instructions;
		data = copy.data;

		mainEntryPoint = copy.mainEntryPoint;
		mainEntryPointDummy = copy.mainEntryPointDummy;

		setInstructionListeners();
	}

	public byte[] getScriptBytes() {
		try {
			DataIOStream baos = new DataIOStream();
			write(baos);
			return baos.toByteArray();
		} catch (IOException ex) {
			Logger.getLogger(GFLPawnScript.class.getName()).log(Level.SEVERE, null, ex);
			return null;
		}
	}

	public void decompressThis() {
		if (!decompressed) {
			decInstructions = quickDecompress(compCode, decCodeLen / 4);
			decompressed = true;
			instructions.clear();
			data.clear();
			for (int i = 0; i < (dataStart - instructionStart) / 4; i++) {
				PawnInstruction ins = new PawnInstruction(i * 4, decInstructions, this);
				instructions.add(ins);
				if (!ins.hasCompressedArgument) {
					i += ins.getArgumentCount();
				}
			}
			for (int j = dataStart - instructionStart; j < decInstructions.length * 4; j += 4) {
				String parse = Integer.toHexString(decInstructions[j / 4]);
				PawnInstruction dataDummy = new PawnInstruction(j, decInstructions[j / 4], parse);
				dataDummy.hasCompressedArgument = false;
				dataDummy.argumentCells = new int[0];
				data.add(dataDummy);
			}
		}
	}

	private void readEntries(List<PawnPrefixEntry> target, PawnPrefixEntry.Type typeForAll, int defsize, DataInput in, int count) throws IOException {
		for (int i = 0; i < count; i++) {
			target.add(new PawnPrefixEntry(defsize, typeForAll, in));
		}
	}

	private void writeEntries(List<PawnPrefixEntry> list, DataOutput target) throws IOException {
		for (int i = 0; i < list.size(); i++) {
			list.get(i).write(target);
		}
	}

	public void updateRaw() {
		int[] codeIns = PawnDisassembler.getRawInstructions(instructions);
		int[] dataIns = PawnDisassembler.getRawInstructions(data);
		int movementLength = dataIns.length * 4;
		dataStart = heapStart - movementLength;
		decInstructions = new int[codeIns.length + dataIns.length];
		System.arraycopy(codeIns, 0, decInstructions, 0, codeIns.length);
		System.arraycopy(dataIns, 0, decInstructions, codeIns.length, dataIns.length);
	}

	public void replaceInstructions(int[] newIns) {
		int[] compiledMovement = PawnDisassembler.getRawInstructions(data);
		int[] target = new int[newIns.length + compiledMovement.length];
		System.arraycopy(newIns, 0, target, 0, newIns.length);
		System.arraycopy(compiledMovement, 0, target, newIns.length, compiledMovement.length);
		decInstructions = target;
	}

	public PawnInstruction lookupInstructionByPtr(int ptr) {
		for (int i = 0; i < instructions.size(); i++) {
			if (instructions.get(i).pointer == ptr) {
				return instructions.get(i);
			}
		}
		return null;
	}

	public void setPtrsByIndex() {
		int currentPtr = 0;
		for (int i = 0; i < instructions.size(); i++) {
			PawnInstruction ins = instructions.get(i);
			ins.pointer = currentPtr;
			currentPtr += ins.getSize();
		}
	}

	public void setInstructionListeners() {
		setPtrsByIndex();

		for (PawnInstruction i : instructions) {
			i.setParent(this);
		}

		for (PawnPrefixEntry p : publics) {
			if (!publicDummies.containsKey(p.data[1])) {
				PawnInstruction jump = new PawnInstruction(0, 0x81, "PUBLIC DUMMY");
				jump.argumentCells = new int[]{p.data[0]};
				jump.setParent(this);
				publicDummies.put(p.data[1], jump);
			}
		}

		if (mainEntryPoint != -1) {
			if (mainEntryPointDummy == null) {
				mainEntryPointDummy = new PawnInstruction(0, 0x81, "MAIN ENTRYPOINT");
				mainEntryPointDummy.argumentCells = new int[]{mainEntryPoint};
				mainEntryPointDummy.setParent(this);
			}
		}
	}

	public void updatePublicListeners() {
		setPtrsByIndex();

		for (PawnInstruction p : publicDummies.values()) {
			p.setParent(this);
		}

		if (mainEntryPoint != -1) {
			if (mainEntryPointDummy != null) {
				mainEntryPointDummy.setParent(this);
			}
		}
	}
	
	public PawnPrefixEntry getPrefixEntryByName(List<PawnPrefixEntry> list, String name) {
		return getPrefixEntryByName(list, GFScrHash.getHash(name));
	}

	public PawnPrefixEntry getPrefixEntryByName(List<PawnPrefixEntry> list, int name) {
		for (PawnPrefixEntry e : list) {
			if (e.data[1] == name) {
				return e;
			}
		}
		return null;
	}

	public void callInstructionListeners() {
		setPtrsByIndex();
		for (int i = 0; i < instructions.size(); i++) {
			instructions.get(i).callJumpListeners();
			instructions.get(i).updateDisassembly();
		}
		for (PawnInstruction i : publicDummies.values()) {
			i.callJumpListeners();
		}
		if (mainEntryPointDummy != null) {
			mainEntryPointDummy.callJumpListeners();
		}
	}

	public static int[] quickDecompress(byte[] data, int count) {
		int[] code = new int[count];
		int i = 0, j = 0, x = 0, f = 0;
		while (i < code.length) {
			int b = data[f++],
					v = b & 0x7F;
			if (++j == 1) // sign extension possible
			{
				x = (int) ((((v >> 6 == 0 ? 1 : 0) - 1) << 6) | v); // only for bit6 being set
			} else {
				x = (x << 7) | (byte) v; // shift data into place
			}
			if ((b & 0x80) != 0) {
				continue; // more data to read
			}
			code[i++] = x;
			j = 0; // write finalized instruction
		}
		return code;
	}

	public static byte[] compressScript(int[] cmd) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			for (int pos = 0; pos < cmd.length; pos++) {
				out.write(compressInstruction(cmd[pos]));
				out.close();
			}
		} catch (IOException ex) {
			Logger.getLogger(GFLPawnScript.class.getName()).log(Level.SEVERE, null, ex);
		}
		return out.toByteArray();
	}

	private static byte[] compressInstruction(int instruction) {
		List<Byte> bytes = new ArrayList<>();
		boolean sign = (instruction & 0x80000000) != 0;

		// Signed (negative) values are handled opposite of unsigned (positive) values.
		// Positive values are "done" when we've shifted the value down to zero, but
		// we don't need to store the highest 1s in a signed value. We handle this by
		// tracking the loop via a NOTed shadow copy of the instruction if it's signed.
		int shadow = sign ? ~instruction : instruction;

		do {
			int least7 = instruction & 0b01111111;
			byte byteVal = (byte) least7;

			if (!bytes.isEmpty()) // Continuation bit on all but the lowest byte
			{
				byteVal |= 0x80;
			}

			bytes.add(byteVal);

			instruction >>= 7;
			shadow >>= 7;
		} while (shadow != 0);

		if (bytes.size() < 5) {
			// Ensure "sign bit" (bit just to the right of highest continuation bit) is
			// correct. Add an extra empty continuation byte if we need to. Values can't
			// be longer than 5 bytes, though.

			int signBit = sign ? 0x40 : 0x00;
			if ((bytes.get(bytes.size() - 1) & 0x40) != signBit) {
				bytes.add((byte) (sign ? 0xFF : 0x80));
			}
		}

		Collections.reverse(bytes);

		byte[] out = new byte[bytes.size()];
		for (int i = 0; i < bytes.size(); i++) {
			out[i] = bytes.get(i);
		}

		return out;
	}
}
