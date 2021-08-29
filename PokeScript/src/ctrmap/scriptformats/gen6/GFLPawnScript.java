package ctrmap.scriptformats.gen6;

import ctrmap.stdlib.fs.FSFile;
import ctrmap.stdlib.fs.accessors.DiskFile;
import ctrmap.stdlib.io.base.iface.DataInputEx;
import ctrmap.stdlib.io.base.iface.DataOutputEx;
import ctrmap.stdlib.io.base.impl.ext.data.DataIOStream;
import ctrmap.stdlib.io.base.impl.ext.data.DataInStream;
import ctrmap.stdlib.io.util.StringIO;
import ctrmap.stdlib.math.BitMath;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GFLPawnScript {

	public static final int PAWN32_MAGIC = 0xF1E0;
	public static final int PAWN64_MAGIC = 0xF1E1;
	public static final int PAWN16_MAGIC = 0xF1E2;

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

	Map<Long, PawnInstruction> publicDummies = new HashMap<>();
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
		magic = PAWN32_MAGIC;
		ver = 10;
		minCompatVer = 10;
		flags = 0x1C;
		defsize = 0x8;
		stackSize = 0x1000;
		sNAMEMAX = 63;
	}

	public int getCellSize() {
		switch (magic) {
			case PAWN16_MAGIC:
				return Short.BYTES;
			case PAWN32_MAGIC:
				return Integer.BYTES;
			case PAWN64_MAGIC:
				return Long.BYTES;
		}
		throw new RuntimeException("Invalid magic: " + Integer.toHexString(magic) + "; Can not determine cell size!");
	}

	public long hashName(String name) {
		if (magic == PAWN32_MAGIC) {
			return GFScrHash.getHash(name);
		} else if (magic == PAWN64_MAGIC) {
			return GFScrHash.getHash64(name);
		}
		throw new UnsupportedOperationException("No hash function for 16-bit Pawn.");
	}

	public static GFLPawnScript createExecutableGFLScript() {
		GFLPawnScript s = new GFLPawnScript();

		s.instructions.add(new PawnInstruction(PawnOpCode.HALT_P));
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
			magic = in.readUnsignedShort(); //F1 E0 == 32-bit cell, 0x04
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

			int cellSize = getCellSize();

			readEntries(publics, PawnPrefixEntry.Type.PUBLIC, defsize, cellSize, in, (nativesOffset - publicsOffset) / defsize);
			readEntries(natives, PawnPrefixEntry.Type.NATIVE, defsize, cellSize, in, (librariesOffset - nativesOffset) / defsize);
			readEntries(libraries, PawnPrefixEntry.Type.LIBRARY, defsize, cellSize, in, (publicVarsOffset - librariesOffset) / defsize);
			readEntries(publicVars, PawnPrefixEntry.Type.PUBLIC_VAR, defsize, cellSize, in, (tagsOffset - publicVarsOffset) / defsize);
			readEntries(tags, PawnPrefixEntry.Type.TAG, defsize, cellSize, in, (overlaysOffset - tagsOffset) / defsize);
			readEntries(overlays, PawnPrefixEntry.Type.OVERLAY, defsize, cellSize, in, (nameTableOffset - overlaysOffset) / defsize);
			//readEntries(unknowns, PawnPrefixEntry.Type.UNKNOWN, defsize, in, (instructionStart - unknownOffset) / defsize);

			readNameTable(in);

			//align
			in.align(4);

			compCodeLen = len - instructionStart;

			decCodeLen = heapStart - instructionStart;
			byte[] compCode = new byte[compCodeLen];
			in.read(compCode);

			byte[] decCode = quickDecompress(compCode, decCodeLen, cellSize);

			new DiskFile("D:/_REWorkspace/scr/enumtest/coderaw.bin").setBytes(decCode);

			DataIOStream codeReader = new DataIOStream(decCode);

			int dataLen = dataStart - instructionStart;

			int pos;
			while ((pos = codeReader.getPosition()) < dataLen) {
				instructions.add(new PawnInstruction(codeReader, pos, cellSize));
			}

			while (codeReader.getPosition() < decCodeLen) {
				data.add(new PawnInstruction(PawnInstruction.readCell(codeReader, cellSize)));
			}

			setInstructionListeners();
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
		publicsOffset = 0x3C; //header length
		nativesOffset = publicsOffset + publics.size() * defsize;
		librariesOffset = nativesOffset + natives.size() * defsize;
		publicVarsOffset = librariesOffset + libraries.size() * defsize;
		tagsOffset = publicVarsOffset + publicVars.size() * defsize;
		overlaysOffset = tagsOffset + tags.size() * defsize;
		nameTableOffset = overlaysOffset + overlays.size() * defsize;

		instructionStart = nameTableOffset + getNameTableLength();

		DataIOStream codeImage = new DataIOStream();

		for (PawnInstruction ins : instructions) {
			ins.write(codeImage);
		}

		int cellSize = getCellSize();

		dataStart = instructionStart + codeImage.getLength();

		for (PawnInstruction ins : data) {
			PawnInstruction.writeCell(codeImage, cellSize, ins.arguments.length > 0 ? ins.arguments[0] : 0);
		}

		heapStart = instructionStart + codeImage.getLength();

		if (mainEntryPointDummy != null) {
			mainEntryPoint = (int) mainEntryPointDummy.arguments[0];
		} else {
			mainEntryPoint = -1;
		}
		codeImage.seek(0);
		byte[] instructionsToWrite = compressScript(codeImage, codeImage.getLength(), getCellSize());
		len = instructionStart + instructionsToWrite.length;

		for (PawnPrefixEntry p : publics) {
			PawnInstruction jump = publicDummies.get(p.data[1]);
			if (jump != null) {
				p.data[0] = jump.arguments[0];
			}
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

		writeEntries(publics, dos, cellSize);
		writeEntries(natives, dos, cellSize);
		writeEntries(libraries, dos, cellSize);
		writeEntries(publicVars, dos, cellSize);
		writeEntries(tags, dos, cellSize);
		writeEntries(overlays, dos, cellSize);
		//writeEntries(unknowns, dos);

		writeNameTable(dos);

		dos.pad(4);

		dos.write(instructionsToWrite);

		dos.pad(4);
	}

	public void replaceAll(GFLPawnScript copy) {
		if (copy == null) {
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
	
	public int readDataAtI(long dataAddr) {
		return (int)readDataAt(dataAddr);
	}
	
	public long readDataAt(long dataAddr) {
		return data.get((int)(dataAddr / getCellSize())).arguments[0];
	}
	
	public void writeDataAt(long dataAddr, long value) {
		long dataIndex = dataAddr / getCellSize();
		if (dataIndex < data.size()) {
			data.get((int)dataIndex).arguments[0] = value;
		}
		else {
			throw new ArrayIndexOutOfBoundsException("Could not write to data section at " + Long.toHexString(dataAddr));
		}
	}
	
	public void expandDataSection(long newSize) {
		long numToAdd = (newSize / getCellSize()) - data.size();
		for (int i = 0; i < numToAdd; i++) {
			PawnInstruction ins = new PawnInstruction(0);
			data.add(ins);
		}
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

	private static void readEntries(List<PawnPrefixEntry> target, PawnPrefixEntry.Type typeForAll, int defsize, int cellSize, DataInput in, int count) throws IOException {
		for (int i = 0; i < count; i++) {
			target.add(new PawnPrefixEntry(defsize, cellSize, typeForAll, in));
		}
	}

	private static void writeEntries(List<PawnPrefixEntry> list, DataOutput target, int cellSize) throws IOException {
		for (int i = 0; i < list.size(); i++) {
			list.get(i).write(target, cellSize);
		}
	}

	public PawnInstruction lookupInstructionByPtr(long ptr) {
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
				PawnInstruction jump = new PawnInstruction(PawnOpCode.JUMP);
				jump.arguments = new long[]{p.data[0]};
				jump.setParent(this);
				publicDummies.put(p.data[1], jump);
			}
		}

		if (mainEntryPoint != -1) {
			if (mainEntryPointDummy == null) {
				mainEntryPointDummy = new PawnInstruction(PawnOpCode.JUMP);
				mainEntryPointDummy.arguments = new long[]{mainEntryPoint};
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

	public List<MethodCall> searchForNativeCalls(String nativeName) {
		List<MethodCall> l = new ArrayList<>();
		PawnPrefixEntry e = getPrefixEntryByName(natives, nativeName);
		if (e != null) {
			long nativeNo = natives.indexOf(e);

			for (PawnInstruction i : instructions) {
				if (i.opCode == PawnOpCode.SYSREQ_N) {
					if (i.arguments[0] == nativeNo) {
						MethodCall call = new MethodCall();
						call.methodName = nativeName;
						call.caller = i;
						call.args.addAll(searchForArgs(i));
						l.add(call);
					}
				}
			}
		}
		return l;
	}

	public List<MethodCall> searchForMethodCalls(PawnInstruction methodProcIns) {
		List<MethodCall> l = new ArrayList<>();
		for (PawnInstruction i : instructions) {
			if (i.opCode == PawnOpCode.CALL) {
				if (i.arguments[0] + i.pointer == methodProcIns.pointer) {
					MethodCall call = new MethodCall();
					call.caller = i;
					call.methodName = "sub_" + Integer.toHexString(methodProcIns.pointer);
					call.args.addAll(searchForArgs(i));
					l.add(call);
				}
			}
		}
		return l;
	}

	public PawnInstruction getMethodStart(PawnInstruction ins) {
		int index = instructions.indexOf(ins);
		for (int i = index; i >= 0; i--) {
			if (instructions.get(i).opCode == PawnOpCode.PROC) {
				return instructions.get(i);
			}
		}
		return null;
	}

	public List<MethodCallArgument> searchForArgs(PawnInstruction callInstr) {
		List<MethodCallArgument> l = new ArrayList<>();

		long numberOf;
		int insIdx = instructions.indexOf(callInstr);
		if (insIdx == -1) {
			return l;
		}

		if (callInstr.opCode == PawnOpCode.SYSREQ_N) {
			numberOf = callInstr.getArgument(1) / getCellSize();
		} else {
			numberOf = instructions.get(insIdx - 1).getArgument(0) / getCellSize();
			insIdx--;
		}
		insIdx--;
		//System.out.println("searching for " + numberOf + " args of method idx " + callInstr.arguments[0]);

		MethodCallArgument arg = new MethodCallArgument();

		for (int i = insIdx; i >= 0; i--) {
			PawnInstruction ins = instructions.get(i);

			int amount = 1;
			switch (ins.opCode) {
				case PUSH2:
				case PUSH2_C:
				case PUSH2_S:
				case PUSH2_ADR:
					amount = 2;
					break;
				case PUSH3:
				case PUSH3_C:
				case PUSH3_S:
				case PUSH3_ADR:
					amount = 3;
					break;
				case PUSH4:
				case PUSH4_C:
				case PUSH4_S:
				case PUSH4_ADR:
					amount = 4;
					break;
				case PUSH5:
				case PUSH5_C:
				case PUSH5_S:
				case PUSH5_ADR:
					amount = 5;
					break;
			}

			for (int j = 0; j < amount; j++) {
				int argIdx = amount - j - 1;

				switch (ins.opCode) {
					case PUSH:
					case PUSH_P:
					case PUSH2:
					case PUSH3:
					case PUSH4:
					case PUSH5:
						arg.type = StackArgType.GVAR;
						arg.value = ins.getArgument(argIdx);
						break;
					case PUSH_C:
					case PUSH_P_C:
					case PUSH2_C:
					case PUSH3_C:
					case PUSH4_C:
					case PUSH5_C:
						arg.type = StackArgType.CONSTANT;
						arg.value = ins.getArgument(argIdx);
						break;
					case PUSH_S:
					case PUSH_P_S:
					case PUSH2_S:
					case PUSH3_S:
					case PUSH4_S:
					case PUSH5_S:
						arg.type = StackArgType.LOCVAR;
						arg.value = ins.getArgument(argIdx);
						break;
					case PUSH_PRI:
						arg.type = StackArgType.REG_PRI;
						break;
					case PUSH_ALT:
						arg.type = StackArgType.REG_ALT;
						break;
					case PUSH_ADR:
					case PUSH_P_ADR:
					case PUSH2_ADR:
					case PUSH3_ADR:
					case PUSH4_ADR:
					case PUSH5_ADR:
						arg.type = StackArgType.ADDRESS;
						arg.value = ins.getArgument(argIdx);
						break;
				}

				if (arg.type != null) {
					l.add(arg);
					arg = new MethodCallArgument();
				}
			}

			if (l.size() >= numberOf) {
				break;
			}
		}

		return l;
	}

	public static class MethodCall {
		public PawnInstruction caller;
		
		public String methodName;

		public List<MethodCallArgument> args = new ArrayList<>();
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(methodName);
			sb.append("(");
			for (int i = 0; i < args.size(); i++) {
				if (i != 0) {
					sb.append(", ");
				}
				sb.append(args.get(i).toString(caller.script));
			}
			sb.append(")");
			return sb.toString();
		}
	}

	public static class MethodCallArgument {

		public StackArgType type;
		public Long value;
		
		public String toString(GFLPawnScript scr) {
			int cellSize = scr.getCellSize();
			switch (type) {
				case ADDRESS:
					return "&(" + value + ")";
				case CONSTANT:
					return String.valueOf(value);
				case GVAR:
					return String.valueOf(scr.data.get((int)(value / cellSize)).arguments[0]);
				case LOCVAR:
					return (value >= cellSize * 3) ? "FUNCARG_" + ((value / cellSize) - 3) : "LOCVAR_" + ((value / -cellSize) - 1);
				case REG_ALT:
					return "ALT";
				case REG_PRI:
					return "PRI";
			}
			return "NULL_TYPE_ERR";
		}
	}

	public static enum StackArgType {
		LOCVAR,
		GVAR,
		REG_PRI,
		REG_ALT,
		CONSTANT,
		ADDRESS
	}

	public PawnPrefixEntry getPrefixEntryByName(List<PawnPrefixEntry> list, String name) {
		long hash = hashName(name);
		return getPrefixEntryByName(list, hash);
	}

	public PawnPrefixEntry getPrefixEntryByName(List<PawnPrefixEntry> list, long name) {
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
		}
		for (PawnInstruction i : publicDummies.values()) {
			i.callJumpListeners();
		}
		if (mainEntryPointDummy != null) {
			mainEntryPointDummy.callJumpListeners();
		}
	}

	public static byte[] quickDecompress(byte[] data, int byteSize, int cellSize) {
		DataIOStream out = new DataIOStream();
		try {
			int i = 0, j = 0, f = 0;
			long x = 0;
			byteSize /= cellSize;
			while (i < byteSize) {
				long b = data[f++],
					v = b & 0x7F;
				if (++j == 1) // sign extension possible
				{
					x = ((((v >> 6 == 0 ? 1 : 0) - 1) << 6) | v); // only for bit6 being set
				} else {
					x = (x << 7) | v; // shift data into place
				}
				if ((b & 0x80) != 0) {
					continue; // more data to read
				}
				PawnInstruction.writeCell(out, cellSize, x);
				i++;
				j = 0; // write finalized instruction

			}
		} catch (IOException ex) {
			Logger.getLogger(GFLPawnScript.class.getName()).log(Level.SEVERE, null, ex);
		}
		return out.toByteArray();
	}

	public static byte[] compressScript(DataInput in, int byteSize, int cellSize) {
		DataIOStream out = new DataIOStream();
		try {
			int cellCount = byteSize / cellSize;
			write_encoded(out, in, cellCount, cellSize);
			out.close();
		} catch (IOException ex) {
			Logger.getLogger(GFLPawnScript.class.getName()).log(Level.SEVERE, null, ex);
		}
		return out.toByteArray();
	}

	/*
	 * Pawn toolkit 3.x.
	 *
	 * C source available at https://github.com/compuphase/pawn/blob/6d82fa4bfa3df019f8144dcb75b994240f8b9a7e/compiler/sc6.c#L193
	 */
	public static void write_encoded(DataOutput out, DataInput in, int count, int cellSize) throws IOException {
		int ENC_MAX = cellSize + 1;
		int ENC_MASK = BitMath.makeMask(cellSize);

		while (count-- > 0) {
			long p = PawnInstruction.readCell(in, cellSize);
			byte[] t = new byte[ENC_MAX];
			byte code;
			int index;
			for (index = 0; index < ENC_MAX; index++) {
				t[index] = (byte) (p & 0x7f);
				/* store 7 bits */
				p >>= 7;
			}
			/* for */
 /* skip leading zeros */
			while (index > 1 && t[index - 1] == 0 && (t[index - 2] & 0x40) == 0) {
				index--;
			}
			/* skip leading -1s */
			if (index == ENC_MAX && t[index - 1] == ENC_MASK && (t[index - 2] & 0x40) != 0) {
				index--;
			}
			while (index > 1 && t[index - 1] == 0x7f && (t[index - 2] & 0x40) != 0) {
				index--;
			}
			/* write high byte first, write continuation bits */
			while (index-- > 0) {
				code = (byte) ((index == 0) ? t[index] : (t[index] | 0x80));
				out.writeByte(code);
			}
			/* while */
		}
	}
}
