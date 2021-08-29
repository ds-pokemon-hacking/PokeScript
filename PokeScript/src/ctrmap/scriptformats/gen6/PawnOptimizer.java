package ctrmap.scriptformats.gen6;

import ctrmap.stdlib.util.ArraysEx;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PawnOptimizer {

	public static OptimizePattern[] patterns = new OptimizePattern[]{
		new OptimizePattern(
		"newins pushc PUSH_C;getarg const 0 0;setarg const pushc 0;optimize pushc;append pushc;remapall pushc;deleteall",
		PawnOpCode.CONST_PRI, PawnOpCode.PUSH_PRI
		),
		new OptimizePattern(
		"newins consts CONST_S;getarg addr 1 0;setarg addr consts 0;getarg const 0 0;setarg const consts 1;optimize consts;append consts;remapall consts;deleteall",
		PawnOpCode.CONST_PRI, PawnOpCode.STOR_P_S_PRI
		),
		new OptimizePattern(
		"newins mulc SMUL_C;getarg value 0 0;setarg value mulc 0;optimize mulc;remap 0 1;delete 0;append mulc;remap 2 mulc;delete 2",
		PawnOpCode.CONST_ALT, null, PawnOpCode.SMUL
		),
		new OptimizePattern(
		"newins pushs PUSH_S;getarg load 0 0;setarg load pushs 0;optimize pushs;append pushs;remapall pushs;deleteall",
		PawnOpCode.LOAD_S_PRI, PawnOpCode.PUSH_PRI
		),
		new OptimizePattern(
		"newins push PUSH;getarg load 0 0;setarg load push 0;optimize push;append push;remapall push;deleteall",
		PawnOpCode.LOAD_PRI, PawnOpCode.PUSH_PRI
		),
		new OptimizePattern(
		"newins loadalt LOAD_ALT;getarg load 0 0;setarg load loadalt 0;optimize loadalt;append loadalt;remapall loadalt;deleteall",
		PawnOpCode.LOAD_PRI, PawnOpCode.MOVE_ALT
		),
		new OptimizePattern(
		"newins loadalt LOAD_S_ALT;getarg load 0 0;setarg load loadalt 0;optimize loadalt;append loadalt;remapall loadalt;deleteall",
		PawnOpCode.LOAD_S_PRI, PawnOpCode.MOVE_ALT
		),
		new OptimizePattern(
		"newins loadspri LOAD_S_PRI;getarg addr 0 0;setarg addr loadspri 0;optimize loadspri;remap 0 1;delete 0;append loadspri;remap 2 loadspri;delete 2",
		PawnOpCode.PUSH_S, null, PawnOpCode.PPRI
		),
		new OptimizePattern(
		"newins loadspri LOAD_S_PRI;getarg addr 0 0;setarg addr loadspri 0;optimize loadspri;append loadspri; remapall loadspri; deleteall",
		PawnOpCode.PUSH_S, PawnOpCode.PPRI
		),
		new OptimizePattern(
		"newins jzer JZER;jmptransfer 1 jzer;append jzer;remapall jzer;deleteall",
		PawnOpCode.NOT, PawnOpCode.JNZ
		),
		new OptimizePattern(
		"newins jnz JNZ;jmptransfer 1 jnz;append jnz;remapall jnz;deleteall",
		PawnOpCode.NOT, PawnOpCode.JZER
		),
		new OptimizePattern(
		"newins jsgeq JSGEQ;jmptransfer 1 jsgeq;append jsgeq;remapall jsgeq;deleteall",
		PawnOpCode.SLESS, PawnOpCode.JZER
		),
		new OptimizePattern(
		"newins jsgrtr JSGRTR;jmptransfer 1 jsgrtr;append jsgrtr;remapall jsgrtr;deleteall",
		PawnOpCode.SLEQ, PawnOpCode.JZER
		),
		new OptimizePattern(
		"getarg increase 1 0;isarg increase 1;getarg vari1 0 0; getarg vari2 3 0;cmpargs vari1 vari2; newins incs INC_S; setarg vari1 incs 0; append incs; remapall incs; deleteall",
		PawnOpCode.LOAD_S_PRI, PawnOpCode.CONST_ALT, PawnOpCode.ADD, PawnOpCode.STOR_S_PRI
		),
		new OptimizePattern(
		"getarg increase 0 0;isarg increase 1;getarg vari1 1 0; getarg vari2 3 0; cmpargs vari1 vari2; newins incs INC_S; setarg vari1 incs 0; append incs; remapall incs; deleteall",
		PawnOpCode.CONST_ALT, PawnOpCode.LOAD_S_PRI, PawnOpCode.ADD, PawnOpCode.STOR_S_PRI
		),
		new OptimizePattern(
		"getarg stack 0 0; isarg stack 0; remapallnext; deleteall",
		PawnOpCode.STACK
		),
		new OptimizePattern(
		"newins singleconst CONST_PRI;getarg positive 0 0;negate positive;setarg positive singleconst 0;optimize singleconst;append singleconst;remapall singleconst;deleteall",
		PawnOpCode.CONST_PRI, PawnOpCode.NEG
		),
		new OptimizePattern(
		"newins altconst CONST_ALT;getarg constarg 0 0;setarg constarg altconst 0;optimize altconst;append altconst;remapall altconst;deleteall",
		PawnOpCode.CONST_PRI, PawnOpCode.MOVE_ALT
		),
		new OptimizePattern(
		"getarg firstjump 0 0; getptr secondjumpptr 1; arg2ptr firstjump 0; cmpargs firstjump secondjumpptr; remapall 1; delete 0",
		PawnOpCode.JUMP, null
		),
		new OptimizePattern(
		"newins onezero ZERO_PRI;append onezero;remapall onezero;deleteall",
		PawnOpCode.ZERO_PRI, PawnOpCode.ZERO_PRI
		),
		//MACRO PUSHES, in this order so that the beefier ones have priority
		new OptimizePattern( //honestly, the other push5s aren't ever gonna be used since packed stuff is always more effective and allows up to 65535, which is a lot for the pawn stack
		"newins push5 PUSH5_C;getarg 0 0 0; getarg 1 1 0; getarg 2 2 0; getarg 3 3 0; getarg 4 4 0; "
		+ "setarg 0 push5 0; setarg 1 push5 1; setarg 2 push5 2; setarg 3 push5 3; setarg 4 push5 4; append push5; remapall push5; deleteall", true,
		PawnOpCode.PUSH_C, PawnOpCode.PUSH_C, PawnOpCode.PUSH_C, PawnOpCode.PUSH_C, PawnOpCode.PUSH_C
		),
		new OptimizePattern(
		"newins push4 PUSH4_C;getarg 0 0 0; getarg 1 1 0; getarg 2 2 0; getarg 3 3 0; "
		+ "setarg 0 push4 0; setarg 1 push4 1; setarg 2 push4 2; setarg 3 push4 3; append push4; remapall push4; deleteall", true,
		PawnOpCode.PUSH_C, PawnOpCode.PUSH_C, PawnOpCode.PUSH_C, PawnOpCode.PUSH_C
		),
		new OptimizePattern(
		"newins push3 PUSH3_C;getarg 0 0 0; getarg 1 1 0; getarg 2 2 0; "
		+ "setarg 0 push3 0; setarg 1 push3 1; setarg 2 push3 2; append push3; remapall push3; deleteall", true,
		PawnOpCode.PUSH_C, PawnOpCode.PUSH_C, PawnOpCode.PUSH_C
		),
		new OptimizePattern(
		"newins push2 PUSH2_C;getarg 0 0 0; getarg 1 1 0; "
		+ "setarg 0 push2 0; setarg 1 push2 1;append push2; remapall push2; deleteall", true,
		PawnOpCode.PUSH_C, PawnOpCode.PUSH_C
		)
	};

	public static void optimize(GFLPawnScript s) {
		s.setInstructionListeners();

		for (int i = 0; i < s.instructions.size(); i++) {
			boolean doAgainCheck = false;
			for (OptimizePattern p : patterns) {
				if (p.match(s.instructions, i)) {
					doAgainCheck |= p.optimize();
				}
			}
			if (doAgainCheck) {
				i--;
			}
		}

		s.callInstructionListeners();
	}

	public static class OptimizePattern {

		private PawnOpCode[] cmds;
		private String filter;

		private List<PawnInstruction> tgt;
		private int tgtPos;

		private boolean strict = false;

		public OptimizePattern(String filter, PawnOpCode... pattern) {
			this(filter, false, pattern);
		}

		public OptimizePattern(String filter, boolean strict, PawnOpCode... pattern) {
			this.filter = filter;
			this.strict = strict;
			this.cmds = pattern;
		}

		public boolean match(List<PawnInstruction> test, int position) {
			boolean yesno = false;
			if (position + cmds.length - 1 < test.size()) {
				for (int i = 0; i < cmds.length; i++) {
					if (cmds[i] == null) {
						yesno = true;
						continue;
					}
					PawnOpCode fullCmd = test.get(position + i).opCode;
					if (fullCmd == cmds[i] || (!strict && fullCmd == cmds[i].getPackedEquivalent())) {
						yesno = true;
					} else {
						yesno = false;
						break;
					}
				}
			}
			if (yesno) {
				tgt = test;
				tgtPos = position;
			}
			return yesno;
		}

		public boolean optimize() {
			Map<String, PawnInstruction> insBuf = new HashMap<>();
			Map<String, Long> numberBuf = new HashMap<>();

			for (int i = 0; i < cmds.length; i++) {
				insBuf.put(String.valueOf(i), tgt.get(tgtPos + i));
			}

			String[] ptn = filter.split(";");
			for (int i = 0; i < ptn.length; i++) {
				ptn[i] = ptn[i].trim();
			}

			int delInsNum = 0;
			boolean doAgainCheck = false;

			CmdLoop:
			for (String c : ptn) {
				String[] args = c.split(" ");
				switch (args[0]) {
					case "newins":
						String opcode = args[2];
						PawnOpCode opcodeC = null;
						for (PawnOpCode op : PawnOpCode.OPCODES) {
							if (op.toString().equals(opcode)) {
								opcodeC = op;
							}
						}
						insBuf.put(args[1], new PawnInstruction(opcodeC, false));
						break;
					case "getarg":
						PawnInstruction ins = insBuf.get(args[2]);
						numberBuf.put(args[1], ins.getArgument(Integer.parseInt(args[3])));
						break;
					case "setarg":
						insBuf.get(args[2]).arguments[Integer.parseInt(args[3])] = numberBuf.get(args[1]);
						break;
					case "arg2ptr":
						//syntax: arg id, ins num
						numberBuf.put(args[1], numberBuf.get(args[1]) + insBuf.get(args[2]).pointer);
						break;
					case "getptr":
						numberBuf.put(args[1], (long)insBuf.get(args[2]).pointer);
						break;
					case "append":
						//System.out.println("append " + insBuf.get(args[1]).getCommandE() + " after " + tgt.get(tgtPos + cmds.length - 1).getCommandE());
						tgt.add(tgtPos + cmds.length - delInsNum, insBuf.get(args[1]));
						break;
					case "optimize":
						insBuf.get(args[1]).optimizePacked();
						break;
					case "negate":
						numberBuf.replace(args[1], -numberBuf.get(args[1]));
						break;
					case "remapallnext":
						int remapPos = tgtPos + cmds.length;
						if (remapPos >= tgt.size()) {
							remapPos--;
						}
						remapInstructions(tgt, tgt.get(remapPos), insBuf.values());
						break;
					case "remapall": {
						PawnInstruction target = insBuf.get(args[1]);
						remapInstructions(tgt, target, insBuf.values());
						break;
					}
					case "remap": {
						PawnInstruction source = insBuf.get(args[1]);
						PawnInstruction target = insBuf.get(args[2]);
						remapInstructions(tgt, target, ArraysEx.asList(source));
						break;
					}
					case "jmptransfer":
						PawnInstruction source = insBuf.get(args[1]);
						PawnInstruction target = insBuf.get(args[2]);
						PawnInstruction.JumpListener sjl = source.jmpListeners.get(0);
						PawnInstruction.JumpListener tjl = new PawnInstruction.JumpListener(target);
						tjl.setParent(sjl.getParent());
						target.jmpListeners.add(tjl);
						break;
					case "isarg":
						if (numberBuf.get(args[1]) != Integer.parseInt(args[2])) {
							break CmdLoop;
						}
						break;
					case "cmpargs":
						if (!numberBuf.get(args[1]).equals(numberBuf.get(args[2]))) {
							break CmdLoop;
						}
						break;
					case "delete":
						tgt.remove(insBuf.get(args[1]));
						insBuf.remove(args[1]);
						doAgainCheck = true;
						delInsNum++;
						break;
					case "deleteall":
						for (int i = 0; i < cmds.length; i++) {
							//System.out.println("Optimized out " + tgt.get(tgtPos).getCommandE());
							tgt.remove(tgtPos);
						}
						insBuf.clear();
						delInsNum = cmds.length;
						doAgainCheck = true;
						break;
					default:
						System.err.println("UNKNOWN COMMAND " + args[0]);
						break;
				}
			}
			return doAgainCheck;
		}
	}

	private static void remapInstructions(List<PawnInstruction> tgt, PawnInstruction target, Collection<PawnInstruction> toRemap) {
		for (PawnInstruction i : tgt) {
			for (PawnInstruction.JumpListener jl : i.jmpListeners) {
				if (jl instanceof PawnInstruction.CaseListener) {
					PawnInstruction.CaseListener cl = (PawnInstruction.CaseListener) jl;
					if (toRemap.contains(cl.defaultTarget)) {
						cl.defaultTarget = target;
					}
					for (Map.Entry<Long, PawnInstruction> t : cl.targets.entrySet()) {
						if (toRemap.contains(t.getValue())) {
							t.setValue(target);
						}
					}
				} else {
					if (jl != null && toRemap.contains(jl.getParent())) {
						jl.setParent(target);
					}
				}
			}
		}
	}
}
