package ctrmap.scriptformats.gen6;

import xstandard.text.StringEx;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class PawnSubroutine {

	public String name;
	public int originalPtr;
	public int lastPtr;
	public List<PawnInstruction> instructions = new ArrayList<>();

	public GFLPawnScript parent;

	public PawnSubroutine(int startingInstruction, List<PawnInstruction> source, GFLPawnScript parent) {
		this.parent = parent;
		originalPtr = startingInstruction;
		name = "sub_" + Integer.toHexString(source.get(startingInstruction).pointer).toUpperCase();
		int ins = startingInstruction;
		boolean hasBegun = false;
		MainLoop:
		for (; ins < source.size();) {
			PawnInstruction instruction = source.get(ins);
			switch (instruction.opCode) {
				case PROC:
					if (!hasBegun) {
						hasBegun = true;
						//fall through
					} else {
						break MainLoop;
						//return, finalize subroutine
					}
				default:
					instructions.add(instruction);
					ins++;
					break;
			}
		}
	}

	public PawnSubroutine(int ptr) {
		this.name = "sub_" + Integer.toHexString(ptr);
		this.originalPtr = ptr;
	}

	public static PawnSubroutine fromCode(int pointer, int cellSize, LineReader code) {
		return fromCode(pointer, cellSize, code, true);
	}

	public static PawnSubroutine fromCode(int pointer, int cellSize, LineReader code, boolean doOutput) {
		PawnSubroutine ret = new PawnSubroutine(pointer);
		ret.originalPtr = pointer;
		String line;
		int ptr = ret.originalPtr;
		while (code.hasNextLine() && !StringEx.deleteAllChars(code.nextLine(), '\t').equals("{")) {
			//await subroutine beginning
		}
		if (!code.hasNextLine()) {
			return null;
		}
		if (doOutput) {
			System.out.println("[INFO] Found code, parsing instructions.");
		}
		while (code.hasNextLine() && !(line = StringEx.deleteAllChars(code.nextLine(), '\t')).equals("}")) {
			if (line.length() == 0) {
				continue;
			}
			PawnInstruction newIns = PawnInstruction.fromString(ptr, line, cellSize, doOutput);
			newIns.checkJmpConvertArgs();
			if (newIns.opCode == PawnOpCode.CASETBL) {
				newIns = caseTblFromString(newIns.pointer, code);
			}
			if (newIns.opCode != PawnOpCode.NONE) {
				ret.instructions.add(newIns);
				if (!newIns.opCode.packed) {
					ptr += newIns.getArgumentCount() * cellSize;
				}
				ptr += cellSize;
			}
		}
		ret.lastPtr = ptr;
		if (doOutput) {
			System.out.println("[INFO] Done parsing instructions for " + ret.name + ", found " + ret.instructions.size() + " valid instructions.");
		}
		return ret;
	}

	public static PawnInstruction caseTblFromString(int ptr, LineReader code) {
		PawnInstruction newIns = new PawnInstruction(PawnOpCode.CASETBL);
		newIns.pointer = ptr;
		while (!code.nextLine().replaceAll("\t", "").equals("{")) {
			//await casetbl beginning
		}
		Map<Integer, Integer> cases = new HashMap<>();
		int defaultCaseJmp = 0;
		String line;
		while (!(line = code.nextLine().replaceAll("\t", "")).equals("}")) {
			if (line.startsWith("*")) {
				int chara = line.lastIndexOf("=>") + 2;
				defaultCaseJmp = Integer.parseInt(line.substring(chara).trim().replaceAll("x", ""), 16);
			} else {
				if (line.contains("=>")) {
					try {
						int firstGap = line.trim().indexOf(' ');
						int id = Integer.parseInt(line.trim().substring(0, firstGap));
						int chara = line.lastIndexOf("=>") + 2;
						if (line.length() > chara) {
							int caseJmp = Integer.parseInt(line.substring(chara).trim().replaceAll("0x", ""), 16);
							cases.put(id, caseJmp);
						}
					} catch (NumberFormatException e) {

					}
				}
			}
			//await casetbl end
		}
		List<Map.Entry<Integer, Integer>> l = new ArrayList<>();
		l.addAll(cases.entrySet());
		Collections.sort(l, (Map.Entry<Integer, Integer> o1, Map.Entry<Integer, Integer> o2) -> o1.getKey() - o2.getKey());

		newIns.arguments = new long[cases.size() * 2 + 2];
		newIns.arguments[0] = cases.size();
		int idx = 2;
		for (Map.Entry e : l) {
			newIns.arguments[idx + 1] = (Integer) e.getValue() - (ptr + idx * 4) - 4;
			newIns.arguments[idx] = (Integer) e.getKey();
			idx += 2;
		}
		newIns.arguments[1] = defaultCaseJmp - (ptr + 4);
		return newIns;
	}

	public int getInstructionCount() {
		return instructions.size();
	}

	public List<String> getAllInstructionStrings(int indentLevel) {
		StringBuilder indentator = new StringBuilder();
		if (indentLevel != -1) {
			for (int j = 0; j < indentLevel; j++) {
				indentator.append("\t");
			}
		}
		String id = indentator.toString();
		List<String> ret = new ArrayList<>();
		for (int i = 0; i < instructions.size(); i++) {
			StringBuilder sb = new StringBuilder();

			sb.append(indentator);
			sb.append(StringEx.replaceFast(instructions.get(i).toString(), "\n", "\n" + id));
			ret.add(sb.toString());
		}
		return ret;
	}
}
