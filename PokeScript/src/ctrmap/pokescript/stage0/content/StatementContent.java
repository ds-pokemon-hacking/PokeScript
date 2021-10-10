package ctrmap.pokescript.stage0.content;

import ctrmap.pokescript.LangConstants;
import ctrmap.pokescript.expr.Throughput;
import ctrmap.pokescript.stage0.Preprocessor;
import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.instructions.abstractcommands.APlainOpCode;
import ctrmap.pokescript.stage0.BraceContent;
import ctrmap.pokescript.stage0.CompilerPragma;
import ctrmap.pokescript.stage0.EffectiveLine;
import ctrmap.pokescript.stage0.Statement;
import ctrmap.pokescript.stage1.BlockStack;
import ctrmap.pokescript.stage1.CompileBlock;
import ctrmap.pokescript.stage1.NCompilableMethod;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.pokescript.stage1.NExpression;
import ctrmap.pokescript.stage1.PendingLabel;
import ctrmap.pokescript.types.DataType;
import ctrmap.pokescript.types.declarers.DeclarerController;
import ctrmap.stdlib.text.StringEx;
import ctrmap.stdlib.util.ArraysEx;
import java.util.ArrayList;
import java.util.List;

public class StatementContent extends AbstractContent {

	public Statement statement;
	public List<String> arguments = new ArrayList<>();

	public StatementContent(Statement statement, EffectiveLine line, int contentStart) {
		super(line);
		this.statement = statement;
		String source = line.data;

		int realStart = EffectiveLine.getFirstNonWhiteSpaceIndex(contentStart, source);

		if (statement.hasFlag(EffectiveLine.StatementFlags.FORBIDS_ARGUMENTS)) {
			char endC;
			if (realStart < source.length()) {
				endC = source.charAt(realStart);
				if (!Preprocessor.isTerminator(endC, statement.hasFlag(EffectiveLine.StatementFlags.PREPROCESSOR_STATEMENT))) {
					line.throwException("Non-argumental statement - terminator expected at " + source);
				}
			} else {
				if (!statement.hasFlag(EffectiveLine.StatementFlags.PREPROCESSOR_STATEMENT)) {
					line.throwException("Unexpected end of line: " + source);
				}
			}
		} else {
			if (statement.hasFlag(EffectiveLine.StatementFlags.HAS_ARGUMENTS_IN_BRACES)) {
				char c = 0;
				if (realStart < source.length()) {
					c = source.charAt(realStart);
				}
				if (c != '(') {
					line.throwException("( expected for statement " + source);
				}
				BraceContent args = Preprocessor.getContentInBraces(source, realStart);
				if (!args.hasIntegrity) {
					line.throwException("Unbalanced statement arguments - ) expected at " + source);
				}
				String[] argsSplit = StringEx.splitOnecharFastNoBlank(args.getContentInBraces(), LangConstants.CH_STATEMENT_SEPARATOR);
				arguments.addAll(ArraysEx.asList(argsSplit));
			} else {
				//arguments separated with gaps
				String at = Preprocessor.getStrWithoutTerminator(source.substring(realStart)).trim();
				if (!at.isEmpty()) {
					StringBuilder sb = new StringBuilder();
					int bl = 0;
					for (int i = 0; i < at.length(); i++) {
						char c = at.charAt(i);
						switch (c) {
							case '(':
								bl++;
								break;
							case ')':
								bl--;
								break;
							case ' ':
								if (bl == 0) {
									arguments.add(sb.toString());
									sb = new StringBuilder();
								}
								break;
						}
						sb.append(c);
					}
					if (sb.length() != 0) {
						arguments.add(sb.toString());
					}
				}
			}

			if (arguments.isEmpty() && statement.hasFlag(EffectiveLine.StatementFlags.NEEDS_ARGUMENTS)) {
				line.throwException("Illegal statement - statement " + statement + " requires arguments.");
			}
			/*if (statement.hasFlag(EffectiveLine.StatementFlags.PREPROCESSOR_STATEMENT)) {
				if (arguments.size() > 1) {
					line.throwException("Illegal preprocessor statement - preprocessor statements can only have 1 argument, got " + arguments.size() + ".");
				}
			}*/
		}
	}

