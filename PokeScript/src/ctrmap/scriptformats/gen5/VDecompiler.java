package ctrmap.scriptformats.gen5;

import ctrmap.pokescript.instructions.gen5.VCmpResultRequest;
import ctrmap.pokescript.instructions.gen5.VConstants;
import ctrmap.pokescript.instructions.gen5.VOpCode;
import ctrmap.pokescript.instructions.ntr.NTRArgument;
import ctrmap.pokescript.instructions.ntr.NTRDataType;
import ctrmap.pokescript.instructions.ntr.NTRInstructionLink;
import ctrmap.scriptformats.gen5.disasm.DisassembledCall;
import ctrmap.scriptformats.gen5.disasm.DisassembledMethod;
import ctrmap.scriptformats.gen5.disasm.MathCommands;
import ctrmap.scriptformats.gen5.disasm.MathMaker;
import ctrmap.scriptformats.gen5.disasm.StackCommands;
import ctrmap.scriptformats.gen5.disasm.StackTracker;
import ctrmap.scriptformats.gen5.disasm.VDisassembler;
import ctrmap.stdlib.fs.FSFile;
import ctrmap.stdlib.fs.FSUtil;
import ctrmap.stdlib.fs.accessors.DiskFile;
import ctrmap.stdlib.text.FormattingUtils;
import ctrmap.stdlib.io.util.IndentedPrintStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class VDecompiler {

	private VScriptFile scr;

	private VDisassembler disasm;

	private StackTracker stack = new StackTracker();
	private List<VExtern> externs = new ArrayList<>();
	private List<VInternFunc> detectedInterns = new ArrayList<>();

	public String overrideClassName = null;

	public VDecompiler(VScriptFile scr, VCommandDataBase cdb) {
		this.scr = scr;
		disasm = new VDisassembler(scr, cdb);
	}

	public static void main(String[] args) {
		FSFile scrFile = new DiskFile("D:\\_REWorkspace\\CTRMapProjects\\White2\\vfs\\data\\a\\0\\5\\6\\1240");
		FSFile cdbFile = new DiskFile("C:\\Users\\Čeněk\\eclipse-workspace\\BsYmlGen\\B2W2\\Base.yml");
		FSFile outFile = new DiskFile("D:\\_REWorkspace\\pokescript_genv\\decomp\\out.pks");

		VDecompiler dec = new VDecompiler(new VScriptFile(scrFile), new VCommandDataBase(cdbFile));
		dec.decompileToFile(outFile);
	}

	public void decompile() {
		disasm.disassemble();
		detectInternalMethodSignatures();
	}

	public void decompileToFile(FSFile fsf) {
		decompile();
		fsf.setBytes(dump().getBytes(StandardCharsets.UTF_8));
	}

	private void detectInternalMethodSignatures() {
		for (DisassembledMethod m : disasm.methods) {
			int idx = 0;
			for (DisassembledCall c : m.instructions) {
				if (c.command.type == VCommandDataBase.CommandType.CALL && !c.command.callsExtern) {
					if (c.link != null && c.link.target != null) {
						getCallParameters(idx, c.link.target.pointer, m, false, true);
					}
				}
				idx++;
			}
		}
	}

	public String dump() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		IndentedPrintStream out = new IndentedPrintStream(baos);

		List<String> imports = new ArrayList<>();

		imports.add("system.EventFlags");
		imports.add("system.EventWorks");

		for (DisassembledMethod m : disasm.methods) {
			for (DisassembledCall c : m.instructions) {
				if (c.command.isDecompPrintable()) {
					String imp = c.command.classPath;
					if (!imports.contains(imp)) {
						imports.add(imp);
					}
				}
			}
		}

		Collections.sort(imports);
		for (String imp : imports) {
			out.print("import ");
			out.print(imp);
			out.println(";");
		}

		if (!imports.isEmpty()) {
			out.println();
		}

		out.print("public class ");
		if (overrideClassName != null) {
			out.print(overrideClassName);
		} else {
			out.print(FormattingUtils.getStrWithoutNonAlphanumeric(FSUtil.getFileNameWithoutExtension(scr.getSourceFile().getName())));
		}
		out.println(" {");
		out.incrementIndentLevel();
		
		disasm.sortMethods();

		for (DisassembledMethod m : disasm.methods) {
			if (m.isPublic) {
				dumpMethod(m, out);
			}
		}

		for (DisassembledMethod m : disasm.methods) {
			if (!m.isPublic) {
				//publics are dumped explicitly to preserve order
				dumpMethod(m, out);
			}
		}

		for (DisassembledMethod m : disasm.actionSequences) {
			dumpActionSeq(m, out);
		}

		for (VExtern ext : externs) {
			out.print("static meta void Global");
			out.print(ext.SCRID);
			out.print("(");
			for (int i = 0; i < ext.paramCount; i++) {
				if (i != 0) {
					out.print(", ");
				}
				out.print("int a");
				out.print(i + 1);
			}
			out.print(") : VGlobalCall[");
			out.print(ext.SCRID);
			out.println("];");
		}

		List<Integer> actionOpCodes = new ArrayList<>();

		for (DisassembledMethod actionSeq : disasm.actionSequences) {
			for (DisassembledCall call : actionSeq.instructions) {
				if (!actionOpCodes.contains(call.definition.opCode)) {
					actionOpCodes.add(call.definition.opCode);
				}
			}
		}

		if (!actionOpCodes.isEmpty()) {
			out.println();
			Collections.sort(actionOpCodes);
			for (Integer mvmt : actionOpCodes) {
				out.print("static native void Action");
				out.print(FormattingUtils.getStrWithLeadingZeros(4, Integer.toHexString(mvmt)));
				out.print("(int amount) : 0x");
				out.print(Integer.toHexString(mvmt));
				out.println(";");
			}
		}

		out.decrementIndentLevel();
		out.println("}");

		out.close();
		return new String(baos.toByteArray());
	}

	private VInternFunc findInternByPtr(int ptr) {
		for (VInternFunc f : detectedInterns) {
			if (f.address == ptr) {
				return f;
			}
		}
		return null;
	}

	private void dumpMethod(DisassembledMethod m, IndentedPrintStream out) {
		List<Integer> paramRegisters = new ArrayList<>();

		if (m.isPublic) {
			out.print("public ");
		}
		out.print("static void ");
		out.print(m.getName());
		out.print("(");
		for (VInternFunc intern : detectedInterns) {
			if (intern.address == m.ptr) {
				for (int i = 0; i < intern.paramWorks.length; i++) {
					if (i != 0) {
						out.print(", ");
					}
					out.print("int v");
					out.print(intern.paramWorks[i] - VConstants.GP_REG_PRI);
					paramRegisters.add(intern.paramWorks[i]);
				}

				break;
			}
		}
		out.println(") {");
		out.incrementIndentLevel();

		List<Integer> usedRegisters = new ArrayList<>();
		int callIdx = 0;
		for (DisassembledCall call : m.instructions) {
			for (int i = 0; i < call.args.length; i++) {
				int av = call.args[i];
				NTRArgument ad = call.definition.parameters[i];

				if (ad.dataType == NTRDataType.VAR || ad.dataType == NTRDataType.FLEX) {
					if (VConstants.isHighWk(av)) {
						if (!findNextCallThroughPush(callIdx, av, m) || i > 0) {
							if (!usedRegisters.contains(av) && !paramRegisters.contains(av)) {
								usedRegisters.add(av);
							}
						}
					}
				}
				if (call.command.type == VCommandDataBase.CommandType.CALL && call.link != null) {
					if (call.link.target != null) {
						VInternFunc f = findInternByPtr(call.link.target.pointer);
						if (f != null) {
							if (getCallParameters(callIdx, call.link.target.pointer, m, false, true).size() < f.paramWorks.length) {
								for (int wk : f.paramWorks) {
									if (wk >= VConstants.GP_REG_PRI) {
										if (!usedRegisters.contains(wk) && !paramRegisters.contains(wk)) {
											usedRegisters.add(wk);
										}
									}
								}
							}
						}
					}
				}
			}
			callIdx++;
		}
		Collections.sort(usedRegisters);

		for (int r : usedRegisters) {
			out.print("int v");
			out.print(r - VConstants.GP_REG_PRI);
			out.println(";");
		}

		if (!usedRegisters.isEmpty()) {
			out.println();
		}

		for (int h = 0; h < m.instructions.size(); h++) {
			DisassembledCall call = m.instructions.get(h);

			handleInstruction(call, h, m, out);
		}

		out.decrementIndentLevel();
		out.println("}");

		out.println();
	}

	private boolean isJumpLabelUsed(DisassembledCall call, DisassembledMethod method) {
		for (DisassembledCall c : method.instructions) {
			if (c.link != null && c.link.target == call) {
				if (!call.ignoredLinks.contains(c.link)) {
					return true;
				} else {
					System.out.println("Label " + call.label + " used, but ignored.");
				}
			}
		}
		return false;
	}

	private boolean findNextCallThroughPush(int insIndex, int wk, DisassembledMethod method) {
		//Scrolls forward through var setters and stack pushes to a global call
		//If there is a non-set or push instruction, this will return false
		for (int i = insIndex; i < method.instructions.size(); i++) {
			VCommandDataBase.VCommand cmd = method.instructions.get(i).command;
			if (cmd.type == VCommandDataBase.CommandType.CALL) {
				if (cmd.callsExtern) {
					//check if the variable is peing pushed/popped on global calls
					//otherwise it's obviously not a method argument
					for (int check = i + 1; check < method.instructions.size(); check++) {
						DisassembledCall c = method.instructions.get(check);
						
						StackCommands stkcmd = StackCommands.valueOf(c.command.def.opCode);
						if (stkcmd == StackCommands.POP_TO) {
							if (c.args[0] == wk) {
								return true;
							}
						}
						else {
							break;
						}
					}
					return false;
				}
				return true;
			} else {
				StackCommands stkcmd = StackCommands.valueOf(cmd.def.opCode);
				MathCommands mathcmd = MathCommands.valueOf(cmd.def.opCode);

				if (stkcmd != StackCommands.PUSH_VAR && !(mathcmd == MathCommands.SET_CONST || mathcmd == MathCommands.SET_FLEX || mathcmd == MathCommands.SET_VAR)) {
					return false;
				}
			}
		}
		return false;
	}

	private boolean findPreviousCallThroughPop(int insIndex, DisassembledMethod method) {
		//Scrolls forward through var setters and stack pushes to a global call
		//If there is a non-set or push instruction, this will return false
		for (int i = insIndex; i >= 0; i--) {
			VCommandDataBase.VCommand cmd = method.instructions.get(i).command;
			if (cmd.type == VCommandDataBase.CommandType.CALL) {
				return true;
			} else {
				StackCommands stkcmd = StackCommands.valueOf(cmd.def.opCode);

				if (stkcmd != StackCommands.POP_TO) {
					return false;
				}
			}
		}
		return false;
	}

	private void handleInstruction(DisassembledCall call, int insIndex, DisassembledMethod method, IndentedPrintStream out) {
		if (call.doNotDisassemble) {
			return;
		}

		StackCommands stackResult = stack.handleInstruction(call);
		MathCommands mathResult = MathMaker.handleInstruction(call);

		if (call.label != null) {
			if (isJumpLabelUsed(call, method)) {
				if (!getIsPointlessToPrintLabel(call)) {
					out.println();
					out.print(call.label);
					out.println(":");
				} else {
					System.out.println("Pointless to label: " + call.label);
				}
			} else {
				System.out.println("Unused label: " + call.label);
			}
		}

		if (stackResult != null) {
			switch (stackResult) {
				case POP_TO:
					int target = call.args[0];
					printAsgnOrWorkSet(out, target, stack.pop().toString());
					out.println(";");
					break;
			}
		} else if (mathResult != null) {
			if (((mathResult == MathCommands.SET_CONST || mathResult == MathCommands.SET_FLEX || mathResult == MathCommands.SET_VAR) && VConstants.isHighWk(call.args[0]))
				&& findNextCallThroughPush(insIndex, call.args[0], method)) {
				//System.out.println("ignoring " + call.command.name + " tgt " + call.args[0] + " mathres " + mathResult);
				//Ignore this - use as call argument later
			} else {
				int target = call.args[0];
				int rhs = call.args[1];
				printVarAssignment(out, mathResult.operator, target, rhs, call.definition.parameters[1].dataType);
				out.println(";");
			}
		} else if (call.command.type == VCommandDataBase.CommandType.REGULAR) {
			if (!call.command.isDecompPrintable()) {
				call.doNotDisassemble = true;
				return;
			}

			int rcbCount = 0;
			for (int i = 0; i < call.command.def.parameters.length; i++) {
				if (call.command.def.parameters[i].isReturnCallback()) {
					rcbCount++;
				}
			}
			boolean isRCB = rcbCount == 1;
			int rcbIndex = call.command.def.getIndexOfFirstReturnArgument();

			if (isRCB) {
				printAsgnOrWorkSet(out, call.args[rcbIndex], null);
			}

			out.print(call.command.getPKSCallName());
			out.print("(");

			boolean started = false;

			for (int i = 0; i < call.args.length; i++) {
				if (i == rcbIndex && rcbCount == 1) {
					continue;
				}
				if (started) {
					out.print(", ");
				}
				started = true;
				int av = call.args[i];
				NTRArgument ad = call.definition.parameters[i];

				if (call.link != null && i == call.link.argIdx) {
					out.print(((DisassembledCall) (call.link.target)).label);
				} else {
					printByDataType(ad.dataType, av, out);
				}
			}

			out.print(")");
			if (isRCB && call.args[rcbIndex] < VConstants.GP_REG_PRI) {
				out.print(")");
			}
			out.println(";");
		} else {
			printIrregularCall(call, method, insIndex, out);
		}
		call.doNotDisassemble = true;
	}

	private static void printAsgnOrWorkSet(PrintStream out, int target, String rhs) {
		if (target < VConstants.GP_REG_PRI) {
			out.print("EventWorks.WorkSet(");
			out.print(target);
			out.print(", ");
			if (rhs != null) {
				out.print(rhs);
				out.print(")");
			}
		} else {
			printFlex(target, out);
			out.print(" = ");
			if (rhs != null) {
				out.print(rhs);
			}
		}
	}

	private static void printVarAssignment(PrintStream out, String operator, int target, int rhs, NTRDataType rhsType) {
		if (target < VConstants.GP_REG_PRI) {
			out.print("EventWorks.WorkSet(");
			out.print(target);
			out.print(", ");
			if (operator != null) {
				out.print(target);
				out.print(" ");
				out.print(operator);
				out.print(" ");
				printByDataType(rhsType, rhs, out);
			} else {
				printFlex(rhs, out);
			}
			out.print(")");
		} else {
			printFlex(target, out);
			out.print(" ");
			if (operator != null) {
				out.print(operator);
			}
			out.print("= ");
			printByDataType(rhsType, rhs, out);
		}
	}

	private static void printByDataType(NTRDataType dataType, int val, PrintStream out) {
		switch (dataType) {
			case VAR:
				out.print(var2StrRef(val));
				break;
			case FLEX:
				printFlex(val, out);
				break;
			case U16:
			case U8:
			case S32:
				out.print(val);
				break;
			case FX16:
			case FX32:
				out.print(val / 4096f);
				out.print("f");
				break;
		}
	}

	private boolean getCanBlockBeTornOut(DisassembledCall blockCaller, DisassembledMethod method) {
		DisassembledCall target = descendToJumpOrigin(blockCaller);
		if (target != null) {
			int linkCount = 0;
			for (DisassembledCall c : method.instructions) {
				if (c.link != null && c.link.target != null && c.link.target == target) {
					linkCount++;
					if (linkCount > 1) {
						return false;
					}
				}
			}

			if (target.pointer > blockCaller.pointer) {
				int idx = method.instructions.indexOf(target) - 1;
				if (idx >= 0) {
					DisassembledCall before = descendToJumpOrigin(method.instructions.get(idx));
					if (before.command.isBranchEnd() || method.instructions.get(idx).command.isBranchEnd()) {
						return true;
					} else {
						//System.out.println("Can not tear out " + Integer.toHexString(target.pointer) + "(" + target.command.name +  ") not branch end " + before.command.name + "(" + Integer.toHexString(before.pointer) + ")");
					}
				}
			}
		}
		return false;
	}

	private List<DisassembledCall> tearOutBlock(DisassembledCall blockCaller, DisassembledMethod method) {
		List<DisassembledCall> instructions = new ArrayList<>();

		DisassembledCall target = descendToJumpOrigin(blockCaller);

		for (int h = method.instructions.indexOf(target); h < method.instructions.size(); h++) {
			DisassembledCall c = method.instructions.get(h);

			boolean end;

			if (c.command.type == VCommandDataBase.CommandType.JUMP && !c.command.readsCmpFlag) {
				end = true;
				instructions.add(c);
				break;
			} else {
				if (c.label != null) {
					end = false;

					for (DisassembledCall caller : method.instructions) {
						if (caller != blockCaller && caller.link != null && caller.link.target == c) {
							if (caller.pointer > c.pointer || caller.pointer < target.pointer) {
								end = true;
								break;
							}
						}
					}
				} else {
					end = false;
				}
			}

			if (end) {
				DisassembledCall jumpToNext = new DisassembledCall(-1, VOpCode.Jump.proto, 0);
				jumpToNext.command = new VCommandDataBase.VCommand("jump-dummy", jumpToNext.definition, VCommandDataBase.CommandType.JUMP, false, false);
				jumpToNext.link = new NTRInstructionLink(jumpToNext, c, 0);
				instructions.add(jumpToNext);
				break;
			} else {
				instructions.add(c);
			}
		}
		target.ignoredLinks.add(blockCaller.link);

		return instructions;
	}

	private boolean getIsPointlessToPrintLabel(DisassembledCall call) {
		//Those will be auto-copied to their respective places in the decompiler
		call = descendToJumpOrigin(call);
		return call.command.type == VCommandDataBase.CommandType.HALT || call.command.type == VCommandDataBase.CommandType.RETURN;
	}

	private void printIrregularCall(DisassembledCall call, DisassembledMethod method, int insIndex, IndentedPrintStream out) {
		switch (call.command.type) {
			case ACTION_JUMP:
				if (call.link != null && call.link.target != null) {
					out.print(((DisassembledCall) call.link.target).label);
					out.print("(");
					printFlex(call.args[0], out);
					out.println(");");
				} else {
					out.println("[ACTION LINK ERROR]");
				}
				break;
			case HALT:
				if (!method.isPublic) {
					out.println("pause;");
					break;
				}
			//fall through
			case RETURN:
				if (method.ptr == -1 || insIndex != method.instructions.size() - 1) {
					out.println("return;");
				}
				break;
			case CALL:
			//fall through to JUMP
			case JUMP:
				if (call.command.readsCmpFlag) {
					out.print("if (");

					int cmpReq = call.args[0];

					if (cmpReq != VCmpResultRequest.STACK_RESULT) {
						String lhs = null;
						String rhs = null;

						DisassembledCall condSrc = null;
						for (int j = insIndex; j >= 0; j--) {
							if (method.instructions.get(j).command.writesCmpFlag) {
								condSrc = method.instructions.get(j);
								break;
							}
						}
						if (condSrc != null) {

							int opCode = condSrc.definition.opCode;

							boolean isGlobal = opCode < VOpCode.CmpVarConst.ordinal();
							if (!isGlobal) {
								lhs = flex2Str(condSrc.args[0]);
							} else {
								lhs = "g_" + condSrc.args[0];
							}
							if (opCode == VOpCode.CmpVMVarConst.ordinal() || opCode == VOpCode.CmpVarConst.ordinal()) {
								rhs = String.valueOf(condSrc.args[1]);
							} else {
								rhs = isGlobal ? "g_" + condSrc.args[1] : flex2Str(condSrc.args[1]);
							}

							out.print(lhs);
							out.print(" ");
							out.print(VCmpResultRequest.getOpStr(cmpReq));
							out.print(" ");
							out.print(rhs);
						} else {
							out.print("false");
						}
					} else {
						StackTracker.StackElement result = stack.pop();

						if (result != null) {
							result.print(out);
						} else {
							out.print("false");
						}
					}

					out.println(") {");
					out.incrementIndentLevel();
				}

				if (call.link == null && !call.command.callsExtern) {
					System.err.println("Link error at " + call.command.name + "(" + Integer.toHexString(call.pointer) + ")");
					out.println("goto [LINK ERROR];");
				} else {
					NTRInstructionLink link = call.link;
					String targetLabel = "[LINK ERROR]";
					int targetAddress = -1;
					if (link != null && link.target != null) {
						targetLabel = ((DisassembledCall) link.target).label;
						targetAddress = link.target.pointer;
					}
					if (call.command.type == VCommandDataBase.CommandType.CALL) {
						List<StackTracker.StackElement> params = getCallParameters(insIndex, targetAddress, method, call.command.callsExtern, false);
						if (call.command.callsExtern) {
							int SCRID = call.args[0];

							VExtern extern = new VExtern();
							extern.SCRID = SCRID;
							extern.paramCount = params.size();
							if (!externs.contains(extern)) {
								externs.add(extern);
							}

							targetLabel = "Global" + SCRID;
						}

						out.print(targetLabel);
						out.print("(");
						for (int i = 0; i < params.size(); i++) {
							if (i != 0) {
								out.print(", ");
							}
							params.get(i).print(out);
						}
						out.println(");");
					} else {
						DisassembledCall trueTarget = descendToJumpOrigin(call);
						targetLabel = trueTarget.label;

						if (!getIsPointlessToGoto(trueTarget, insIndex, method)) {
							switch (trueTarget.command.type) {
								case HALT:
								case RETURN:
									printIrregularCall((DisassembledCall) call.link.target, method, insIndex, out);
									break;
								default:
									if (getCanBlockBeTornOut(call, method)) {
										List<DisassembledCall> tornOut = tearOutBlock(call, method);

										DisassembledMethod dummyMethod = new DisassembledMethod(-1);
										dummyMethod.instructions.addAll(tornOut);
										dummyMethod.isPublic = method.isPublic;

										for (int h2 = 0; h2 < dummyMethod.instructions.size(); h2++) {
											handleInstruction(dummyMethod.instructions.get(h2), h2, dummyMethod, out);
										}
									} else {
										out.print("goto ");
										out.print(targetLabel);
										out.println(";");
									}
									break;
							}
						}
					}
				}

				if (call.command.readsCmpFlag) {
					out.decrementIndentLevel();
					out.println("}");
				}
				break;
		}
	}

	private List<StackTracker.StackElement> getCallParameters(int insIndex, int callAddress, DisassembledMethod method, boolean extern, boolean noFailsafe) {
		List<StackTracker.StackElement> l = new ArrayList<>();
		if (extern) {
			for (int i = insIndex + 1; i < method.instructions.size(); i++) {
				DisassembledCall c = method.instructions.get(i);
				if (StackCommands.valueOf(c.command.def.opCode) != StackCommands.POP_TO) {
					break;
				} else {
					c.doNotDisassemble = true;
					l.add(stack.pop());
				}
			}

			l.sort((o1, o2) -> {
				return o1.value - o2.value;
			});

			int limit = insIndex;
			for (int i = insIndex - 1; i >= 0; i--) {
				if (MathCommands.valueOf(method.instructions.get(i).command.def.opCode) == null) {
					limit = i + 1;
					break;
				}
			}

			for (int i = 0; i < l.size(); i++) {
				StackTracker.StackElement e = l.get(i);
				if (e.type == StackTracker.StackElementType.VARIABLE) {
					for (int j = limit; j < insIndex; j++) {
						//this has to be a math command as per limit
						DisassembledCall call = method.instructions.get(j);
						if (call.args.length < 2) {
							throw new RuntimeException("Var assignment needs to have 2 arguments: " + call.command.name);
						}
						if (call.args[0] == e.value) {
							//the call sets this variable
							StackTracker.StackElementType type;
							switch (MathCommands.valueOf(call.definition.opCode)) {
								case SET_CONST:
									type = StackTracker.StackElementType.CONSTANT;
									break;
								case SET_VAR:
									type = StackTracker.StackElementType.VARIABLE;
									break;
								default:
									if (!VConstants.isWk(call.args[1])) {
										type = StackTracker.StackElementType.CONSTANT;
									} else {
										type = StackTracker.StackElementType.VARIABLE;
									}
									break;
							}

							l.set(i, new StackTracker.StackElement(type, call.args[1]));
							break;
						}
					}
				}
			}
		} else {
			VInternFunc intern = null;
			for (VInternFunc i : detectedInterns) {
				if (i.address == callAddress) {
					intern = i;
					break;
				}
			}

			List<Integer> works = new ArrayList<>();

			Outer:
			for (int i = insIndex - 1; i >= 0; i--) {
				DisassembledCall c = method.instructions.get(i);
				MathCommands cmd = MathCommands.valueOf(c.definition.opCode);
				if (cmd == null) {
					break Outer;
				}

				StackTracker.StackElementType type;

				switch (cmd) {
					case SET_VAR:
						type = StackTracker.StackElementType.VARIABLE;
						break;
					case SET_CONST:
						type = StackTracker.StackElementType.CONSTANT;
						break;
					case SET_FLEX:
						type = VConstants.isWk(c.args[1]) ? StackTracker.StackElementType.VARIABLE : StackTracker.StackElementType.CONSTANT;
						break;
					default:
						break Outer;
				}
				if (c.args[0] < VConstants.GP_REG_PRI) {
					break Outer;
				}
				l.add(new StackTracker.StackElement(type, c.args[1]));
				works.add(c.args[0]);
				if (intern != null && intern.paramWorks.length == l.size()) {
					break Outer;
				}
			}
			Collections.reverse(works);

			if (intern == null) {
				intern = new VInternFunc();
				intern.address = callAddress;
				intern.setParamWorks(works);
				detectedInterns.add(intern);
			} else {
				if (l.size() > intern.paramWorks.length) {
					intern.setParamWorks(works);
				} else if (l.size() < intern.paramWorks.length) {
					if (!noFailsafe) {
						int sz = l.size();
						for (int i = intern.paramWorks.length - 1; i >= sz; i--) {
							l.add(new StackTracker.StackElement(StackTracker.StackElementType.VARIABLE, intern.paramWorks[i]));
						}
					}
				}
			}
		}
		Collections.reverse(l);

		return l;
	}

	private boolean getIsPointlessToGoto(DisassembledCall trueTarget, int insIndex, DisassembledMethod method) {
		for (insIndex++; insIndex < method.instructions.size(); insIndex++) {
			DisassembledCall ins = method.instructions.get(insIndex);

			if (!ins.doNotDisassemble) {
				if (ins != trueTarget) {
					return false;
				} else {
					return true;
				}
			}
		}
		return false;
	}

	private DisassembledCall descendToJumpOrigin(DisassembledCall jump) {
		if (jump.link == null) {
			return jump;
		}
		DisassembledCall target = (DisassembledCall) jump.link.target;
		if (target == null || target.command == null) {
			return jump;
		}

		if (target.command.type == VCommandDataBase.CommandType.JUMP && !target.command.readsCmpFlag) {
			target = descendToJumpOrigin(target);
		}

		return target;
	}

	private static void printFlex(int av, PrintStream out) {
		out.print(flex2Str(av));
	}

	public static String flex2Str(int av) {
		if (av < VConstants.GP_REG_PRI) {
			if (av >= VConstants.WKVAL_START) {
				return "EventWorks.WorkGet(" + (av /*- 0x4000*/) + ")";
			}
			return String.valueOf(av);
		} else if (av > VConstants.WKVAL_END) {
			return String.valueOf(av);
		} else {
			return "v" + (av - VConstants.GP_REG_PRI);
		}
	}

	public static String var2StrRef(int av) {
		if (av < VConstants.GP_REG_PRI) {
			return String.valueOf(av);
		}
		return flex2Str(av);
	}

	private void dumpActionSeq(DisassembledMethod m, IndentedPrintStream out) {
		out.print("static meta void ");
		out.print(m.getName());
		out.println("(int npcId) : VActionSequence {");
		out.incrementIndentLevel();

		for (DisassembledCall call : m.instructions) {
			out.print("Action");
			out.print(FormattingUtils.getStrWithLeadingZeros(4, Integer.toHexString(call.definition.opCode)));
			out.print("(");
			out.print(call.args[0]);
			out.println(");");
		}

		out.decrementIndentLevel();
		out.println("}");

		out.println();
	}

	public static class VExtern {

		public int SCRID;
		public int paramCount;

		@Override
		public boolean equals(Object o) {
			if (o != null && o instanceof VExtern) {
				VExtern v = (VExtern) o;
				return v.SCRID == SCRID && v.paramCount == paramCount;
			}
			return false;
		}

		@Override
		public int hashCode() {
			int hash = 7;
			hash = 59 * hash + this.SCRID;
			hash = 59 * hash + this.paramCount;
			return hash;
		}
	}

	public static class VInternFunc {

		public int address;
		public int[] paramWorks;

		public void setParamWorks(List<Integer> works) {
			paramWorks = new int[works.size()];
			for (int i = 0; i < works.size(); i++) {
				paramWorks[i] = works.get(i);
			}
		}

		@Override
		public boolean equals(Object o) {
			if (o != null && o instanceof VExtern) {
				VInternFunc v = (VInternFunc) o;
				return v.address == address && Arrays.equals(paramWorks, v.paramWorks);
			}
			return false;
		}

		@Override
		public int hashCode() {
			int hash = 3;
			hash = 53 * hash + this.address;
			hash = 53 * hash + Arrays.hashCode(this.paramWorks);
			return hash;
		}
	}
}
