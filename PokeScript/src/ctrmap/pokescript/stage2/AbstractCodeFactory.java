package ctrmap.pokescript.stage2;

import ctrmap.pokescript.InboundDefinition;
import ctrmap.pokescript.data.Variable;
import ctrmap.pokescript.instructions.abstractcommands.*;
import ctrmap.pokescript.instructions.providers.MetaFunctionHandler;
import ctrmap.pokescript.stage0.Modifier;
import ctrmap.pokescript.stage1.NCompilableMethod;
import ctrmap.pokescript.stage1.NCompileGraph;
import xstandard.util.ArraysEx;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractCodeFactory<I> {

	protected Map<String, I> labels = new HashMap<>();
	protected Map<NCompilableMethod, I> methods = new HashMap<>();

	protected Map<I, String> pendingSimpleJumps = new HashMap<>();
	protected Map<I, TempJumptable> pendingJumptables = new HashMap<>();

	protected Map<I, NCompilableMethod> pendingMethodCalls = new HashMap<>();
	protected Map<I, NCompilableMethod> pendingMetaCalls = new HashMap<>();
	protected Map<I, InboundDefinition> pendingNativeCalls = new HashMap<>();

	protected List<NCompilableMethod> pendingMethods = new ArrayList<>();
	protected List<String> pendingLabels = new ArrayList<>();

	protected List<I> output = new ArrayList<>();

	protected final NCompileGraph cg;

	protected List<NCompilableMethod> usedMethods = new ArrayList<>();
	protected List<Variable.Global> usedGlobals = new ArrayList<>();

	protected List<NCompilableMethod> publics = new ArrayList<>();
	protected List<String> libraries = new ArrayList<>();
	protected List<String> natives = new ArrayList<>();
	protected List<Variable.Global> pubvars = new ArrayList<>();

	private final boolean stripUnused = true;

	public AbstractCodeFactory(NCompileGraph cg) {
		this.cg = cg;
	}

	public void prepare() {
		NCompilableMethod main = cg.getMainMethod();

		for (Variable g : cg.globals.variables) {
			if (!stripUnused || g.hasModifier(Modifier.PUBLIC)) {
				Variable.Global gg = (Variable.Global) g;
				usedGlobals.add(gg);
				registerGlobal(gg);
			}
		}

		for (NCompilableMethod method : cg.methods) {
			if (stripUnused) {
				if (method.hasModifier(Modifier.PUBLIC) || method == main) {
					loadUsedMethods(method);
				}
			} else {
				usedMethods.add(method);
			}
		}

		for (NCompilableMethod m : usedMethods) {
			registerMethod(m);
		}
	}

	public void compile() {
		for (NCompilableMethod m : usedMethods) {
			if (!m.hasModifier(Modifier.NATIVE)) {
				assembleMethod(m);
			}
		}
	}

	private void loadUsedMethods(NCompilableMethod usedMethod) {
		ArraysEx.addIfNotNullOrContains(usedMethods, usedMethod);

		for (AInstruction instructionBatch : usedMethod.body) {
			for (AInstruction ins : instructionBatch.getAllInstructions()) {
				switch (ins.getType()) {
					case CALL_LOCAL:
					case CALL_META:
					case CALL_NATIVE:
						ALocalCall call = (ALocalCall) ins;
						NCompilableMethod m = cg.getMethodByDef(call.call);
						if (m != null) {
							ArraysEx.addIfNotNullOrContains(usedMethods, m);
							loadUsedMethods(m);
						}
						break;
					case GET_VARIABLE:
					case SET_VARIABLE:
						AAccessVariable varaccess = (AAccessVariable) ins;
						if (varaccess.var.getLocation() == Variable.VarLoc.DATA) {
							usedGlobals.add((Variable.Global) varaccess.var);
						}
						break;
				}
			}
		}
	}

	public void addLabel(String label) {
		pendingLabels.add(label);
	}

	public void addJumpInstruction(I ins, String targetLabel) {
		addInstruction(ins);
		pendingSimpleJumps.put(ins, targetLabel);
	}

	public void addNativeCallInstruction(I ins, InboundDefinition def) {
		addInstruction(ins);
		if (def != null) {
			pendingNativeCalls.put(ins, def);
		}
	}

	public I addInstruction(I ins) {
		while (!pendingLabels.isEmpty()) {
			labels.put(pendingLabels.remove(0), ins);
		}
		while (!pendingMethods.isEmpty()) {
			methods.put(pendingMethods.remove(0), ins);
		}
		output.add(ins);
		return ins;
	}

	public abstract void addNative(InboundDefinition def);

	public void registerMethod(NCompilableMethod method) {
		if (method.hasModifier(Modifier.NATIVE)) {
			addNative(method.def);
		} else if (method.hasModifier(Modifier.PUBLIC) && method.hasModifier(Modifier.STATIC) && !method.hasModifier(Modifier.META)) {
			publics.add(method);
		}
	}

	public void registerGlobal(Variable.Global gvar) {
		if (gvar.hasModifier(Modifier.PUBLIC)) {
			pubvars.add(gvar);
		}
	}

	public Iterable<Variable.Global> globals() {
		return usedGlobals;
	}

	public Iterable<String> nativeNames() {
		return natives;
	}

	public Iterable<String> libraryNames() {
		return libraries;
	}

	public Iterable<Variable.Global> pubVars() {
		return pubvars;
	}

	public void assembleMethod(NCompilableMethod m) {
		if (!m.hasModifier(Modifier.NATIVE)) {
			if (m.hasModifier(Modifier.META)) {
				if (!m.isBodiless() && m.metaHandler != null) {
					pendingMethods.add(m);
					assembleMeta(m.body, m.metaHandler);
				}
			} else {
				pendingMethods.add(m);
				assemble(m.body);
			}
		}
	}

	public void assemble(List<AInstruction> instructions) {
		for (AInstruction i : instructions) {
			assemble(i);
		}
	}

	public void assembleMeta(List<AInstruction> instructions, MetaFunctionHandler handler) {
		for (AInstruction i : instructions) {
			assembleMeta(i, handler);
		}
	}

	public void assembleMeta(AInstruction instruction, MetaFunctionHandler handler) {
		if (!handler.assembleMetaInstruction(this, instruction, cg)) {
			assemble(instruction);
		}
	}

	public void assemble(AInstruction instruction) {
		for (String label : instruction.labels()) {
			addLabel(label);
		}
		switch (instruction.getType()) {
			case PLAIN:
				assemblePlain((APlainInstruction) instruction);
				break;
			case PLAIN_FLOAT:
				assembleVFP((AFloatInstruction) instruction);
				break;
			case JUMP:
				pendingSimpleJumps.put(assembleJump((AConditionJump) instruction), ((AConditionJump) instruction).targetLabel);
				break;
			case CALL_LOCAL: {
				ALocalCall call = (ALocalCall) instruction;
				NCompilableMethod m = cg.getMethodByDef(call.call);
				if (m != null) {
					pendingMethodCalls.put(assembleLocalCall(call), m);
				}
				break;
			}
			case CALL_NATIVE: {
				ANativeCall call = (ANativeCall) instruction;
				InboundDefinition natv = cg.findMethod(call.call);
				if (natv != null) {
					pendingNativeCalls.put(assembleNativeCall(call), natv);
				}
				break;
			}
			case CASE_TABLE:
				assembleCaseTable((ACaseTable) instruction);
				break;
			case SET_VARIABLE:
				assembleVarSet((AAccessVariable.AWriteVariable) instruction);
				break;
			case GET_VARIABLE:
				assembleVarGet((AAccessVariable.AReadVariable) instruction);
				break;
			case CALL_META: {
				MetaCall call = (MetaCall) instruction;
				NCompilableMethod meta = cg.getMethodByDef(call.call);
				if (meta != null) {
					I metaCall = assembleMetaCall(call);
					if (metaCall != null) {
						pendingMetaCalls.put(metaCall, meta);
					}
				}
				break;
			}
		}
	}
	
	public abstract int getVariablePointer(Variable var);

	public abstract void assembleVarSet(AAccessVariable.AWriteVariable instruction);

	public abstract void assembleVarGet(AAccessVariable.AReadVariable instruction);

	public abstract void assemblePlain(APlainInstruction instruction);

	public abstract I assembleJump(AConditionJump instruction);

	public abstract void assembleVFP(AFloatInstruction instruction);

	public abstract I assembleLocalCall(ALocalCall instruction);

	public abstract I assembleNativeCall(ANativeCall instruction);

	public abstract void assembleCaseTable(ACaseTable instruction);

	public I assembleMetaCall(MetaCall instruction) {
		NCompilableMethod target = cg.getMethodByDef(instruction.call);

		if (target != null && target.metaHandler != null) {
			return (I) target.metaHandler.compileMetaCall(this, instruction, cg);
		}

		return null;
	}
	
	protected static class TempJumptable {

		public String defaultCase;
		public Map<Integer, String> cases = new HashMap<>();
	}
}
