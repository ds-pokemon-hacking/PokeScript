package ctrmap.scriptformats.gen6;

import xstandard.text.StringEx;
import java.util.ArrayList;
import java.util.List;

public class PawnDisassembler {

	public static List<PawnSubroutine> disassembleScript(GFLPawnScript scr) {
		List<PawnSubroutine> ret = new ArrayList<>();
		for (int ins = 0; ins < scr.instructions.size();) {
			PawnSubroutine newSub = new PawnSubroutine(ins, scr.instructions, scr);
			newSub.parent = scr;
			ret.add(newSub);
			ins += newSub.getInstructionCount();
		}
		return ret;
	}

	public static List<PawnSubroutine> assembleScript(String code, int cellSize, boolean doOutput) {
		if (doOutput) {
			System.out.println("[INFO] CTRMap Pawn assembler running");
			System.out.println("[INFO] Going to parse " + code.length() + " characters of input.");
		}
		long begin = System.currentTimeMillis();
		List<PawnSubroutine> ret = new ArrayList<>();
		LineReader scanner = new LineReader(code);
		int ptr = 0;
		while (scanner.hasNextLine()) {
			String line = StringEx.deleteAllChars(scanner.nextLine(), '\t');
			if (line.length() > 0) {
				if (line.startsWith("sub_")) {
					if (doOutput) {
						System.out.println("[INFO] Found subroutine " + line + " at pointer 0x" + Integer.toHexString(ptr).toUpperCase());
					}
					PawnSubroutine newSub = PawnSubroutine.fromCode(ptr, cellSize, scanner, doOutput);
					if (newSub != null) {
						ret.add(newSub);
						ptr = newSub.lastPtr;
					}
				}
			}
		}
		if (doOutput) {
			System.out.println("[INFO] All work done.");
			System.out.println("[INFO] The assembly has finished in " + (System.currentTimeMillis() - begin) / 1000f + " seconds.");
		}
		return ret;
	}
}
