package ctrmap.scriptformats.gen5.optimizer;

import ctrmap.pokescript.instructions.gen5.VCmpResultRequest;
import ctrmap.pokescript.instructions.gen5.VConstants;
import ctrmap.pokescript.instructions.gen5.VOpCode;
import ctrmap.pokescript.instructions.gen5.VStackCmpOpRequest;
import ctrmap.pokescript.instructions.ntr.NTRInstructionCall;
import ctrmap.pokescript.instructions.ntr.NTRInstructionLink;
import ctrmap.scriptformats.gen5.VScriptFile;
import xstandard.text.StringEx;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VAsmOptimizer {

	//Optimization level 1 - variable calls to constant calls, remove useless jumps
	public static final Pattern[] OPTFUNC_STREAMLINE_CALLS
		= new Pattern[]{
			new Pattern("SetOpCode 0 VarUpdateConst",
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "VarUpdateVar(VARIABLE, CONSTANT)")
			),
			new Pattern("SetOpCode 0 PushConst",
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "PushVar(CONSTANT)")
			),
			new Pattern("Remap 0 1; Delete 0",
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "Jump(ZERO)"),
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.ANY, null)
			),
			new Pattern("GetArg Var1 0 0; GetArg Var2 0 1; Cmp Var1 Var2; Remap 0 1; Delete 0",
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "VarUpdateVar(VARIABLE, VARIABLE)"),
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.ANY, null)
			)
		};

	//Optimization level 2 - primary reg ops to direct ops
	public static final Pattern[] OPTFUNC_REGISTER_TO_DIRECT
		= new Pattern[]{
			new Pattern("GetArg TargetVar 1 0; SetArg 0 0 TargetVar; RemapAll 0; Delete 1",
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "VarUpdateConst(ANY_GPR, CONSTANT)"),
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "VarUpdateVar(VARIABLE, ANY_GPR)")
			),
			new Pattern("GetArg Target 0 1; SetArg 1 1 Target; Remap 0 1; Delete 0",
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "VarUpdateVar(PRIMARY_GPR, ANY)"),
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "VarUpdateVar(VARIABLE, PRIMARY_GPR)")
			),
			new Pattern("GetArg SrcVar 0 1; SetArg 1 0 SrcVar; RemapAll 1; Delete 0",
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "VarUpdateVar(ANY_GPR, VARIABLE)"),
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "PushVar(ANY_GPR)")
			),
			new Pattern("SetOpCode 1 PushConst; GetArg SrcVar 0 1; SetArg 1 0 SrcVar; RemapAll 1; Delete 0",
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "VarUpdateConst(ANY_GPR, CONSTANT)"),
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "PushVar(ANY_GPR)")
			)
		};

	//Optimization level 3 - primary reg ops to direct ops
	public static final Pattern[] OPTFUNC_STACK_TO_DIRECT
		= new Pattern[]{
			new Pattern("GetArg VarTgt 2 0; GetArg ConstSrc 0 0; MakeIns SetVar VarUpdateConst VarTgt ConstSrc; Remap 0 SetVar; Remap 2 SetVar; Append SetVar; Delete 0; Delete 2",
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "PushConst(CONSTANT)"),
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.NOT_OPCODE, "PushVar|PushConst|CmpPriAlt|Call|CallIf|GlobalCall|GlobalCallAsync"),
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "PopToVar(ANY_GPR)")
			),
			new Pattern("GetArg VarTgt 2 0; GetArg VarSrc 0 0; MakeIns SetVar VarUpdateVar VarTgt VarSrc; Remap 0 1; Remap 2 1; Append SetVar; Delete 0; Delete 2",
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "PushVar(VARIABLE)"),
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.NOT_OPCODE, "PushVar|PushConst|CmpPriAlt|Call|CallIf|GlobalCall|GlobalCallAsync"),
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "PopToVar(ANY_GPR)")
			),
			new Pattern("GetArg ConstVal 0 1; SetArg 2 0 ConstVal; SetOpCode 2 PushConst; Remap 0 1; Delete 0",
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "VarUpdateConst(PRIMARY_GPR, CONSTANT)"),
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.NOT_FULL, "ANY_VAR_UPDATE(PRIMARY_GPR, ANY)"),
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "PushVar(PRIMARY_GPR)")
			),
			new Pattern("GetArg VarSrc 0 1; SetArg 2 0 VarSrc; Remap 0 1; Delete 0",
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "VarUpdateVar(PRIMARY_GPR, VARIABLE)"),
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.NOT_FULL, "ANY_VAR_UPDATE(PRIMARY_GPR, ANY)"),
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "PushVar(PRIMARY_GPR)")
			),
			new Pattern("GetArg ConstVal 0 1; SetArg 2 0 ConstVal; SetOpCode 2 PushConst; Remap 0 1; Delete 0",
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "VarUpdateConst(ALTERNATIVE_GPR, CONSTANT)"),
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.NOT_FULL, "ANY_VAR_UPDATE(ALTERNATIVE_GPR, ANY)"),
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "PushVar(ALTERNATIVE_GPR)")
			)
		};

	//Optimization level 4 - direct arithmetic
	public static final Pattern[] OPTFUNC_DIRECT_ARITHMETICS
		= new Pattern[]{
			new Pattern("GetArg ConstVal 0 1; SetArg 2 1 ConstVal; Remap 0 1; Delete 0",
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "VarUpdateConst(ALTERNATIVE_GPR, CONSTANT)"),
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.NOT_FULL, "ANY_VAR_UPDATE(ALTERNATIVE_GPR, ANY)"),
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "MATH_VAR_UPDATE(ANY, ALTERNATIVE_GPR)")
			),
			new Pattern("GetArg Op1 0 1; SetArg 2 1 Op1; Remap 0 1; Delete 0",
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "VarUpdateVar|VarUpdateConst(ALTERNATIVE_GPR, ANY)"),
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "VarUpdateVar|VarUpdateConst(PRIMARY_GPR, ANY)"),
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "MATH_VAR_UPDATE(PRIMARY_GPR, ALTERNATIVE_GPR)")
			),
			new Pattern("GetArg VarTgt 0 1; GetArg ChkVarTgt 2 0; Cmp VarTgt ChkVarTgt; GetArg Operand 1 1; GetOpCode OpCode 1; SetOpCode 2 OpCode; SetArg 2 1 Operand; RemapAll 2; Delete 0; Delete 1",
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "VarUpdateVar(PRIMARY_GPR, VARIABLE)"),
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "MATH_VAR_UPDATE(PRIMARY_GPR, ANY)"),
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "VarUpdateVar(VARIABLE, PRIMARY_GPR)")
			),
			new Pattern("GetArg Target1 1 0; GetArg Source2 2 1; Cmp Target1 Source2; GetArg Target0 0 0; Cmp Target1 Target0; GetArg VarTgt 2 0; SetArg 0 0 VarTgt; SetArg 1 0 VarTgt; Remap 2 1; Delete 2;",
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "VarUpdateVar|VarUpdateConst(ANY_GPR, ANY)"),
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "MATH_VAR_UPDATE(ANY_GPR, ANY)"),
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "VarUpdateVar(VARIABLE, ANY_GPR)")
			)
		};

	//Optimization level 5 - stack comparisons to normal comparisons
	public static final Pattern[] OPTFUNC_COMPARISONS_STACK_TO_REG
		= new Pattern[]{
			new Pattern("GetArg LHS 0 0; GetArg RHS 1 0; GetArg CmpType 2 0; ExecSpecial MakeSimpleCmp; SetArg 5 0 CmpType; SetArg 4 0 LHS; SetArg 4 1 RHS; ExecSpecial SetCorrectCmpVar 4 RHS; Remap 0 4; Remap 1 4; Remap 2 4; Remap 3 4; Delete 0; Delete 1; Delete 2; Delete 3;",
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "PushVar|PushConst(ANY)"), //0
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "PushVar|PushConst(ANY)"), //1
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "CmpPriAlt(ANY)"), //2
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "PopToVar(ANY_GPR)"), //3
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "CmpVarConst(ANY_GPR, ZERO)"), //4
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "JumpIf(ONE, ANY)") //5
			)
		};

	//Optimization level 6 - register comparisons to direct comparisons
	public static final Pattern[] OPTFUNC_COMPARISONS_REG_TO_DIRECT
		= new Pattern[]{
			new Pattern("GetArg CmpLHS 2 0; GetArg CmpRHS 2 1; GetArg In1 0 0; GetArg In2 1 0; GetArg In1Val 0 1; GetArg In2Val 1 1; ExecSpecial TryMakeDirectCmp 2 0 1; RemapAll 2; Delete 0; Delete 1",
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "VarUpdateVar|VarUpdateConst(ANY_GPR, ANY)"),
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "VarUpdateVar|VarUpdateConst(ANY_GPR, ANY)"),
				new VInstructionMatch(VInstructionMatch.InstructionMatchType.FULL, "CmpVarVar(ANY_GPR, ANY_GPR)")
			)
		};

	public static final Pattern[][] OPTIMIZATION_TABLE = new Pattern[][]{
		OPTFUNC_STREAMLINE_CALLS,
		OPTFUNC_REGISTER_TO_DIRECT,
		OPTFUNC_REGISTER_TO_DIRECT,
		OPTFUNC_STACK_TO_DIRECT,
		OPTFUNC_DIRECT_ARITHMETICS,
		OPTFUNC_DIRECT_ARITHMETICS,
		OPTFUNC_COMPARISONS_STACK_TO_REG,
		OPTFUNC_COMPARISONS_REG_TO_DIRECT
	};

	public static void optimize(VScriptFile scrFile, int optimizationLevel) {
		if (optimizationLevel > OPTIMIZATION_TABLE.length) {
			System.err.println("Optimization level " + optimizationLevel + " too high! Sanitizing to " + OPTIMIZATION_TABLE.length);
			optimizationLevel = OPTIMIZATION_TABLE.length;
		}

		for (int lvl = 0; lvl < optimizationLevel; lvl++) {
			for (int i = 0; i < scrFile.instructions.size(); i++) {
				//System.out.println("off now " + Integer.toHexString(scrFile.instructions.get(i).pointer) + " has " + scrFile.instructions.get(i));
				for (Pattern p : OPTIMIZATION_TABLE[lvl]) {
					if (p.match(scrFile.instructions, i)) {
						try {
							//System.out.println("optimizing with " + Arrays.toString(p.code));
							if (p.optimize(scrFile, i)) {
								i--;
								break;
							}
						} catch (Exception ex) {
							throw new RuntimeException("Error when processing pattern " + Arrays.toString(p.code), ex);
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
			String[] codeSrc = StringEx.splitOnecharFastNoBlank(optimizationCode, ';');
			code = new String[codeSrc.length];
			for (int i = 0; i < codeSrc.length; i++) {
				code[i] = codeSrc[i].trim();
			}
		}

		public boolean optimize(VScriptFile script, int offset) {
			List<NTRInstructionCall> list = script.instructions;
			Map<String, NTRInstructionCall> instructionBuffer = new HashMap<>();
			Map<String, Integer> numberBuffer = new HashMap<>();

			for (int i = 0; i < matches.length; i++) {
				instructionBuffer.put(String.valueOf(i), list.get(offset + i));
			}

			Outer:
			for (String c : code) {
				String[] src = StringEx.splitOnecharFastNoBlank(c, ' ');
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
						if (!numberBuffer.get(args[0]).equals(numberBuffer.get(args[1]))) {
							return false;
						}
						break;
					case "Delete":
						if (list.remove(instructionBuffer.get(args[0]))) {
							instructionBuffer.remove(args[0]);
						} else {
							throw new RuntimeException("Failed to remove " + instructionBuffer.get(args[0]) + " (label " + args[0] + ")");
						}
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
					case "GetOpCode":
						numberBuffer.put(args[0], instructionBuffer.get(args[1]).definition.opCode);
						break;
					case "SetOpCode":
						int num = numberBuffer.getOrDefault(args[1], -1);
						if (num != -1) {
							instructionBuffer.get(args[0]).definition = VOpCode.parse(num).proto;
						} else {
							instructionBuffer.get(args[0]).definition = VOpCode.parse(args[1]).proto;
						}
						break;
					case "Append":
						list.add(offset + matches.length, instructionBuffer.get(args[0]));
						break;
					case "Remap": {
						NTRInstructionCall source = instructionBuffer.get(args[0]);
						NTRInstructionCall target = instructionBuffer.get(args[1]);
						for (NTRInstructionCall call : list) {
							tryRelink(call, source, target);
						}
						for (NTRInstructionLink pub : script.publics) {
							if (pub.target == source) {
								pub.target = target;
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
						for (NTRInstructionLink pub : script.publics) {
							if (vals.contains(pub.target)) {
								pub.target = target;
							}
						}
						break;
					}
					case "ExecSpecial": {
						String behaviorType = args[0];

						switch (behaviorType) {
							case "MakeSimpleCmp":
								int stkCmpType = numberBuffer.get("CmpType");
								if (stkCmpType <= 5) {
									stkCmpType = VStackCmpOpRequest.getVmCmpReqForStkCmpReq(stkCmpType);
									numberBuffer.put("CmpType", stkCmpType);
								} else {
									break Outer; //This is an AND or OR, thus can not be performed outside of the stack
								}

								break;
							case "SetCorrectCmpVar":
								NTRInstructionCall ins = instructionBuffer.get(args[1]);
								int rhs = numberBuffer.get(args[2]);

								if (rhs < VConstants.WKVAL_START) {
									ins.definition = VOpCode.CmpVarConst.proto;
								} else {
									ins.definition = VOpCode.CmpVarVar.proto;
								}

								break;
							case "TryMakeDirectCmp": {
								int cmpLhs = numberBuffer.get("CmpLHS");
								int cmpRhs = numberBuffer.get("CmpRHS");

								int param1 = numberBuffer.get("In1");
								int param2 = numberBuffer.get("In2");

								if ((cmpLhs == param1 && cmpRhs == param2) || (cmpLhs == param2 && cmpRhs == param1)) {
									boolean isP1Const = instructionBuffer.get(args[2]).definition.opCode == VOpCode.VarUpdateConst.ordinal();
									boolean isP2Const = instructionBuffer.get(args[3]).definition.opCode == VOpCode.VarUpdateConst.ordinal();
									NTRInstructionCall cmpIns = instructionBuffer.get(args[1]);
									boolean isLHSParam1 = cmpLhs == param1;
									//System.out.println("p1c " + isP1Const + " p2c " + isP2Const + " islhs1 " + isLHSParam1);
									if (!(isP1Const || isP2Const) || (isLHSParam1 && !isP1Const) || (!isLHSParam1 && !isP2Const)) {
										int p1Val = numberBuffer.get("In1Val");
										int p2Val = numberBuffer.get("In2Val");
										cmpIns.args[0] = isLHSParam1 ? p1Val : p2Val;
										cmpIns.args[1] = isLHSParam1 ? p2Val : p1Val;
										if (isP1Const || isP2Const) {
											cmpIns.definition = VOpCode.CmpVarConst.proto;
										}
										break;
									}
								}
								return false;
							}
							default:
								System.err.println("WARN: Unknown SpBhv " + behaviorType);
								break;
						}

						break;
					}
					default:
						System.err.println("WARN: Unrecognized command: " + cmd);
						break;
				}
			}

			return true;
		}

		private void tryRelink(NTRInstructionCall call, NTRInstructionCall orgTgt, NTRInstructionCall newTgt) {
			if (call.link != null) {
				if (call.link.target == orgTgt) {
					call.link.target = newTgt;
				}
			}
		}

		public boolean match(List<NTRInstructionCall> list, int offset) {
			if (offset + matches.length > list.size()) {
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