	@Override
	public CompilerContentType getContentType() {
		return CompilerContentType.STATEMENT;
	}

	@Override
	public void declareToGraph(NCompileGraph graph, DeclarerController declarer) {
		if (statement == Statement.PACKAGE) {
			if (arguments.size() > 1) {
				line.throwException("Package can have only one argument.");
			}
			String packageName = (arguments.isEmpty() ? null : arguments.get(0));
			declarer.setPackage(packageName);
		} else if (statement == Statement.IMPORT) {
			if (arguments.size() != 1) {
				line.throwException("Import can have exactly one argument.");
			}
			String importPath = arguments.get(0);
			boolean success = graph.ensureIncludeForPath(importPath); //this ensures the inclusion of the import's compile graph
			if (!success) {
				line.throwException("Unresolved import: " + importPath);
			}
			String parentPath = getTrimmedImport(importPath);
			graph.importedNamespaces.add(importPath);
			graph.applyImport(parentPath);
			graph.includedClasses.add(importPath);
		}
	}

	@Override
	public void addToGraph(NCompileGraph graph) {
		List<AInstruction> l = new ArrayList<>();
		switch (statement) {
			case PAUSE:
				l.add(graph.getPlain(APlainOpCode.ZERO_PRI));
				l.add(graph.getPlain(APlainOpCode.ZERO_ALT));
				l.add(graph.getPlain(APlainOpCode.ABORT_EXECUTION, 12));
				break;
			case BREAK: {
				String label = null;
				if (!arguments.isEmpty()) {
					label = arguments.get(0);
				}
				BlockStack.BlockResult b = graph.currentBlocks.getBlocksToAttribute(CompileBlock.BlockAttribute.BREAKABLE, label);
				if (b.blocks.isEmpty()) {
					line.throwException("Nothing to break out of.");
				}
				l.add(graph.getLocalsRemoveIns(b.collectLocalsNoBottom()));
				//jump to the end of the block
				CompileBlock bb = b.getBottomBlock();
				l.add(graph.provider.getConditionJump(APlainOpCode.JUMP, bb.getBlockTermFullLabel()));
				break;
			}
			case CONTINUE: {
				String label = null;
				if (!arguments.isEmpty()) {
					label = arguments.get(0);
				}
				BlockStack.BlockResult b = graph.currentBlocks.getBlocksToAttribute(CompileBlock.BlockAttribute.LOOP, label);
				if (b.blocks.isEmpty()) {
					line.throwException("No loop found to continue.");
				}
				l.add(graph.getLocalsRemoveIns(b.collectLocalsNoBottom()));
				//jump to the beginning of the block
				CompileBlock bb = b.getBottomBlock();
				l.add(graph.provider.getConditionJump(APlainOpCode.JUMP, bb.fullBlockName));
				break;
			}
			case RETURN: {
				NExpression expr = getExprArg(graph);

				NCompilableMethod m = graph.getCurrentMethod();

				if (m != null) {
					if (expr != null) {
						l.addAll(expr.toThroughput(graph).getCode(m.def.retnType));
					} else {
						if (m.def.retnType.baseType != DataType.VOID) {
							line.throwException("Expected a return value.");
						}
						l.add(graph.getPlain(APlainOpCode.ZERO_PRI));
					}
					if (m.locals != null) {
						l.add(graph.getLocalsRemoveIns(m.locals.getNonArgVars())); //removes all the locals used in the method so far
					}
					else {
						line.throwException("Internal error: method not initialized " + m.def);
					}
					l.add(graph.getPlain(APlainOpCode.RETURN));
				} else {
					line.throwException("No method to return from!");
				}
				break;
			}
			case SWITCH: {
				CompileBlock switchBlock = new CompileBlock(statement, graph);
				graph.pushBlock(switchBlock);
				NExpression expr = getExprArg(graph);
				if (expr != null) {
					Throughput tp = expr.toThroughput(graph);
					if (tp.checkImplicitCast(DataType.INT.typeDef(), line)) {
						l.addAll(expr.toThroughput(graph).getCode(DataType.INT.typeDef()));
					} else {
						line.throwException("A switch statement requires an integer target.");
					}
				} else {
					line.throwException("A switch statement requires a target.");
				}

				l.add(graph.provider.getConditionJump(APlainOpCode.SWITCH, switchBlock.fullBlockName + "_casetbl"));
				switchBlock.blockEndControlInstruction = graph.provider.getCaseTable();
				switchBlock.blockEndInstructions.add(graph.provider.getConditionJump(APlainOpCode.JUMP, switchBlock.getBlockTermFullLabel()));
				switchBlock.blockEndControlInstruction.addLabel(switchBlock.fullBlockName + "_casetbl");
				//now the compilation follows like normal, adding the casetbl labels to the switch by recognizing the block's control instruction
				break;
			}
			case IF: {
				CompileBlock ifBlock = new CompileBlock(statement, graph);
				ifBlock.explicitAdjustStack = true;
				graph.pushBlock(ifBlock);
				NExpression expr = getExprArg(graph);
				if (expr != null) {
					addCondToList(expr, l, graph.provider.getConditionJump(APlainOpCode.JUMP_IF_ZERO, ifBlock.getBlockHaltFullLabel()), graph);
				} else {
					line.throwException("An if statement requires a condition.");
				}

				//l.add(ifBlock.blockBeginAdjustStackIns);
				ifBlock.blockEndControlInstruction = graph.provider.getConditionJump(APlainOpCode.JUMP, ifBlock.getBlockHaltFullLabel());
				break;
			}
			case ELSE:
			case ELSE_IF: {
				CompileBlock lastHistoryBlock = graph.blockHistory.getLatestBlock();
				if (lastHistoryBlock == null || !lastHistoryBlock.hasAttribute(CompileBlock.BlockAttribute.NEGATEABLE)) {
					line.throwException("Nothing to negate with the else statement.");
				}

				CompileBlock elseBlock = new CompileBlock(statement, graph);
				elseBlock.explicitAdjustStack = true;
				elseBlock.chainPredecessor = lastHistoryBlock;
				graph.pushBlock(elseBlock);
				NExpression expr = getExprArg(graph);
				if (expr != null) {
					if (statement == Statement.ELSE) {
						line.throwException("An else statement can not have a condition.");
					} else {
						addCondToList(expr, l, graph.provider.getConditionJump(APlainOpCode.JUMP_IF_ZERO, elseBlock.getBlockHaltFullLabel()), graph);
					}
				} else if (statement == Statement.ELSE_IF) {
					line.throwException("An else-if statement requires a condition.");
				}
				if (statement == Statement.ELSE_IF) {
					elseBlock.blockEndControlInstruction = graph.provider.getConditionJump(APlainOpCode.JUMP, elseBlock.getBlockHaltFullLabel());
				}
				//l.add(elseBlock.blockBeginAdjustStackIns);
				//The if statement jumps to the beginning to the else statement if it fails naturally.
				//However, we also need it to jump to the end of the whole chain if it succeeds.
				elseBlock.setChainJumpArg0(elseBlock.getBlockHaltFullLabel());
				break;
			}
			case WHILE: {
				CompileBlock whileBlock = new CompileBlock(statement, graph);
				whileBlock.explicitAdjustStack = true;
				graph.pushBlock(whileBlock);
				NExpression expr = getExprArg(graph);
				if (expr != null) {
					addCondToList(expr, l, graph.provider.getConditionJump(APlainOpCode.JUMP_IF_ZERO, whileBlock.getBlockHaltFullLabel()), graph);
				} else {
					line.throwException("A while statement requires a condition.");
				}

				//l.add(whileBlock.blockBeginAdjustStackIns);
				whileBlock.blockEndControlInstruction = graph.provider.getConditionJump(APlainOpCode.JUMP, whileBlock.fullBlockName);
				break;
			}
			case FOR: {
				CompileBlock forDeclBlock = new CompileBlock(statement, graph);
				CompileBlock forBlock = new CompileBlock(statement, graph);
				forBlock.explicitAdjustStack = false;
				forBlock.popNext = true;
				//graph.pushBlock(forDeclBlock);
				EffectiveLine.AnalysisState dummyState = new EffectiveLine.AnalysisState();
				dummyState.incrementBlock(EffectiveLine.AnalysisLevel.LOCAL);
				if (arguments.size() == 3) {
					boolean isLeakDecl = graph.getIsBoolPragmaEnabledSimple(CompilerPragma.LEAK_FOR_DECL);
					if (!isLeakDecl) {
						graph.pushBlock(forDeclBlock);
					}
					DeclarationContent tryDecl = DeclarationContent.getDeclarationCnt(arguments.get(0), line, dummyState);
					if (tryDecl != null) {
						tryDecl.addToGraph(graph);
					}
					if (isLeakDecl) {
						graph.pushBlock(forDeclBlock);
					}
					graph.pushBlock(forBlock);
					PendingLabel effectiveStart = graph.addPendingLabel("FOR_effective_start");
					NExpression condition = getExprArg(graph, 1);
					addCondToList(condition, l, graph.provider.getConditionJump(APlainOpCode.JUMP_IF_ZERO, forBlock.getBlockHaltFullLabel()), graph);
					//l.add(forBlock.blockBeginAdjustStackIns);
					NExpression expr = getExprArg(graph, 2);
					if (expr != null) {
						Throughput tp = expr.toThroughput(graph);
						if (tp != null) {
							forBlock.blockEndInstructions.addAll(tp.getCode(DataType.ANY.typeDef()));
						}
					}
					forBlock.blockEndControlInstruction = graph.provider.getConditionJump(APlainOpCode.JUMP, effectiveStart.getFullLabel());
				} else {
					line.throwException("A for exception requires a declaration, condition and expression.");
				}

				break;
			}
			case GOTO: {
				if (graph.provider.getMachineInfo().getAllowsGotoStatement()) {
					if (arguments.isEmpty()) {
						line.throwException("A goto statement needs a target.");
					} else {
						String label = arguments.get(0);
						graph.addLabelRequest(label);
						l.add(graph.provider.getConditionJump(APlainOpCode.JUMP, label));
					}
				} else {
					line.throwException("This compiler does not support the goto statement.");
				}
				break;
			}
			case P_PRAGMA: {
				CompilerPragma.PragmaValue val = CompilerPragma.tryIdentifyPragma(this, line);
				if (val != null) {
					graph.pragmata.put(val.pragma, val);
				}
				break;
			}
		}
		graph.addInstructions(l);
	}

	private void addCondToList(NExpression expr, List<AInstruction> l, AInstruction endJump, NCompileGraph cg) {
		if (expr != null) {
			Throughput tp = expr.toThroughput(cg);
			if (tp.checkImplicitCast(DataType.BOOLEAN.typeDef(), line)) {
				l.addAll(tp.getCode(DataType.BOOLEAN.typeDef()));
				l.add(endJump);
			}
		}
	}

	private NExpression getExprArg(NCompileGraph g) {
		return getExprArg(g, 0);
	}

	private NExpression getExprArg(NCompileGraph g, int idx) {
		if (!arguments.isEmpty()) {
			NExpression ex = new NExpression(arguments.get(idx), line, g);
			if (line.exceptions.isEmpty()) {
				return ex;
			}
		}
		return null;
	}

	private String getTrimmedImport(String str) {
		int liodot = str.lastIndexOf(LangConstants.CH_PATH_SEPARATOR);
		if (liodot != -1) {
			str = str.substring(0, liodot);
		}
		return str;
	}
}
