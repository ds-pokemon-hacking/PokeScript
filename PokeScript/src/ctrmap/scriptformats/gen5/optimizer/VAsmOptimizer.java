package ctrmap.scriptformats.gen5.optimizer;

import ctrmap.pokescript.instructions.gen5.VConstants;
import ctrmap.pokescript.instructions.gen5.VOpCode;
import ctrmap.pokescript.instructions.gen5.VStackCmpOpRequest;
import ctrmap.pokescript.instructions.ntr.NTRInstructionCall;
import ctrmap.scriptformats.gen5.VScriptFile;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VAsmOptimizer {

	public static final Pattern[][] PTNS = new Pattern[][]{
		//Optimization level 1 - variable calls to constant calls
		new Pattern[]{
			new Pattern("SetOpCode 0 VarUpdateConst",
			new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "VarUpdateVar(VARIABLE, CONSTANT)")
			),
			new Pattern("SetOpCode 0 PushConst",
			new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "PushVar(CONSTANT)")
			)
		},
		//Optimization level 2 - primary reg ops to direct ops
		new Pattern[]{
			new Pattern("GetArg TargetVar 1 0; SetArg 0 0 TargetVar; RemapAll 0; Delete 1",
			new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "VarUpdateConst(ANY_GPR, CONSTANT)"),
			new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "VarUpdateVar(VARIABLE, ANY_GPR)")
			),
			new Pattern("GetArg SrcVar 0 1; SetArg 1 0 SrcVar; RemapAll 1; Delete 0",
			new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "VarUpdateVar(ANY_GPR, VARIABLE)"),
			new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "PushVar(ANY_GPR)")
			),
			new Pattern("SetOpCode 1 PushConst; GetArg SrcVar 0 1; SetArg 1 0 SrcVar; RemapAll 1; Delete 0",
			new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "VarUpdateConst(ANY_GPR, CONSTANT)"),
			new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "PushVar(ANY_GPR)")
			)
		},
		//Optimization level 3 - primary reg ops to direct ops

		new Pattern[]{
			new Pattern("GetArg VarTgt 2 0; GetArg ConstSrc 0 0; MakeIns SetVar VarUpdateConst VarTgt ConstSrc; Remap 0 SetVar; Remap 2 SetVar; Append SetVar; Delete 0; Delete 2",
			new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "PushConst(CONSTANT)"),
			new VInstructionMatch(VInstructionMatch.InstructionMatchType.NOT_OPCODE, "PushVar|PushConst|CmpPriAlt"),
			new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "PopToVar(VARIABLE)")
			),
			new Pattern("GetArg VarTgt 2 0; GetArg VarSrc 0 0; MakeIns SetVar VarUpdateVar VarTgt VarSrc; Remap 0 SetVar; Remap 2 SetVar; Append SetVar; Delete 0; Delete 2",
			new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "PushVar(VARIABLE)"),
			new VInstructionMatch(VInstructionMatch.InstructionMatchType.NOT_OPCODE, "PushVar|PushConst|CmpPriAlt"),
			new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "PopToVar(VARIABLE)")
			),
			new Pattern("GetArg ConstVal 0 1; SetArg 2 0 ConstVal; SetOpCode 2 PushConst; Remap 0 1; Delete 0",
			new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "VarUpdateConst(PRIMARY_GPR, CONSTANT)"),
			new VInstructionMatch(VInstructionMatch.InstructionMatchType.NOT_FULL, "VarUpdateVar|VarUpdateConst(PRIMARY_GPR, ANY)"),
			new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "PushVar(PRIMARY_GPR)")
			),
			new Pattern("GetArg VarSrc 0 1; SetArg 2 0 VarSrc; Remap 0 1; Delete 0",
			new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "VarUpdateVar(PRIMARY_GPR, VARIABLE)"),
			new VInstructionMatch(VInstructionMatch.InstructionMatchType.NOT_FULL, "VarUpdateVar|VarUpdateConst(PRIMARY_GPR, ANY)"),
			new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "PushVar(PRIMARY_GPR)")
			),
			new Pattern("GetArg ConstVal 0 1; SetArg 2 0 ConstVal; SetOpCode 2 PushConst; Remap 0 1; Delete 0",
			new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "VarUpdateConst(ALTERNATIVE_GPR, CONSTANT)"),
			new VInstructionMatch(VInstructionMatch.InstructionMatchType.NOT_FULL, "VarUpdateVar|VarUpdateConst(ALTERNATIVE_GPR, ANY)"),
			new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "PushVar(ALTERNATIVE_GPR)")
			),
			new Pattern("GetArg ConstVal 0 1; SetArg 2 1 ConstVal; Remap 0 1; Delete 0",
			new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "VarUpdateConst(ALTERNATIVE_GPR, CONSTANT)"),
			new VInstructionMatch(VInstructionMatch.InstructionMatchType.NOT_FULL, "VarUpdateVar|VarUpdateConst(ALTERNATIVE_GPR, ANY)"),
			new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "VarUpdateAdd|VarUpdateSub|VarUpdateMul|VarUpdateDiv|VarUpdateMod|VarUpdateOR|VarUpdateAND(ANY, ALTERNATIVE_GPR)")
			),
		},
		
		//Optimization level 4 - direct arithmetic
		new Pattern[]{
			new Pattern("GetArg VarTgt 0 1; GetArg ChkVarTgt 2 0; Cmp VarTgt ChkVarTgt; GetArg Operand 1 1; SetOpCode 2 VarUpdateAdd; SetArg 2 1 Operand; RemapAll 2; Delete 0; Delete 1",
			new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "VarUpdateVar(PRIMARY_GPR, VARIABLE)"),
			new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "VarUpdateAdd|VarUpdateSub|VarUpdateMul|VarUpdateDiv|VarUpdateMod|VarUpdateOR|VarUpdateAND(PRIMARY_GPR, ANY)"),
			new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "VarUpdateVar(VARIABLE, PRIMARY_GPR)")
			)
		},
		new Pattern[]{
			new Pattern("GetArg LHS 1 0; GetArg RHS 0 0; GetArg CmpType 2 0; ExecSpecial MakeSimpleCmp; SetArg 5 0 CmpType; SetArg 4 0 LHS; SetArg 4 1 RHS; ExecSpecial SetCorrectCmpVar 4 RHS; Remap 0 4; Remap 1 4; Remap 2 4; Remap 3 4; Delete 0; Delete 1; Delete 2; Delete 3;",
			new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "PushVar|PushConst(ANY)"), //0
			new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "PushVar|PushConst(ANY)"), //1
			new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "CmpPriAlt(ANY)"),         //2
			new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "PopToVar(ANY_GPR)"),      //3
			new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "CmpVarConst(ANY_GPR, ZERO)"), //4
			new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "JumpOnCmp(ONE, ANY)")         //5
			)
		},
	};
	public static final int[] PTN_ITER_COUNT = new int[]{1, 2, 1, 1, 1};

	public static void optimize(VScriptFile scrFile, int optimizationLevel) {
		if (optimizationLevel > PTNS.length) {
			System.err.println("Optimization level " + optimizationLevel + " too high! Sanitizing to " + PTNS.length);
			optimizationLevel = PTNS.length;
		}
		for (int lvl = 0; lvl < optimizationLevel; lvl++) {
			for (int iter = 0; iter < PTN_ITER_COUNT[lvl]; iter++) {
				for (int i = 0; i < scrFile.instructions.size(); i++) {
					for (Pattern p : PTNS[lvl]) {
						if (p.match(scrFile.instructions, i)) {
							p.optimize(scrFile.instructions, i);
							i--;
							break;
						}
					}
				}
			}
		}
		scrFile.updateLinks();
	}

	public static class Pattern {

		private VInstructionMatch[] matches;
		private String[] code;

		public Pattern(String optimizationCode, VInstructionMatch... matches) {
			this.matches = matches;
			String[] codeSrc = optimizationCode.split(";");
			code = new String[codeSrc.length];
			for (int i = 0; i < codeSrc.length; i++) {
				code[i] = codeSrc[i].trim();
			}
		}

		public boolean optimize(List<NTRInstructionCall> list, int offset) {
			Map<String, NTRInstructionCall> instructionBuffer = new HashMap<>();
			Map<String, Integer> numberBuffer = new HashMap<>();

			for (int i = 0; i < matches.length; i++) {
				instructionBuffer.put(String.valueOf(i), list.get(offset + i));
			}

			boolean lenChg = false;

			Outer:
			for (String c : code) {
				String[] src = c.split("\\s+");
				String cmd = src[0];
				String[] args = Arrays.copyOfRange(src, 1, src.length);

				switch (cmd) {
					case "GetArg":
						numberBuffer.put(args[0], instructionBuffer.get(args[1]).args[Integer.parseInt(args[2])]);
						break;
					case "SetArg":
						instructionBuffer.get(args[0]).args[Integer.parseInt(args[1])] = numberBuffer.get(args[2]);
						break;
					case "Cmp":
						if (!numberBuffer.get(args[0]).equals(numberBuffer.get(args[1]))){
							break Outer;
						}
						break;
					case "Delete":
						list.remove(instructionBuffer.get(args[0]));
						instructionBuffer.remove(args[0]);
						lenChg = true;
						break;
					case "MakeIns": {
						String name = args[0];
						VOpCode opCode = VOpCode.parse(args[1]);
						if (opCode != null) {
							int[] funcArgs = new int[args.length - 2];
							for (int i = 2; i < args.length; i++) {
								if (numberBuffer.containsKey(args[i])) {
									funcArgs[i - 2] = numberBuffer.get(args[i]);
								} else {
									funcArgs[i - 2] = Integer.parseInt(args[i]);
								}
							}
							instructionBuffer.put(name, opCode.createCall(funcArgs));
						} else {
							System.err.println("WARN: Unrecognized OpCode: " + args[0]);
						}
						break;
					}
					case "SetOpCode":
						instructionBuffer.get(args[0]).definition = VOpCode.parse(args[1]).proto;
						break;
					case "Append":
						list.add(offset + matches.length, instructionBuffer.get(args[0]));
						break;
					case "Remap": {
						NTRInstructionCall source = instructionBuffer.get(args[0]);
						NTRInstructionCall target = instructionBuffer.get(args[1]);
						for (NTRInstructionCall call : list) {
							if (call.link != null) {
								if (call.link.target == source) {
									call.link.target = target;
								}
							}
						}
						break;
					}
					case "RemapAll": {
						NTRInstructionCall target = instructionBuffer.get(args[0]);
						Collection<NTRInstructionCall> vals = instructionBuffer.values();
						for (NTRInstructionCall call : list) {
							if (call.link != null) {
								if (vals.contains(call.link.target)) {
									call.link.target = target;
								}
							}
						}
						break;
					}
					case "ExecSpecial":
					{
						String behaviorType = args[0];
						
						switch (behaviorType){
							case "MakeSimpleCmp":
								int stkCmpType = numberBuffer.get("CmpType");
								if (stkCmpType <= 5){
									stkCmpType = VStackCmpOpRequest.getVmCmpReqForStkCmpReq(stkCmpType);
									numberBuffer.put("CmpType", stkCmpType);
								}
								else {
									break Outer; //This is an AND or OR, thus can not be performed outside of the stack
								}
								
								break;
							case "SetCorrectCmpVar":
								NTRInstructionCall ins = instructionBuffer.get(args[1]);
								int rhs = numberBuffer.get(args[2]);
								
								if (rhs < VConstants.WKVAL_START){
									ins.definition = VOpCode.CmpVarConst.proto;
								}
								else {
									ins.definition = VOpCode.CmpVarVar.proto;
								}
								
								break;
							default:
								System.err.println("WARN: Unknown SpBhv " + behaviorType);
						}
						
						break;
					}
					default:
						System.err.println("WARN: Unrecognized command: " + cmd);
						break;
				}
			}

			return lenChg;
		}

		public boolean match(List<NTRInstructionCall> list, int offset) {
			if (offset + matches.length >= list.size()) {
				return false;
			}
			for (int i = 0; i < matches.length; i++) {
				if (!matches[i].match(list.get(offset + i))) {
					return false;
				}
			}
			return true;
		}
	}
}
