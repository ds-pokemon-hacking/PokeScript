package ctrmap.pokescript.stage1;

import ctrmap.pokescript.InboundDefinition;
import ctrmap.pokescript.LangCompiler;
import ctrmap.pokescript.LangConstants;
import ctrmap.pokescript.stage0.Preprocessor;
import ctrmap.pokescript.OutboundDefinition;
import ctrmap.pokescript.data.DataGraph;
import ctrmap.pokescript.data.Variable;
import ctrmap.pokescript.instructions.abstractcommands.AAccessGlobal;
import ctrmap.pokescript.instructions.abstractcommands.ACaseTable;
import ctrmap.pokescript.instructions.abstractcommands.AConditionJump;
import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.instructions.abstractcommands.ALocalCall;
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
import ctrmap.pokescript.types.TypeDef;
import ctrmap.pokescript.types.classes.ClassDefinition;
import ctrmap.stdlib.fs.FSFile;
import ctrmap.stdlib.fs.FSUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NCompileGraph {

	public DataGraph globals = new DataGraph();

	public List<String> natives = new ArrayList<>();
	public List<String> libraries = new ArrayList<>();

	public List<ClassDefinition> classDefs = new ArrayList<>();

	public List<InboundDefinition> methodHeaders = new ArrayList<>();
	public List<NCompilableMethod> methods = new ArrayList<>();

	public List<FSFile> includePaths = new ArrayList<>();
	public List<Preprocessor> includedReaders = new ArrayList<>();

	public BlockStack<CompileBlock> currentBlocks = new BlockStack<>();
	public BlockStack<CompileBlock> blockHistory = new BlockStack<>();
	public BlockStack<CompileBlock> methodBlocks = new BlockStack<>();

	private List<PendingLabel> pendingLabels = new ArrayList<>();
	private List<LabelRequest> requestedLabels = new ArrayList<>();
	private List<CompilerAnnotation> pendingAnnotations = new ArrayList<>();

	private String graphUID;
	private LangCompiler.CompilerArguments args;

	public EffectiveLine currentCompiledLine;
	public String appliedNamespace;
	public List<String> importedNamespaces = new ArrayList<>();
	public List<String> includedClasses = new ArrayList<>();

	public AInstructionProvider provider;

	public NCompileGraph(LangCompiler.CompilerArguments args) {
		graphUID = UUID.randomUUID().toString();
		this.args = args;
		this.provider = args.provider;
	}

	public LangCompiler.CompilerArguments getArgs() {
		return args;
	}

	public NCompilableMethod getCurrentMethod() {
		if (!hasMethods()) {
			currentCompiledLine.throwException("Can not request a method without any.");
			return new NCompilableMethod("dummy", new ArrayList<Modifier>(), new DeclarationContent.Argument[0], new TypeDef(DataType.INT.getFriendlyName()));
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
		for (NCompilableMethod m : methods) {
			if (m.def.name.equals("main") && !m.hasModifier(Modifier.NATIVE)) {
				return m;
			}
		}
		return null;
	}

	public ClassDefinition getClassByName(String name) {
		for (ClassDefinition d : classDefs) {
			if (d.hasName(name)) {
				return d;
			}
		}
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
		}
	}

	public void prepareNativeTable() {
		for (NCompilableMethod m : methods) {
			if (m.hasModifier(Modifier.NATIVE) && !natives.contains(m.def.name)) {
				natives.add(m.def.name);
			}
		}
	}

	public int getNativeIdx(String name) {
		return natives.indexOf(name);
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
		if (blk.blockEndControlInstruction != null) {
			if (blk.blockEndControlInstruction instanceof ACaseTable) {
				ACaseTable casetbl = (ACaseTable) blk.blockEndControlInstruction;
				if (casetbl.defaultCase == null) {
					casetbl.defaultCase = blk.getBlockTermFullLabel();
				}
			}
			boolean noAddBECI = false;
			if (blk.blockEndControlInstruction instanceof AConditionJump) {
				//If the block ends with an unconditional jump (if blocks), but the last instruction is a return, omit it
				
				if (((AConditionJump)blk.blockEndControlInstruction).getOpCode() == APlainOpCode.JUMP){
					List<AInstruction> instructions = getCurrentMethod().body;
					if (!instructions.isEmpty()){
						AInstruction last = instructions.get(instructions.size() - 1);
						if (last instanceof APlainInstruction){
							if (((APlainInstruction)last).opCode == APlainOpCode.RETURN){
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
		blockHistory.push(blk);
		boolean hasReturn = false;
		boolean isMethodBlk = methodBlocks.contains(blk);

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

		if (isMethodBlk) {
			NCompilableMethod m = getCurrentMethod();
			if (m.metaHandler != null) {
				m.metaHandler.onCompileEnd(this, m);
			}
		}

		if (blk.popNext) {
			popBlock();
		}
	}

	public void addMethod(NCompilableMethod method) {
		CompileBlock methodBlk = new CompileBlock(this);
		methodBlocks.push(methodBlk);
		method.def.annotations.addAll(pendingAnnotations);
		pendingAnnotations.clear();
		methods.add(method);
		pushBlock(methodBlk);

		if (method.metaHandler != null) {
			method.metaHandler.onCompileBegin(this, method);
		}
	}

	public void addGlobal(Variable.Global glb) {
		glb.annotations.addAll(pendingAnnotations);
		pendingAnnotations.clear();
		globals.addVariable(glb, this);
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
		return args.pragmata.containsKey(prg);
	}

	public boolean getIsBoolPragmaEnabledSimple(CompilerPragma prg) {
		if (hasPragma(prg)) {
			return args.pragmata.get(prg).boolValue();
		}
		return false;
	}

	public int getIntPragma(CompilerPragma prg) {
		if (hasPragma(prg)) {
			return args.pragmata.get(prg).intValue();
		}
		return 0;
	}

	public void pushBlock(CompileBlock block) {
		boolean skipAddingLabel = false;
		if (!pendingLabels.isEmpty()) {
			PendingLabel l = pendingLabels.get(pendingLabels.size() - 1);
			if (l.isUserLabel) {
				block.setShortBlockLabel(l.shortName);
			}
			//pendingLabels.remove(l); //keep the label
			skipAddingLabel = true;
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
			addInstruction(block.blockBeginAdjustStackIns);
		}
		if (!skipAddingLabel) {
			pendingLabels.add(new PendingLabel(block.getShortBlockName(), lastBlock));
		}
	}

	public void addLocal(Variable.Local l) {
		getCurrentMethod().locals.addVariable(l, this);
		getCurrentBlock().localsOfThisBlock.add(l);
	}

	public void removeLocals(List<Variable> l) {
		getCurrentMethod().locals.removeAll(l, this);
		writeLocalsRemoveIns(l);
	}

	public void writeLocalsRemoveIns(List<Variable> l) {
		addInstruction(getLocalsRemoveIns(l));
	}

	public AInstruction getLocalsRemoveIns(List<Variable> l) {
		return getPlain(APlainOpCode.RESIZE_STACK, l.size() * provider.getMemoryInfo().getStackIndexingStep());
	}

	public void addInstruction(AInstruction i) {
		for (PendingLabel lbl : pendingLabels) {
			i.addLabel(lbl.getAddableLabel());
		}
		pendingLabels.clear();
		getCurrentMethod().addInstruction(i);
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
		return provider.getPlainInstruction(opCode, args);
	}

	public ALocalCall getMethodCall(OutboundDefinition def, InboundDefinition resolvedMethod) {
		if (resolvedMethod.hasModifier(Modifier.META)) {
			return new MetaCall(def);
		} else if (resolvedMethod.hasModifier(Modifier.NATIVE)) {
			return provider.getNativeCall(def);
		} else {
			return provider.getMethodCall(def);
		}
	}

	public boolean hasMethods() {
		return !methods.isEmpty();
	}

	public InboundDefinition resolveMethod(OutboundDefinition def) {
		InboundDefinition m = findMethod(def);
		if (m != null) {
			return m;
		}

		String filePath = def.name.replace('.', '/');
		if (filePath.contains("/")) {
			//need to compile another file
			ensureIncludeForPath(filePath);
		}
		m = findMethod(def);
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
		for (NCompilableMethod m : methods) {
			if (m.def.accepts(def)) {
				return m.def;
			}
		}
		for (InboundDefinition d : methodHeaders) {
			if (d.accepts(def)) {
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
					for (String ext : new String[]{LangConstants.NATIVE_DEFINITION_EXTENSION, LangConstants.LANG_SOURCE_FILE_EXTENSION}) {
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
				candComp.include = includePaths;
				NCompileGraph candCG = candComp.getCompileGraph();
				if (candCG == null) {
					return false;
				}
				String candidatePathRelative = c.getPathRelativeTo(candidateRoots.get(c));
				candidatePathRelative = candidatePathRelative.replace('/', '.');
				candCG.applyNameSpace(candidatePathRelative);
				merge(candCG);
				break;
			default:
				currentCompiledLine.throwException("Ambiguous symbol: " + path);
		}
		return true;
	}

	public void applyNameSpace(String ns) {
		if (appliedNamespace != null) {
			throw new UnsupportedOperationException("Namespace " + appliedNamespace + " is already applied - can not replace.");
		}
		ns = FSUtil.getFileNameWithoutExtension(ns);
		for (NCompilableMethod m : methods) {
			for (AInstruction i0 : m.body) {
				for (AInstruction ins : i0.getAllInstructions()) {
					if (ins instanceof ALocalCall) {
						ALocalCall c = (ALocalCall) ins;
						InboundDefinition tgt = findMethod(c.call);
						if (!tgt.isNameAbsolute) {
							c.call.name = ns + "." + c.call.name;
						}
					} else if (ins instanceof AAccessGlobal) {
						AAccessGlobal a = (AAccessGlobal) ins;
						Variable.Global tgt = findGVar(a.gName);
						if (!tgt.isNameAbsolute) {
							a.gName = ns + "." + a.gName;
						}
					}
				}
			}
		}
		for (NCompilableMethod m : methods) {
			if (!m.def.isNameAbsolute) {
				m.def.name = ns + "." + m.def.name;
				m.def.isNameAbsolute = true;
			}
//			m.def.aliases.clear();
		}
		for (Variable glb : globals) {
			if (!glb.isNameAbsolute) {
				glb.name = ns + "." + glb.name;
				glb.isNameAbsolute = true;
			}
//			glb.aliases.clear();
		}
		appliedNamespace = ns;
	}

	public void applyImport(String importName) {
		importName += ".";
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
				methods.add(m);
			}
		}
		for (Variable glb : cg.globals) {
			if (globals.variables.contains(glb)) {
				continue;
			}
			globals.addVariable(glb, this);
		}
		for (String ins : cg.includedClasses) {
			if (!includedClasses.contains(ins)) {
				includedClasses.add(ins);
			}
		}
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
			for (Variable l : getCurrentMethod().locals) {
				if (l.name.equals(name)) {
					return l;
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
