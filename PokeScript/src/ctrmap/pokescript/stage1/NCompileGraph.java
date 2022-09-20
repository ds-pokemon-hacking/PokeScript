package ctrmap.pokescript.stage1;

import ctrmap.pokescript.InboundDefinition;
import ctrmap.pokescript.LangCompiler;
import ctrmap.pokescript.LangConstants;
import ctrmap.pokescript.stage0.Preprocessor;
import ctrmap.pokescript.OutboundDefinition;
import ctrmap.pokescript.data.DataGraph;
import ctrmap.pokescript.data.LocalDataGraph;
import ctrmap.pokescript.data.Variable;
import ctrmap.pokescript.instructions.abstractcommands.AAccessVariable;
import ctrmap.pokescript.instructions.abstractcommands.ACaseTable;
import ctrmap.pokescript.instructions.abstractcommands.AConditionJump;
import ctrmap.pokescript.instructions.abstractcommands.AFloatInstruction;
import ctrmap.pokescript.instructions.abstractcommands.AFloatOpCode;
import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.instructions.abstractcommands.AInstructionType;
import ctrmap.pokescript.instructions.abstractcommands.ALocalCall;
import ctrmap.pokescript.instructions.abstractcommands.ANativeCall;
import ctrmap.pokescript.instructions.abstractcommands.MetaCall;
import ctrmap.pokescript.instructions.abstractcommands.APlainInstruction;
import ctrmap.pokescript.instructions.abstractcommands.APlainOpCode;
import ctrmap.pokescript.instructions.providers.AInstructionProvider;
import ctrmap.pokescript.stage0.CompilerAnnotation;
import ctrmap.pokescript.stage0.CompilerPragma;
import ctrmap.pokescript.stage0.EffectiveLine;
import ctrmap.pokescript.stage0.Modifier;
import ctrmap.pokescript.stage0.content.DeclarationContent;
import ctrmap.pokescript.types.DataType;
import ctrmap.pokescript.types.classes.ClassDefinition;
import xstandard.fs.FSFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NCompileGraph {

	public DataGraph globals = new DataGraph();

	public List<String> libraries = new ArrayList<>();

	public List<ClassDefinition> classDefs = new ArrayList<>();

	public List<InboundDefinition> methodHeaders = new ArrayList<>();
	public List<NCompilableMethod> methods = new ArrayList<>();

	public NCompilableMethod staticInitializers;
	public Variable.Global gNeedsStaticInit = new Variable.Global("::__static_needInit", new ArrayList<>(), DataType.BOOLEAN.typeDef(), this, 1);

	public List<FSFile> includePaths = new ArrayList<>();
	public List<Preprocessor> includedReaders = new ArrayList<>();

	public BlockStack<CompileBlock> currentBlocks = new BlockStack<>();
	public BlockStack<CompileBlock> blockHistory = new BlockStack<>();
	public BlockStack<CompileBlock> methodBlocks = new BlockStack<>();

	private List<PendingLabel> pendingLabels = new ArrayList<>();
	private List<LabelRequest> requestedLabels = new ArrayList<>();
	private List<CompilerAnnotation> pendingAnnotations = new ArrayList<>();

	public ClassDefinition currentClass;
	public String packageName;

	private String graphUID;
	private LangCompiler.CompilerArguments args;

	public Map<CompilerPragma, CompilerPragma.PragmaValue> pragmata = new HashMap<>();

	public EffectiveLine currentCompiledLine = new EffectiveLine();
	public List<String> importedNamespaces = new ArrayList<>();
	public List<String> includedClasses = new ArrayList<>();

	public AInstructionProvider provider;

	public NCompileGraph(LangCompiler.CompilerArguments args) {
		graphUID = UUID.randomUUID().toString();
		this.args = args;
		if (args != null) {
			this.provider = args.provider;
			pragmata.putAll(args.pragmata);
		}
		staticInitializers = new NCompilableMethod("::__static", new ArrayList<>(), new DeclarationContent.Argument[0], DataType.VOID.typeDef());
		globals.addVariable(gNeedsStaticInit, this);
	}

	public LangCompiler.CompilerArguments getArgs() {
		return args;
	}

	public NCompilableMethod getCurrentMethod() {
		if (!hasMethods()) {
			currentCompiledLine.throwException("Can not request a method without any.");
			NCompilableMethod dmy = new NCompilableMethod("dummy", new ArrayList<>(), new DeclarationContent.Argument[0], DataType.INT.typeDef());
			dmy.initWithCompiler(currentCompiledLine, this);
			return dmy;
		}
		return methods.get(methods.size() - 1);
	}

	public CompileBlock getCurrentBlock() {
		if (currentBlocks.empty()) {
			currentCompiledLine.throwException("No active code block.");
			return new CompileBlock(this);
		}
		return currentBlocks.peek();
	}

	public NCompilableMethod getMainMethod() {
		if (currentClass != null) {
			String desiredName = LangConstants.makePath(currentClass.className, LangConstants.MAIN_METHOD_NAME);
			for (NCompilableMethod m : methods) {
				if (m.def.name.equals(desiredName) && !m.hasModifier(Modifier.NATIVE)) {
					return m;
				}
			}
		}
		return null;
	}

	public ClassDefinition findClass(String name) {
		for (ClassDefinition d : classDefs) {
			if (d.hasName(name)) {
				return d;
			}
		}
		return null;
	}

	public ClassDefinition resolveClass(String name) {
		ClassDefinition d = findClass(name);
		if (d == null) {
			d = findClass(LangConstants.makePath(packageName, name));
		}
		if (d != null) {
			return d;
		}
		for (ClassDefinition cd : classDefs) {
			//System.err.println("has cd " + cd.className + "; " + cd.aliases);
		}
		currentCompiledLine.throwException("Could not resolve class " + name);
		return null;
	}

	private AInstruction getInstructionByLabelSafe(String label) {
		for (NCompilableMethod m : methods) {
			for (AInstruction i : m.body) {
				if (i.hasLabel(label)) {
					return i;
				}
			}
		}
		return null;
	}

	public AInstruction getInstructionByLabel(String label) {
		AInstruction ins = getInstructionByLabelSafe(label);
		if (ins == null) {
			throw new IllegalArgumentException("Jump label refers to non-existent instruction. " + label);
		}
		return ins;
	}

	public void finishCompileLoad() {
		//end static initializers
		staticInitializers.addInstruction(getPlain(APlainOpCode.RETURN));
		//check that all labels were correctly linked
		for (LabelRequest reqLabel : requestedLabels) {
			if (getInstructionByLabelSafe(reqLabel.label) == null) {
				reqLabel.source.throwException("Label not found: " + reqLabel.label);
			}
		}
	}

	public void addNative(InboundDefinition def) {
		if (!methodHeaders.contains(def)) {
			methodHeaders.add(def);
			//do not change current method
			int addind = methods.size() - 1;
			if (addind < 0) {
				addind = 0;
			}
			methods.add(addind, new NCompilableMethod(def));
		}
	}

	public int getNativeIdx(String name) {
		int nidx = 0;
		for (NCompilableMethod method : methods) {
			if (method.hasModifier(Modifier.NATIVE)) {
				if (method.def.hasName(name)) {
					return nidx;
				}
				nidx++;
			}
		}
		for (NCompilableMethod method : methods) {
			if (method.hasModifier(Modifier.NATIVE)) {
				System.err.println("Has native: " + method.def);
			}
		}
		throw new RuntimeException("Could not find native method: " + name);
	}

	public void addLibrary(String name) {
		if (!libraries.contains(name)) {
			libraries.add(name);
		}
	}

	public void popBlock() {
		if (currentBlocks.empty()) {
			currentCompiledLine.throwException("Unexpected end of block.");
			return;
		}
		CompileBlock blk = currentBlocks.pop();
		addInstructions(blk.blockEndInstructions);

		blockHistory.push(blk);
		boolean hasReturn = false;
		boolean isMethodBlk = methodBlocks.contains(blk);

		if (blk.blockEndControlInstruction != null) {
			if (blk.blockEndControlInstruction.getType() == AInstructionType.CASE_TABLE) {
				ACaseTable casetbl = (ACaseTable) blk.blockEndControlInstruction;
				if (casetbl.defaultCase == null) {
					//System.out.println("null defcase at " + blk.fullBlockName);
					casetbl.defaultCase = blk.getBlockTermFullLabel();
				}
			}
			boolean noAddBECI = false;
			if (blk.blockEndControlInstruction.getType() == AInstructionType.JUMP) {
				//If the block ends with an unconditional jump (if blocks), but the last instruction is a return, omit it

				if (((AConditionJump) blk.blockEndControlInstruction).getOpCode() == APlainOpCode.JUMP) {
					List<AInstruction> instructions = getCurrentMethod().body;
					if (!instructions.isEmpty()) {
						AInstruction last = instructions.get(instructions.size() - 1);
						if (last.getType() == AInstructionType.PLAIN) {
							if (((APlainInstruction) last).opCode == APlainOpCode.RETURN) {
								noAddBECI = true;
							}
						}
					}
				}
			}
			if (!noAddBECI) {
				addInstruction(blk.blockEndControlInstruction);
			}
		}
		pendingLabels.add(new PendingLabel(blk.getBlockTermLabel(), blk));

		if (isMethodBlk) {
			NCompilableMethod m = getCurrentMethod();
			if (!m.body.isEmpty()) {
				AInstruction last = m.body.get(m.body.size() - 1);
				if (last instanceof APlainInstruction) {
					APlainInstruction pi = (APlainInstruction) last;
					if (pi.opCode == APlainOpCode.RETURN) {
						hasReturn = true;
						pendingLabels.add(new PendingLabel(blk.getBlockHaltLabel(), blk));
						for (PendingLabel l : pendingLabels) {
							pi.addLabel(l.getAddableLabel());
						}
						pendingLabels.clear();
					}
				}
			}
			if (!hasReturn && m.def.retnType.baseType != DataType.VOID) {
				currentCompiledLine.throwException("Method " + m.def.name + " does not return a value!");
			}
		}
		if (!hasReturn) {
			pendingLabels.add(new PendingLabel(blk.getBlockHaltLabel(), blk));
			blk.gracefullyTerminate(this);
			if (isMethodBlk) {
				addInstruction(getPlain(APlainOpCode.ZERO_PRI));
				addInstruction(getPlain(APlainOpCode.RETURN));
			}
		} else {
			blk.updateBlockStackIns();
		}

		/*if (isMethodBlk) {
			NCompilableMethod m = getCurrentMethod();
			if (m.metaHandler != null) {
				m.metaHandler.onCompileEnd(this, m);
			}
		}*/

		if (blk.popNext) {
			popBlock();
		}
	}

	public void addMethod(NCompilableMethod method, boolean hasBody) {
		method.def.annotations.addAll(pendingAnnotations);
		pendingAnnotations.clear();
		methods.add(method);

		if (!staticInitializers.isBlankBody()) {
			//Inject static initializer call to all methods, but only if there is an initialization needed
			//Global inits are predeclared, so at this stage, the static initializer method should be final
			//This check could be performed in the static initializer itself instead, but inlining saves us a few pushes and a call
			if (hasBody && !method.hasModifier(Modifier.NATIVE) && !method.hasModifier(Modifier.META) && method.hasModifier(Modifier.STATIC) && method.hasModifier(Modifier.PUBLIC)) {
				//add static initializer call BEFORE blocks and labels are set up
				addInstruction(getVarRead(gNeedsStaticInit));															//Read initializer status
				addInstruction(getConditionJump(APlainOpCode.JUMP_IF_ZERO, "__static_init_if_end"));					//If no longer needs initialization, skip
				addInstruction(getMethodCall(staticInitializers.def.createBlankOutbound(), staticInitializers.def));	//Call static initializer method
				addPendingLabel("__static_init_if_end");																//Prepare label for initialization skip
			}
		}

		if (hasBody) {
			CompileBlock methodBlk = new CompileBlock(this);
			methodBlk.setShortBlockLabel("method_" + method.def.name);
			methodBlocks.push(methodBlk);
			pushBlock(methodBlk);
		}

		/*if (method.metaHandler != null) {
			method.metaHandler.onCompileBegin(this, method);
		}*/
	}

	public void addGlobal(Variable.Global glb) {
		glb.annotations.addAll(pendingAnnotations);
		pendingAnnotations.clear();
		globals.addVariable(glb, this);
		if (!glb.isImmediate()) {
			staticInitializers.addInstructions(glb.init_from);
		}
	}

	public PendingLabel addPendingLabel(String label) {
		PendingLabel pl = new PendingLabel(label, currentBlocks.isEmpty() ? null : getCurrentBlock());
		pendingLabels.add(pl);
		return pl;
	}

	public void addLabelRequest(String label) {
		LabelRequest req = new LabelRequest(currentCompiledLine, label);
		requestedLabels.add(req);
	}

	public void addPendingAnnot(CompilerAnnotation ant) {
		pendingAnnotations.add(ant);
	}

	public String getLabelByBlock(String label) {
		if (currentBlocks.isEmpty()) {
			return label;
		} else {
			return getCurrentBlock() + "_" + label;
		}
	}

	public boolean hasPragma(CompilerPragma prg) {
		return pragmata.containsKey(prg);
	}

	public boolean getIsBoolPragmaEnabledSimple(CompilerPragma prg) {
		if (hasPragma(prg)) {
			return pragmata.get(prg).boolValue();
		}
		return false;
	}

	public int getIntPragma(CompilerPragma prg) {
		if (hasPragma(prg)) {
			return pragmata.get(prg).intValue();
		}
		return 0;
	}

	public void pushBlock(CompileBlock block) {
		boolean skipAddingLabel = false;
		if (!pendingLabels.isEmpty()) {
			PendingLabel l = pendingLabels.get(pendingLabels.size() - 1);
			if (l.isUserLabel) {
				block.setShortBlockLabel(l.shortName);
				//skipAddingLabel = true;
			}
			//pendingLabels.remove(l); //keep the label
		}
		CompileBlock lastBlock = null;
		if (!currentBlocks.empty()) {
			lastBlock = getCurrentBlock();
			block.fullBlockName = lastBlock.fullBlockName + "_" + block.getShortBlockName();
		} else {
			block.fullBlockName = graphUID + "_" + block.getShortBlockName();
		}
		currentBlocks.push(block);
		if (!block.explicitAdjustStack) {
			//addInstruction(block.blockBeginAdjustStackIns);
		}
		if (!skipAddingLabel) { //unused: always multi-label
			pendingLabels.add(new PendingLabel(block.getShortBlockName(), lastBlock));
		} else {
			//System.out.println("skip label " + block.fullBlockName);
		}
	}

	public void addLocal(Variable.Local l) {
		if (getCurrentMethod().locals == null) {
			throw new RuntimeException("Internal error: Method " + getCurrentMethod().def + " not initialized !!");
		}
		getCurrentMethod().locals.addVariable(l, this);
		getCurrentBlock().localsOfThisBlock.add(l);
	}

	public void removeLocals(List<Variable> l) {
		NCompilableMethod m = getCurrentMethod();
		//System.out.println(m.def.name);
		if (m.locals != null) {
			m.locals.removeAll(l, this);
			writeLocalsRemoveIns(l);
		}
	}

	public void writeLocalsRemoveIns(List<Variable> l) {
		addInstruction(getLocalsRemoveIns(l));
	}

	public AInstruction getLocalsRemoveIns(List<Variable> l) {
		return getPlain(APlainOpCode.RESIZE_STACK, -l.size());
	}

	public void addInstruction(AInstruction i) {
		if (i != null) {
			for (PendingLabel lbl : pendingLabels) {
				i.addLabel(lbl.getAddableLabel());
			}
			pendingLabels.clear();
			getCurrentMethod().addInstruction(i);
		}
	}

	public void addInstructions(List<AInstruction> l) {
		for (AInstruction i : l) {
			addInstruction(i);
		}
	}

	public APlainInstruction getPlain(APlainOpCode opCode) {
		return getPlain(opCode, new int[0]);
	}

	public APlainInstruction getPlain(APlainOpCode opCode, int... args) {
		return new APlainInstruction(opCode, args);
	}

	public AFloatInstruction getPlainFloat(AFloatOpCode opCode) {
		return getPlainFloat(opCode, new float[0]);
	}

	public AFloatInstruction getPlainFloat(AFloatOpCode opCode, float... args) {
		return new AFloatInstruction(opCode, args);
	}

	public AConditionJump getConditionJump(APlainOpCode opCode, String label) {
		return new AConditionJump(opCode, label);
	}

	public ACaseTable getCaseTable() {
		return new ACaseTable();
	}

	public ALocalCall getMethodCall(OutboundDefinition def, InboundDefinition resolvedMethod) {
		def.name = resolvedMethod.name;
		if (resolvedMethod.hasModifier(Modifier.META)) {
			return new MetaCall(def);
		} else if (resolvedMethod.hasModifier(Modifier.NATIVE)) {
			return new ANativeCall(def);
		} else {
			return new ALocalCall(def);
		}
	}

	public AAccessVariable.AReadVariable getVarRead(Variable var) {
		return new AAccessVariable.AReadVariable(var);
	}

	public AAccessVariable.AWriteVariable getVarWrite(Variable var) {
		return new AAccessVariable.AWriteVariable(var);
	}

	public boolean hasMethods() {
		return !methods.isEmpty();
	}
	
	public InboundDefinition findBestMatchingMethod(OutboundDefinition def) {
		return resolveMethod(def, true);
	}

	public InboundDefinition resolveMethod(OutboundDefinition def) {
		return resolveMethod(def, false);
	}
	
	public InboundDefinition resolveMethod(OutboundDefinition def, boolean dirty) {
		InboundDefinition m = findMethod(def, dirty);
		if (m != null) {
			return m;
		}
		if (currentClass != null) {
			OutboundDefinition def2 = new OutboundDefinition(LangConstants.makePath(currentClass.className, def.name), def.args);
			m = findMethod(def2);
		}
		if (m != null) {
			return m;
		}

		String filePath = def.name.replace('.', '/');
		if (filePath.contains("/")) {
			//need to compile another file
			ensureIncludeForPath(filePath);
		}
		m = findMethod(def, dirty);
		if (m != null) {
			return m;
		} else {
			return null;
		}
	}

	public InboundDefinition findMethod(InboundDefinition def) {
		for (NCompilableMethod m : methods) {
			if (m.def.equals(def)) {
				return m.def;
			}
		}
		for (InboundDefinition d : methodHeaders) {
			if (d.equals(def)) {
				return d;
			}
		}
		return null;
	}

	public InboundDefinition findMethod(OutboundDefinition def) {
		return findMethod(def, false);
	}
	
	public InboundDefinition findMethod(OutboundDefinition def, boolean dirty) {
		//System.out.println("req " + def);
		for (NCompilableMethod m : methods) {
			//System.out.println("method " + m.def);
			if (m.def.accepts(def, dirty)) {
				return m.def;
			}
		}
		for (InboundDefinition d : methodHeaders) {
			if (d.accepts(def, dirty)) {
				return d;
			}
		}
		return null;
	}

	public NCompilableMethod getMethodByDef(OutboundDefinition out) {
		for (NCompilableMethod m : methods) {
			if (m.def.accepts(out)) {
				return m;
			}
		}
		return null;
	}

	public Variable.Global resolveGVar(String name) {
		Variable.Global v = findGVar(name);
		if (v != null) {
			return v;
		}
		if (currentClass != null) {
			v = findGVar(currentClass.className + "." + name);
		}
		if (v != null) {
			return v;
		}

		String filePath = name.replace('.', '/');
		if (filePath.contains("/")) {
			//need to compile another file
			ensureIncludeForPath(filePath);
		}
		v = findGVar(name);
		if (v != null) {
			return v;
		} else {
			return null;
		}
	}

	public Variable.Global findGVar(String name) {
		//System.out.println("have total " + globals.variables.size() + " globals");
		for (Variable g : globals) {
			if (g.hasName(name)) {
				return (Variable.Global) g;
			}
		}
		return null;
	}

	public boolean ensureIncludeForPath(String path) {
		if (includedClasses.contains(path)) {
			return true;
		}

		String[] pathElems = path.replace('.', '/').split("/");

		List<FSFile> candidates = new ArrayList<>();
		Map<FSFile, FSFile> candidateRoots = new HashMap<>();
		List<Integer> cPEIdx = new ArrayList<>();

		for (FSFile inc : includePaths) {
			FSFile f = inc;
			boolean notFoundBreak = false;
			for (int i = 0; i < pathElems.length; i++) {
				String pe = pathElems[i];
				FSFile f2 = f.getChild(pe);
				if (f2 == null || !f2.exists()) {
					for (String ext : LangConstants.SUPPORTED_EXTENSIONS) {
						f2 = f.getChild(pe + ext);
						if (f2 != null && f2.exists()) {
							break;
						}
					}
					if (f2 == null || !f2.exists()) {
						cPEIdx.add(i);
						notFoundBreak = true;
						break; //f stays as is without the f2 continuation
					}
				}
				f = f2;
			}
			candidates.add(f);
			if (!notFoundBreak) {
				cPEIdx.add(pathElems.length);
			}
			candidateRoots.put(f, inc);
		}
		List<FSFile> matchingCandidates = new ArrayList<>();
		Map<FSFile, Preprocessor> compilers = new HashMap<>();

		for (FSFile cand : candidates) {
			if (cand.isDirectory()) {
				continue;
			}
			Preprocessor nlr = null;
			try {
				nlr = new Preprocessor(cand, args, this);
			} catch (Exception ex) {
				currentCompiledLine.throwException("Exception while importing " + path + ": " + ex.getMessage());
			}

			if (nlr != null) {
				boolean found = false;
				int i0 = cPEIdx.get(candidates.indexOf(cand));
				if (pathElems.length == i0) {
					found = true;
				} else {
					StringBuilder remainder = new StringBuilder();
					for (int i = i0; i < pathElems.length; i++) {
						if (i != i0) {
							remainder.append('.');
						}
						remainder.append(pathElems[i]);
					}
					String toLookup = remainder.toString();
					for (String field : nlr.getDeclaredFields()) {
						if (field.equals(toLookup)) {
							found = true;
							break;
						}
					}
					if (!found) {
						for (InboundDefinition m : nlr.getDeclaredMethods()) {
							if (m.name.equals(toLookup)) {
								found = true;
								break;
							}
						}
					}
				}
				if (found) {
					matchingCandidates.add(cand);
					compilers.put(cand, nlr);
				}
			}
		}

		switch (matchingCandidates.size()) {
			case 0:
				return false;
			case 1:
				FSFile c = matchingCandidates.get(0);
				Preprocessor candComp = compilers.get(c);
				includedReaders.add(candComp);
				NCompileGraph candCG = candComp.getCompileGraph();
				if (candCG == null) {
					return false;
				}
				merge(candCG);
				break;
			default:
				String errmsg = "Ambiguous symbol \"" + path + "\" :\n";
				
				for (FSFile f : matchingCandidates) {
					errmsg += f.getPath();
					errmsg += "\n";
				}
				
				currentCompiledLine.throwException(errmsg);
				break;
		}
		return true;
	}

	public void applyImport(String importName) {
		importName += LangConstants.CH_PATH_SEPARATOR;
		for (ClassDefinition cd : classDefs) {
			if (cd.className.startsWith(importName)) {
				String newName = cd.className.substring(importName.length());
				if (findClass(newName) == null) {
					cd.aliases.add(newName);
				}
			}
		}
		for (NCompilableMethod m : methods) {
			if (m.def.name.startsWith(importName)) {
				String newName = m.def.name.substring(importName.length());
				String match = findMethodMatchName(newName);
				if (match != null) {
					InboundDefinition mm = findMethodMatch(match);
					if (mm != null) {
						if (!mm.name.equals(m.def.name)) {
							continue;
						}
					}
				}
				m.def.aliases.add(newName);
			}
		}
		for (Variable glb : globals) {
			if (glb.name.startsWith(importName)) {
				String newName = glb.name.substring(importName.length());
				if (findGlobalMatchName(newName) == null) {
					glb.aliases.add(newName);
				}
			}
		}
	}

	public void merge(NCompileGraph cg) {
		for (NCompilableMethod m : cg.methods) {
			if (methods.contains(m)) {
				continue;
			}
			if (findMethod(m.def) == null) {
				methods.add(0, m);
			}
		}
		for (Variable glb : cg.globals) {
			if (globals.variables.contains(glb)) {
				continue;
			}
			globals.addVariable(glb, this);
		}
		for (ClassDefinition cd : cg.classDefs) {
			if (classDefs.contains(cd)) {
				continue;
			}
			if (findClass(cd.className) == null) {
				classDefs.add(cd);
			}
		}
		for (String ins : cg.includedClasses) {
			if (!includedClasses.contains(ins)) {
				includedClasses.add(ins);
			}
		}
		pragmata.putAll(cg.pragmata);
	}

	public String findGlobalMatchName(String name) {
		for (Variable v : globals) {
			if (v.name.equals(name)) {
				return v.name;
			}
			for (String a : v.aliases) {
				if (a.equals(name)) {
					return a;
				}
			}
		}
		return null;
	}

	public InboundDefinition findMethodMatch(String name) {
		for (NCompilableMethod m : methods) {
			if (m.def.name.equals(name)) {
				return m.def;
			}
			for (String a : m.def.aliases) {
				if (a.equals(name)) {
					return m.def;
				}
			}
		}
		return null;
	}

	public String findMethodMatchName(String name) {
		for (NCompilableMethod m : methods) {
			if (m.def.name.equals(name)) {
				return m.def.name;
			}
			for (String a : m.def.aliases) {
				if (a.equals(name)) {
					return a;
				}
			}
		}
		return null;
	}

	public Variable resolveLocal(String name) {
		if (hasMethods()) {
			LocalDataGraph locals = getCurrentMethod().locals;
			if (locals != null) {
				for (Variable l : locals) {
					if (l.name.equals(name)) {
						return l;
					}
				}
			}
		}
		return null;
	}

	public static class LabelRequest {

		public EffectiveLine source;
		public String label;

		public LabelRequest(EffectiveLine line, String label) {
			this.source = line;
			this.label = label;
		}
	}
}
