package ctrmap.pokescript.stage0.content;

import ctrmap.pokescript.InboundDefinition;
import ctrmap.pokescript.LangConstants;
import ctrmap.pokescript.data.ClassVariable;
import ctrmap.pokescript.stage0.Preprocessor;
import ctrmap.pokescript.data.Variable;
import ctrmap.pokescript.types.DataType;
import ctrmap.pokescript.expr.Throughput;
import ctrmap.pokescript.instructions.abstractcommands.APlainOpCode;
import ctrmap.pokescript.stage0.BraceContent;
import ctrmap.pokescript.stage0.EffectiveLine;
import ctrmap.pokescript.stage0.Modifier;
import ctrmap.pokescript.stage1.NCompilableMethod;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.pokescript.stage1.NExpression;
import ctrmap.pokescript.types.TypeDef;
import ctrmap.pokescript.types.classes.ClassDefinition;
import ctrmap.pokescript.types.declarers.DeclarerController;
import ctrmap.stdlib.text.StringEx;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class DeclarationContent extends AbstractContent {

	public String declaredName;
	public String declaredExtendsName;
	public TypeDef declaredType;
	public List<Modifier> declaredModifiers = new ArrayList<>();

	public String initFromContent;
	public List<Argument> arguments = new ArrayList<>();

	public DeclarationContent(EffectiveLine l) {
		super(l);
	}

	public boolean hasModifier(Modifier mod) {
		return declaredModifiers.contains(mod);
	}

	public boolean isClassDeclaration() {
		return hasModifier(Modifier.CLASSDEF);
	}

	public boolean isEnumDeclaration() {
		return hasModifier(Modifier.ENUMDEF);
	}

	public boolean isClassOrEnumDeclaration() {
		return isClassDeclaration() || isEnumDeclaration();
	}

	public boolean isVarDeclaration() {
		return hasModifier(Modifier.VARIABLE);
	}

	public boolean isMethodDeclaration() {
		return !isVarDeclaration() && !isClassOrEnumDeclaration();
	}

	private Object declaredObject;

	@Override
	public void declareToGraph(NCompileGraph graph, DeclarerController declarer) {
		if (isVarDeclaration()) {
			if (line.context != EffectiveLine.AnalysisLevel.LOCAL) {
				declaredType = getCorrectTypeDef(graph);

				NExpression init_from = null;
				if (initFromContent != null) {
					init_from = new NExpression(initFromContent, line, graph);
				}

				if (hasModifier(Modifier.STATIC) || line.context == EffectiveLine.AnalysisLevel.GLOBAL) {
					if (declarer.classDecl != null) {
						Variable.Global g = new Variable.Global(
							LangConstants.makePath(declarer.classDecl.def.className, declaredName),
							declaredModifiers,
							declaredType,
							init_from,
							graph
						);
						declarer.addGlobal(g);
					} else {
						line.throwException("Can not declare outside of a class!!");
					}
				} else {
					//TODO class fields
				}

				if (init_from != null) {
					Throughput tp = init_from.toThroughput(graph);
					checkVariableInitType(tp);
				}
			}
		} else if (isClassDeclaration()) {
			declaredObject = declarer.beginClass(declaredName);
			declarer.classDecl.def.modifiers.addAll(declaredModifiers);
		} else if (isEnumDeclaration()) {
			declaredObject = declarer.beginEnum(declaredName);
			declarer.classDecl.def.modifiers.addAll(declaredModifiers);
		} else if (isMethodDeclaration()) {
			NCompilableMethod m = getMethod();
			m.def.retnType = getCorrectTypeDef(graph);
			for (Argument a : m.def.args) {
				a.typeDef = getCorrectTypeDef(graph, a.typeDef);
			}
			declaredObject = m;
			InboundDefinition def = m.def;
			if (declarer.classDecl != null) {
				def.name = LangConstants.makePath(declarer.classDecl.def.className, def.name);
				declarer.addMethod(def);
			} else {
				line.throwException("Can not declare outside of a class!!");
			}
		}
	}

	private TypeDef getCorrectTypeDef(NCompileGraph graph) {
		return getCorrectTypeDef(graph, declaredType);
	}

	private TypeDef getCorrectTypeDef(NCompileGraph graph, TypeDef td) {
		if (td.isClassOrEnum()) {
			ClassDefinition def = graph.resolveClass(td.className);
			if (def == null) {
				line.throwException("Could not resolve argument type " + td);
				return td;
			} else {
				return def.getTypeDef();
			}
		} else {
			return td;
		}
	}

	@Override
	public void addToGraph(NCompileGraph graph) {
		if (isVarDeclaration()) {
			NExpression init_from = null;
			if (initFromContent != null) {
				init_from = new NExpression(initFromContent, line, graph);
			}

			Variable result = null;

			if (line.context == EffectiveLine.AnalysisLevel.LOCAL) {
				declaredType = getCorrectTypeDef(graph);

				Variable.Local l = new Variable.Local(declaredName, declaredModifiers, declaredType, graph);
				graph.addLocal(l);
				graph.addInstruction(graph.getPlain(APlainOpCode.RESIZE_STACK, graph.provider.getMemoryInfo().getStackIndexingStep()));
				result = l;
				if (init_from != null) {
					Throughput tp = init_from.toThroughput(graph);
					if (checkVariableInitType(tp)) {
						if (line.context == EffectiveLine.AnalysisLevel.LOCAL) {
							graph.addInstructions(tp.getCode(declaredType));
							graph.addInstruction(result.getWriteIns(graph));
						}
					}
				}
			}
		} else if (isMethodDeclaration()) {
			if (declaredObject instanceof NCompilableMethod) {
				NCompilableMethod m = (NCompilableMethod) declaredObject;
				m.initWithCompiler(line, graph);
				if (m.metaHandler != null) {
					m.metaHandler.onDeclare(this);
				}
				graph.addMethod(m);
			}
		} else {
			if (isClassOrEnumDeclaration()) {
				if (declaredObject instanceof ClassDefinition) {
					graph.currentClass = (ClassDefinition) declaredObject;
				}
			}
		}
	}

	private boolean checkVariableInitType(Throughput tp) {
		if (tp != null) {
			if (!Throughput.checkImplicitCast(declaredType, tp.type)) {
				line.throwException("Invalid assignment type: " + tp.type + " for variable of type " + declaredType + ".");
				return true;
			} else {
				return true;
			}
		}
		return false;
	}

	public NCompilableMethod getMethod() {
		if (!isMethodDeclaration()) {
			throw new UnsupportedOperationException("This is not a method - " + declaredName);
		}
		NCompilableMethod m = new NCompilableMethod(declaredName, declaredModifiers, arguments.toArray(new Argument[arguments.size()]), declaredType);
		m.def.extendsBase = declaredExtendsName;
		return m;
	}

	public static DeclarationContent getDeclarationCnt(String str, EffectiveLine line, EffectiveLine.AnalysisState state) {
		if (!line.hasType(EffectiveLine.LineType.NORMAL) && !line.hasType(EffectiveLine.LineType.BLOCK_START)) {
			return null;
		}
		str = Preprocessor.getStrWithoutTerminator(str);
		int firstBrace = str.indexOf('(');
		int firstEquals = str.indexOf(LangConstants.CH_ASSIGNMENT);
		int declarationEnd = -1;
		if (firstBrace != -1) {
			declarationEnd = firstBrace;
		}
		if (firstEquals != -1) {
			declarationEnd = (declarationEnd != -1) ? Math.min(firstEquals, declarationEnd) : firstEquals;
		}
		if (declarationEnd == -1) {
			declarationEnd = str.length();
		}
		String declStr = str.substring(0, declarationEnd).trim();
		for (int i = 0; i < declStr.length(); i++) {
			char c = declStr.charAt(i);
			if (!(Character.isWhitespace(c) || LangConstants.isAllowedNameChar(c))) {
				return null;
			}
		}
		//the EffectiveLine preprocessor will already have removed duped whitespaces
		String[] declCommands = StringEx.splitOnecharFast(declStr, ' ');
		boolean modifiersDone = false;
		DataType declaredType = null;
		String declaredTypeName = null;
		String declaredName = null;
		List<Modifier> mods = new ArrayList<>();
		if (declCommands.length < 2) {
			return null;
		}

		boolean isClassDef = false;

		for (String cmd : declCommands) {
			if (declaredName != null) {
				return null;
			}
			if (declaredTypeName != null) {
				declaredName = cmd;
			}
			if (!modifiersDone) {
				Modifier mod = Modifier.fromName(cmd);
				if (mod != null) {
					if (mods.contains(mod)) {
						line.throwException("Duplicate modifier: " + cmd);
					}
					mods.add(mod);
				} else {
					modifiersDone = true;
				}
			}
			if (modifiersDone && declaredType == null) {
				declaredType = DataType.fromName(cmd);
				declaredTypeName = cmd;

				if (declaredType == null) {
					declaredType = DataType.CLASS;
				} else {
					if (declaredType == DataType.CLASS || declaredType == DataType.ENUM) {
						isClassDef = true;
					}
				}
			}
		}

		if (declaredTypeName != null) {
			DeclarationContent cnt = new DeclarationContent(line);
			cnt.declaredName = declaredName;
			cnt.declaredType = new TypeDef(declaredTypeName);
			cnt.declaredModifiers = mods;
			//No going back, this is MEANT to be a declaration
			for (Modifier m : mods) {
				if (state.getLevel() == EffectiveLine.AnalysisLevel.LOCAL) {
					line.throwException("Modifier " + m + " is not allowed in a local context!");
				}
			}
			char charAtDeclEnd = Preprocessor.safeCharAt(str, declarationEnd);
			boolean hasSetting = charAtDeclEnd == LangConstants.CH_ASSIGNMENT;
			boolean isMethod = charAtDeclEnd == '(';

			if (isMethod) {
				if (hasSetting) {
					line.throwException(str + " A method can not be assigned with = !");
				}
				BraceContent braceCnt = Preprocessor.getContentInBraces(str, firstBrace);
				String[] argsCommands = StringEx.splitOnecharFast(braceCnt.getContentInBraces(), LangConstants.CH_ELEMENT_SEPARATOR);
				for (String argCmd : argsCommands) {
					argCmd = argCmd.trim();
					if (argCmd.isEmpty()) {
						continue;
					}
					String[] argCmdCmds = StringEx.splitOnecharFast(argCmd, ' ');
					if (argCmdCmds.length < 2) {
						line.throwException("An argument declaration must contain at least a return type and a name. - " + braceCnt.getContentInBraces());
					} else {
						/*DataType argType = DataType.fromName(argCmdCmds[0]);
						if (argType == null) {
							//line.throwException("Unknown argument type: " + argCmdCmds[0]);
						}*/
						Argument a = new Argument();

						int defStart = 0;
						for (; defStart < argCmdCmds.length; defStart++) {
							Modifier mod = Modifier.fromName(argCmdCmds[defStart]);
							if (mod != null) {
								a.requestedModifiers.add(mod);
							} else {
								break;
							}
						}

						checkModifiers(a.requestedModifiers, Modifier.ModifierTarget.ARG, line);
						a.typeDef = new TypeDef(argCmdCmds[defStart]);

						if (a.requestedModifiers.contains(Modifier.VAR)) {
							if (a.typeDef.baseType != DataType.CLASS) {
								a.typeDef.baseType = a.typeDef.baseType.getVarType();
							} else {
								line.throwException("A class parameter can not have the var modifier specified.");
							}
						}

						a.name = argCmdCmds[defStart + 1];
						if (!Preprocessor.checkNameValidity(a.name)) {
							line.throwException("Illegal character in name: " + a.name);
						}
						cnt.arguments.add(a);

						if (argCmdCmds.length != defStart + 2) {
							//line.throwException("Too many parameters in argument declaration.");
							for (int i = 0; i < argCmdCmds.length - 2; i++) {
								if (Modifier.fromName(argCmdCmds[i]) == null) {
									line.throwException("Invalid modifier: " + argCmdCmds[i]);
								}
							}
						}
					}
				}
				if (cnt.hasModifier(Modifier.NATIVE)) {
					if (!line.hasType(EffectiveLine.LineType.NORMAL)) {
						line.throwException("Native methods may not have a body.");
					}
					if (!cnt.hasModifier(Modifier.STATIC)) { //native methods are automatically static
						cnt.declaredModifiers.add(Modifier.STATIC);
					}
				} else if (!cnt.hasModifier(Modifier.META)) {
					if (!line.hasType(EffectiveLine.LineType.BLOCK_START)) {
						line.throwException("Non-native methods require a body.");
					}
				}

				int extendsIdx = str.indexOf(LangConstants.CH_METHOD_EXTENDS_IDENT, braceCnt.endIndex);
				if (extendsIdx != -1) {
					String extendsStr = str.substring(extendsIdx + 1, str.length()).trim();

					cnt.declaredExtendsName = extendsStr;
				}
				checkModifiers(cnt.declaredModifiers, Modifier.ModifierTarget.METHOD, line);
			} else if (!isClassDef) {
				if (declaredType == DataType.VOID) {
					line.throwException("A variable can not be of the void type.");
				}
				if (hasSetting) {
					int initStart = declarationEnd + 1;
					if (initStart >= str.length()) {
						line.throwException("Empty assignment.");
					}
					cnt.initFromContent = Preprocessor.getStrWithoutTerminator(str.substring(initStart, str.length()).trim());
				}
				checkModifiers(cnt.declaredModifiers, Modifier.ModifierTarget.VAR, line);
				cnt.declaredModifiers.add(Modifier.VARIABLE);
			} else {
				if (declaredType != DataType.ENUM) {
					//Class definition
					if (!line.hasType(EffectiveLine.LineType.BLOCK_START) || hasSetting) {
						line.throwException("Class definitions should start with {");
					}

					checkModifiers(cnt.declaredModifiers, Modifier.ModifierTarget.CLASS, line);
					cnt.declaredModifiers.add(Modifier.CLASSDEF);
				} else {
					if (hasSetting) {
						line.throwException("Enum definitions may not have an assignment.");
					}

					checkModifiers(cnt.declaredModifiers, Modifier.ModifierTarget.ENUM, line);
					cnt.declaredModifiers.add(Modifier.ENUMDEF);
				}
			}
			return cnt;
		} else {
			return null;
		}
	}

	public static void checkModifiers(List<Modifier> modifiers, Modifier.ModifierTarget tgt, EffectiveLine line) {
		for (Modifier m : modifiers) {
			if (!m.supportsTarget(tgt)) {
				line.throwException("Modifier " + m.name + " not supported on target " + tgt);
			}
		}
	}

	@Override
	public CompilerContentType getContentType() {
		return CompilerContentType.DECLARATION;
	}

	public static class Argument {

		public String name;
		public TypeDef typeDef;
		public List<Modifier> requestedModifiers = new ArrayList<>();

		public Argument() {

		}

		public Argument(String name, TypeDef typeDef) {
			this.name = name;
			this.typeDef = typeDef;
		}

		@Override
		public boolean equals(Object o) {
			if (o != null && o instanceof Argument) {
				Argument a = (Argument) o;
				return a.name.equals(name) && a.typeDef.equals(typeDef);
			}
			return false;
		}
	}
}
