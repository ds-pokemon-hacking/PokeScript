package ctrmap.scriptformats.gen5.optimizer;

import ctrmap.pokescript.instructions.gen5.VOpCode;
import ctrmap.pokescript.instructions.ntr.NTRInstructionCall;
import java.util.ArrayList;
import java.util.List;

public class VInstructionMatch {

	public final List<Integer> opCodes = new ArrayList<>();
	public final InstructionMatchType matchType;

	public VOpCodeArgMatch[] argMatch = new VOpCodeArgMatch[0];

	public VInstructionMatch(InstructionMatchType matchType, String source) {
		this.matchType = matchType;
		if (source != null) {
			String op = source;
			if (source.contains("(")) {
				op = source.substring(0, source.indexOf('('));
			}
			String[] ops = op.split("\\|");

			for (String opC : ops) {
				VOpCode opCode = VOpCode.parse(opC);

				if (opCode == null) {
					System.err.println("WARN: Invalid opcode: " + opC);
				} else {
					opCodes.add(opCode.ordinal());
				}
			}

			if (source.contains("(")) {
				String[] args = source.substring(source.indexOf('(') + 1, source.lastIndexOf(')')).split(",");
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
		ARGS_ONLY,
		ARGS_ONLY_NONINTERNAL,
		NOT_FULL,
		NOT_OPCODE
	}
}
