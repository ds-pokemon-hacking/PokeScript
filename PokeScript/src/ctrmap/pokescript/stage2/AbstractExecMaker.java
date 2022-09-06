package ctrmap.pokescript.stage2;

import ctrmap.pokescript.InboundDefinition;
import ctrmap.pokescript.LangCompiler;
import ctrmap.pokescript.data.Variable;
import ctrmap.pokescript.instructions.providers.MetaFunctionHandler;
import ctrmap.pokescript.stage1.NCompilableMethod;
import ctrmap.pokescript.stage1.NCompileGraph;
import java.util.List;
import java.util.Map;

public abstract class AbstractExecMaker<X, I, CF extends AbstractCodeFactory<I>> {
	
	protected X exec;
	protected CF codeFactory;
	
	public AbstractExecMaker<X, I, CF> bindCG(NCompileGraph cg) {
		codeFactory = createCodeFactory(cg);
		codeFactory.prepare();
		return this;
	}
	
	public abstract X newExecutable();
	
	public abstract CF createCodeFactory(NCompileGraph cg);
	
	public AbstractExecMaker<X, I, CF> openNewExecutable() {
		this.exec = newExecutable();
		return this;
	}
	
	public AbstractExecMaker<X, I, CF> openExecutable(X exec) {
		this.exec = exec;
		return this;
	}
	
	public AbstractExecMaker<X, I, CF> compileCode() {
		codeFactory.compile();
		return this;
	}
	
	public X linkCode() {
		resetExec();
		
		for (Variable.Global g : codeFactory.globals()) {
			addGlobal(g);
		}
		
		for (String nativeName : codeFactory.nativeNames()) {
			addNative(nativeName);
		}
		
		for (String libraryName : codeFactory.libraryNames()) {
			addLibrary(libraryName);
		}
		
		for (Variable.Global pubvar : codeFactory.pubvars) {
			addPubVar(pubvar);
		}
		
		addInstructions(codeFactory.output);
		
		calcInstructionPointers();
		
		for (Map.Entry<I, String> jumpEntry : codeFactory.pendingSimpleJumps.entrySet()) {
			I jump = jumpEntry.getKey();
			I jumpTarget = codeFactory.labels.get(jumpEntry.getValue());
			if (jump != null && jumpTarget != null) {
				linkJump(jump, jumpTarget);
			}
		}
		
		for (Map.Entry<I, AbstractCodeFactory.TempJumptable> jumptableEntry : codeFactory.pendingJumptables.entrySet()) {
			I casetbl = jumptableEntry.getKey();
			AbstractCodeFactory.TempJumptable jumptable = jumptableEntry.getValue();
			if (casetbl != null && jumptable != null) {
				linkJumpTable(casetbl, jumptable);
			}
		}
		
		for (Map.Entry<I, InboundDefinition> nativeEntry : codeFactory.pendingNativeCalls.entrySet()) {
			I natvCall = nativeEntry.getKey();
			InboundDefinition def = nativeEntry.getValue();
			if (natvCall != null && def != null) {
				linkNative(natvCall, def);
			}
		}
		
		for (Map.Entry<I, NCompilableMethod> methodCallEntry : codeFactory.pendingMethodCalls.entrySet()) {
			I methodCall = methodCallEntry.getKey();
			I calleeBeginIns = codeFactory.methods.get(methodCallEntry.getValue());
			if (methodCall != null && calleeBeginIns != null) {
				linkCall(methodCall, calleeBeginIns);
			}
		}
		
		for (Map.Entry<I, NCompilableMethod> metaCallEntry : codeFactory.pendingMetaCalls.entrySet()) {
			I methodCall = metaCallEntry.getKey();
			I calleeBeginIns = codeFactory.methods.get(metaCallEntry.getValue());
			if (methodCall != null && calleeBeginIns != null) {
				linkMetaCall(methodCall, calleeBeginIns, metaCallEntry.getValue().metaHandler);
			}
		}
		
		boolean separateMainMethod = hasSeparateMainMethod();
		
		NCompilableMethod main = separateMainMethod ? codeFactory.cg.getMainMethod() : null;
		
		if (separateMainMethod) {
			setMainMethod(main);
		}
		
		for (NCompilableMethod m : codeFactory.publics) {
			if (m != main) {
				addPublic(m);
			}
		}
		
		finishLinking();
		
		optimize(codeFactory.cg.getArgs());
		
		return exec;
	}
	
	protected abstract boolean hasSeparateMainMethod();
	
	public abstract void resetExec();
	public abstract void addGlobal(Variable.Global g);
	public abstract void addNative(String name);
	public abstract void addLibrary(String libraryName);
	public abstract void addPubVar(Variable.Global g);
	public abstract void addPublic(NCompilableMethod method);
	public abstract void setMainMethod(NCompilableMethod method);
	
	public abstract void addInstructions(List<I> instructions);
	
	public abstract void calcInstructionPointers();
	
	public abstract void linkJump(I source, I target);
	public abstract void linkCall(I source, I target);
	public abstract void linkJumpTable(I instruction, AbstractCodeFactory.TempJumptable jumptable);
	public abstract void linkNative(I caller, InboundDefinition target);
	
	public void linkMetaCall(I source, I target, MetaFunctionHandler<I> handler) {
		if (handler != null) {
			handler.linkCall(source, target);
		}
	}
	
	public abstract void finishLinking();
	
	public abstract void optimize(LangCompiler.CompilerArguments args);
}
