package ctrmap.scriptformats.gen5.optimizer;

import ctrmap.pokescript.instructions.gen5.VOpCode;
import ctrmap.pokescript.instructions.ntr.NTRInstructionCall;
import xstandard.text.StringEx;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VInstructionMatch {

	private static final Map<String, VOpCode[]> OPCODE_MACROS = new HashMap<>();

	static {
		OPCODE_MACROS.put("ANY_VAR_UPDATE", new VOpCode[]{
			VOpCode.VarUpdateVar,
			VOpCode.VarUpdateConst,
			VOpCode.VarUpdateFlex,
			VOpCode.VarUpdateAdd,
			VOpCode.VarUpdateSub,
			VOpCode.VarUpdateMul,
			VOpCode.VarUpdateDiv,
			VOpCode.VarUpdateMod,
			VOpCode.VarUpdateAND,
			VOpCode.VarUpdateOR
		});
		OPCODE_MACROS.put("MATH_VAR_UPDATE", new VOpCode[]{
			VOpCode.VarUpdateAdd,
			VOpCode.VarUpdateSub,
			VOpCode.VarUpdateMul,
			VOpCode.VarUpdateDiv,
			VOpCode.VarUpdateMod,
			VOpCode.VarUpdateAND,
			VOpCode.VarUpdateOR
		});
	}

	public final List<Integer> opCodes = new ArrayList<>();
	public final InstructionMatchType matchType;

	public VOpCodeArgMatch[] argMatch = new VOpCodeArgMatch[0];

	public VInstructionMatch(InstructionMatchType matchType, String source) {
		this.matchType = matchType;
		if (source != null) {
			String op = source;
			int idxPrts = source.indexOf('(');
			if (idxPrts != -1) {
				op = source.substring(0, idxPrts);
			}
			String[] ops = StringEx.splitOnecharFastNoBlank(op, '|');

			for (String opC : ops) {
				VOpCode[] macro = OPCODE_MACROS.get(opC);

				if (macro != null) {
					for (VOpCode opcode : macro) {
						opCodes.add(opcode.ordinal());
					}
				} else {
					VOpCode opCode = VOpCode.parse(opC);

					if (opCode == null) {
						System.err.println("WARN: Invalid opcode: " + opC);
					} else {
						opCodes.add(opCode.ordinal());
					}
				}
			}

			if (idxPrts != -1) {
				String[] args = StringEx.splitOnecharFastNoBlank(source.substring(idxPrts + 1, source.lastIndexOf(')')), ',');
				argMatch = new VOpCodeArgMatch[args.length];

				for (int i = 0; i < argMatch.length; i++) {
					if (i >= args.length) {
						break;
					}
					argMatch[i] = VOpCodeArgMatch.valueOf(args[i].trim().toUpperCase());
				}
				for (int i = 0; i < argMatch.length; i++) {
					if (argMatch[i] == null) {
						argMatch[i] = VOpCodeArgMatch.ANY;
					}
				}
			}
		}
	}

	public boolean match(NTRInstructionCall instruction) {
		switch (matchType) {
			case ANY:
				return true;
			case OPCODE_ONLY:
				return opCodes.contains(instruction.definition.opCode);
			case FULL:
			case NOT_FULL:
				boolean b = opCodes.contains(instruction.definition.opCode);
				if (b) {
					b = tryMatchArgs(instruction);
				}
				return b ^ matchType == InstructionMatchType.NOT_FULL;
			case ARGS_ONLY:
				return tryMatchArgs(instruction);
			case ARGS_ONLY_NONINTERNAL:
				if (instruction.definition.opCode > VOpCode.VarUpdateMod.ordinal()) {
					return tryMatchArgs(instruction);
				}
				return false;
			case NOT_OPCODE:
				return !opCodes.contains(instruction.definition.opCode);
		}
		return false;
	}

	private boolean tryMatchArgs(NTRInstructionCall instruction) {
		if (instruction.args.length == argMatch.length) {
			for (int i = 0; i < instruction.args.length; i++) {
				if (!argMatch[i].matches(instruction.args[i])) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	public enum InstructionMatchType {
		ANY,
		FULL,
		OPCODE_ONLY,
		ARGS_ONLY,
		ARGS_ONLY_NONINTERNAL,
		NOT_FULL,
		NOT_OPCODE
	}
}
