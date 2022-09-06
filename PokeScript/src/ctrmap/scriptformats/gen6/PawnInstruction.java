package ctrmap.scriptformats.gen6;

import xstandard.math.BitMath;
import xstandard.text.StringEx;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PawnInstruction {

	public int pointer;

	public PawnOpCode opCode;
	public long[] arguments;
	public int cellSize = 4;

	public int line;

	public List<JumpListener> jmpListeners = new ArrayList<>();

	public GFLPawnScript script;

	public PawnInstruction(PawnOpCode cmd) {
		this(cmd, true);
	}

	public PawnInstruction(PawnOpCode cmd, boolean optimizePacked) {
		pointer = 0;
		opCode = cmd;
		arguments = new long[opCode.argumentCount];
		if (optimizePacked) {
			optimizePacked();
		}
	}

	private PawnInstruction(int ptr, PawnOpCode cmd) {
		pointer = ptr;
		opCode = cmd;
		arguments = new long[cmd.argumentCount];
	}

	public void optimizePacked() {
		if (arguments.length == 1 && (short) arguments[0] == arguments[0]) {
			opCode = opCode.getPackedEquivalent();
		}
	}

	public PawnInstruction(PawnOpCode cmd, long... args) {
		opCode = cmd;
		arguments = args;
		optimizePacked();
	}

	public PawnInstruction(long singleValue) {
		arguments = new long[]{singleValue};
	}

	public PawnInstruction(DataInput in, int ptr, int cellSize) throws IOException {
		this.cellSize = cellSize;
		this.pointer = ptr;

		long op = readCell(in, cellSize);
		opCode = PawnOpCode.OPCODES[(int) (op & BitMath.makeMask64(cellSize * 4))];

		if (opCode.packed) {
			arguments = new long[]{op >> (cellSize * 4)};
		} else {
			if (opCode == PawnOpCode.CASETBL) {
				long caseCount = readCell(in, cellSize);
				arguments = new long[(int) (caseCount * 2 + 2)];
				arguments[0] = caseCount;
				arguments[1] = readCell(in, cellSize); //default case

				for (int i = 0; i < caseCount * 2; i++) {
					arguments[2 + i] = readCell(in, cellSize);
				}
			} else {
				arguments = new long[opCode.argumentCount];

				for (int i = 0; i < arguments.length; i++) {
					arguments[i] = readCell(in, cellSize);
				}
			}
		}
	}

	public void write(DataOutput out) throws IOException {
		if (opCode.packed) {
			writeCell(out, cellSize, opCode.ordinal() | (arguments.length > 0 ? (arguments[0] << 4 * cellSize) : 0));
		} else {
			writeCell(out, cellSize, opCode.ordinal());
			for (int i = 0; i < arguments.length; i++) {
				writeCell(out, cellSize, arguments[i]);
			}
		}
	}

	public static long readCell(DataInput in, int cellSize) throws IOException {
		switch (cellSize) {
			case Integer.BYTES:
				return in.readInt();
			case Long.BYTES:
				return in.readLong();
			case Short.BYTES:
				return in.readShort();
		}
		throw new IllegalArgumentException("Bad cell size");
	}

	public static void writeCell(DataOutput out, int cellSize, long cell) throws IOException {
		switch (cellSize) {
			case Integer.BYTES:
				out.writeInt((int) cell);
				break;
			case Long.BYTES:
				out.writeLong(cell);
				break;
			case Short.BYTES:
				out.writeShort((short) cell);
				break;
		}
	}

	public int getArgumentCount() {
		return arguments.length;
	}

	public int getSize() {
		int size = cellSize;
		if (!opCode.packed) {
			size += arguments.length * cellSize;
		}
		return size;
	}

	public static PawnInstruction fromString(int ptr, int cellSize, String instruction) {
		return fromString(ptr, instruction, cellSize, true);
	}

	public static PawnInstruction fromString(int ptr, String instruction, int cellSize, boolean doOutput) {
		PawnInstruction ret = null;
		int lastJunk = instruction.lastIndexOf(':');
		int idx = lastJunk + 1;
		while (idx < instruction.length() && instruction.charAt(idx) == ' ') {
			idx++;
		}
		char character;
		StringBuilder output = new StringBuilder();
		while (idx < instruction.length() && (character = instruction.charAt(idx)) != '(') {
			idx++;
			if (character == ' ') {
				break;
			} else {
				output.append(character);
			}
		}
		String finalCmd = StringEx.deleteAllChars(output.toString().toUpperCase(), '_');
		for (int i = 0; i < PawnOpCode.OPCODES.length; i++) {
			if (PawnOpCode.OPCODES[i].compactName.equals(finalCmd)) {
				ret = new PawnInstruction(ptr, PawnOpCode.OPCODES[i]);
				break;
			}
		}

		if (ret != null && ret.opCode == PawnOpCode.CASETBL) { //CASETBL
			return ret; //the subroutine disassembler will handle the rest
		}

		if (ret == null) {
			//syntax error or invalid instruction
			return new PawnInstruction(ptr, PawnOpCode.NONE);
		}

		while (idx < instruction.length() && instruction.charAt(idx) == ' ') {
			idx++;
		}
		if (instruction.substring(idx).startsWith("=>")) {
			idx += 2;
			while (idx < instruction.length() && instruction.charAt(idx) == ' ') {
				idx++;
			}
			StringBuilder jmpBldr = new StringBuilder();
			while (idx < instruction.length() && (character = instruction.charAt(idx)) != ' ') {
				jmpBldr.append(character);
				idx++;
			}
			if (ret.opCode.isJump() && ret.arguments.length == 1 && jmpBldr.toString().length() > 0) {
				try {
					ret.arguments[0] = Integer.parseInt(StringEx.deleteAllString(jmpBldr.toString(), "0x"), 16);
				} catch (NumberFormatException ex) {
					if (doOutput) {
						System.err.println("[ERR] 0x" + Integer.toHexString(ptr).toUpperCase() + " : " + "Argument is not a number.");
					}
				}
			} else if (doOutput) {
				System.err.println("[ERR] 0x" + Integer.toHexString(ptr).toUpperCase() + " : " + "Jump instruction syntax on incompatible instruction.");
			}
		} else if (instruction.substring(idx).startsWith("(")) {
			String allArgs = StringEx.deleteAllChars(instruction.substring(idx + 1), ' ', ')').trim();
			if (allArgs.isEmpty()) {
				if (ret.getArgumentCount() != 0) {
					if (doOutput) {
						System.err.println("[ERR] 0x" + Integer.toHexString(ptr).toUpperCase() + " : " + ret + " : Expected arguments, got none.");
					}
				}
			} else {
				String[] argsUnparsed = StringEx.splitOnecharFast(allArgs, ',');
				if (argsUnparsed.length == ret.getArgumentCount()) {
					for (int i = 0; i < argsUnparsed.length; i++) {
						String src = argsUnparsed[i];
						try {
							if (src.endsWith("f")) {
								String fsrc = src.substring(0, src.length() - 1);
								if (cellSize == Long.BYTES) {
									ret.arguments[i] = Double.doubleToLongBits(Double.parseDouble(fsrc));
								} else {
									ret.arguments[i] = Float.floatToIntBits(Float.parseFloat(fsrc));
								}
								//System.out.println("floatarg " + src + " at " + Integer.toHexString(ptr));
							} else {
								ret.arguments[i] = Integer.parseInt(argsUnparsed[i]);
							}
						} catch (NumberFormatException e) {
							if (doOutput) {
								System.err.println("[ERR] 0x" + Integer.toHexString(ptr).toUpperCase() + " : " + "Argument " + i + " is not a number.");
							}
						}
					}
				} else if (doOutput) {
					System.err.println("[ERR] 0x" + Integer.toHexString(ptr).toUpperCase() + " : " + ret + " : " + "Source argument count doesn't match its command.");
				}
			}
		} else {
			if (doOutput) {
				System.err.println("[ERR] 0x" + Integer.toHexString(ptr).toUpperCase() + " : " + ret + " : " + "Expected arguments but none found.");
			}
			ret.opCode = PawnOpCode.NONE;
		}

		return ret;
	}

	public long getArgument(int num) {
		return arguments[num];
	}

	public int getArgumentInt(int num) {
		return (int) arguments[num];
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(opCode);
		if (arguments.length > 0) {
			if (opCode.isJump()) {
				sb.append(" => 0x");
				sb.append(Long.toHexString(pointer + arguments[0]));
			} else if (opCode.isCasetbl()) {
				sb.append("\n{\n");

				long count = arguments[0];

				Map<String, Long> jumpCases = new LinkedHashMap<>();

				jumpCases.put("*", arguments[1] + pointer + cellSize);

				for (int i = 0; i < count; i++) {
					int index = (i + 1) * 2;
					jumpCases.put(String.valueOf(arguments[index]), arguments[index + 1] + pointer + cellSize * (index + 1));
				}

				for (Map.Entry<String, Long> jc : jumpCases.entrySet()) {
					sb.append("\t");
					sb.append(jc.getKey());
					sb.append(" => 0x");
					sb.append(Long.toHexString(jc.getValue()));
					sb.append("\n");
				}

				sb.append("}");
			} else {
				sb.append("(");
				for (int i = 0; i < arguments.length; i++) {
					if (i != 0) {
						sb.append(",");
					}
					long floatTest = arguments[i];
					if (floatTest < 0) {
						floatTest = -floatTest;
					}
					if (floatTest > 0xFFFFFFL) {
						double d;
						if (cellSize == Long.BYTES) {
							d = Double.longBitsToDouble(arguments[i]);
						} else {
							d = Float.intBitsToFloat((int) arguments[i]);
						}
						double abs = Math.abs(d);
						if (abs > 0.0001f && abs < 10E7) {
							sb.append(d);
							sb.append("f");
						} else {
							sb.append(arguments[i]);
						}
					} else {
						sb.append(arguments[i]);
					}
				}
				sb.append(")");
			}
		} else {
			sb.append("()");
		}

		return sb.toString();
	}

	public void chkAddJumpListener() {
		jmpListeners.clear();
		if (opCode.isJump() && script != null) {
			JumpListener jl = new JumpListener(this);
			jl.setParent(script.lookupInstructionByPtr(arguments[0] + pointer));
			jmpListeners.add(jl);
		}
		if (opCode.isCasetbl() && script != null) {
			CaseListener cl = new CaseListener(this, script);
			jmpListeners.add(cl);
		}
	}

	public void setParent(GFLPawnScript parent) {
		this.script = parent;
		this.cellSize = parent.getCellSize();
		chkAddJumpListener();
	}

	public void callJumpListeners() {
		for (int i = 0; i < jmpListeners.size(); i++) {
			jmpListeners.get(i).onAddressChange();
		}
	}

	public boolean setArgsFromString(String str) {
		if (!opCode.isJump()) {
			String allArgs = StringEx.deleteAllChars(str, '(', ')', ' ');
			String[] argsUnparsed = StringEx.splitOnecharFast(allArgs, ',');
			if (argsUnparsed.length == getArgumentCount()) {
				for (int i = 0; i < argsUnparsed.length; i++) {
					try {
						String src = argsUnparsed[i];
						if (src.endsWith("f")) {
							arguments[i] = Float.floatToIntBits(Float.parseFloat(StringEx.deleteAllChars(src, 'f')));
						} else {
							arguments[i] = Integer.parseInt(argsUnparsed[i]);
						}
					} catch (NumberFormatException e) {
						return false;
					}
				}
			} else {
				return false;
			}
		} else {
			String jumpOnly = StringEx.deleteAllString(StringEx.deleteAllString(StringEx.deleteAllChars(str, ' ', 'n'), "=>"), "0x");
			try {
				arguments[0] = Integer.parseInt(jumpOnly.trim(), 16) - pointer;
				jmpListeners.clear();
				chkAddJumpListener();
			} catch (NumberFormatException e) {
				return false;
			}
		}
		return true;
	}

	public void checkJmpConvertArgs() {
		if (opCode.isJump()) {
			arguments[0] = arguments[0] - pointer;
		}
	}

	public static class JumpListener {

		private final PawnInstruction src;
		private PawnInstruction target;

		public JumpListener(PawnInstruction jumpSource) {
			src = jumpSource;
		}

		public void setParent(PawnInstruction target) {
			this.target = target;
		}

		public PawnInstruction getParent() {
			return target;
		}

		public void onAddressChange() {
			try {
				src.arguments[0] = target.pointer - src.pointer;
			} catch (NullPointerException e) {
				src.arguments[0] = -src.pointer;
			}
		}
	}

	public static class CaseListener extends JumpListener {

		private final PawnInstruction src;
		public Map<Long, PawnInstruction> targets = new HashMap<>();
		public PawnInstruction defaultTarget;

		private GFLPawnScript instlib;

		public CaseListener(PawnInstruction jumpSource, GFLPawnScript instlib) {
			super(jumpSource);
			this.instlib = instlib;
			src = jumpSource;
			for (int i = 2; i < src.arguments.length; i += 2) {
				long ptr = (src.pointer + i * src.cellSize) + src.arguments[i + 1] + src.cellSize;
				targets.put(src.arguments[i], instlib.lookupInstructionByPtr(ptr));
			}
			defaultTarget = instlib.lookupInstructionByPtr((src.pointer + src.cellSize) + src.arguments[1]);
		}

		@Override
		public void onAddressChange() {
			for (int i = 2; i < src.arguments.length; i += 2) {
				if (targets.get(src.arguments[i]) != null) {
					try {
						src.arguments[i + 1] = targets.get(src.arguments[i]).pointer - (src.pointer + i * src.cellSize) - src.cellSize;
					} catch (NullPointerException e) {
						//invalid ptr
					}
				}
			}
			try {
				src.arguments[1] = defaultTarget.pointer - (src.pointer + src.cellSize);
			} catch (NullPointerException e) {

			}
		}
	}

}
