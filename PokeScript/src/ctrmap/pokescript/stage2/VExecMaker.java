package ctrmap.pokescript.stage2;

import ctrmap.pokescript.InboundDefinition;
import ctrmap.pokescript.LangCompiler;
import ctrmap.pokescript.data.Variable;
import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.instructions.gen5.metahandlers.VActionSeqHandler;
import ctrmap.pokescript.instructions.ntr.NTRInstructionCall;
import ctrmap.pokescript.stage0.Modifier;
import ctrmap.pokescript.stage1.NCompilableMethod;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.scriptformats.gen5.VScriptFile;
import ctrmap.scriptformats.gen5.optimizer.VAsmOptimizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VExecMaker extends AbstractExecMaker<VScriptFile, NTRInstructionCall, VCodeFactory> {

	public static void compileVScript(NCompileGraph cg, VScriptFile target) {
		/*for (NCompilableMethod m : cg.methods) {
			if (!m.isBlankBody()) {
				System.out.println("METHOD " + m.def.name);
				for (AInstruction ins : m.body) {
					for (AInstruction i : ins.getAllInstructions()) {
						for (String lbl : i.labels()) {
							System.out.println(lbl + ":");
						}
						System.out.println(i);
					}
				}
			}
		}*/
		VExecMaker maker = new VExecMaker();
		maker.bindCG(cg).compileCode().openExecutable(target).linkCode();
	}

	@Override
	public VScriptFile newExecutable() {
		return new VScriptFile();
	}

	@Override
	public VCodeFactory createCodeFactory(NCompileGraph cg) {
		return new VCodeFactory(cg);
	}

	@Override
	protected boolean hasSeparateMainMethod() {
		return false;
	}

	@Override
	public void resetExec() {
		exec.instructions.clear();
		exec.actions.clear();
		exec.publics.clear();
	}

	@Override
	public void addGlobal(Variable.Global g) {
		//Nothing. Constant globals are constant folded and non-constant ones don't need allocation.
	}

	@Override
	public void addNative(String name) {
		//Nothing here either. Natives have hardcoded opcodes.
	}

	@Override
	public void addLibrary(String libraryName) {
		//This platform does not support library imports.
	}

	@Override
	public void addPubVar(Variable.Global g) {
		//This platform does not support field exports.
	}

	@Override
	public void addPublic(NCompilableMethod method) {
		NTRInstructionCall ins = codeFactory.methods.get(method);
		if (ins != null) {
			exec.addPublic(ins);
		} else {
			throw new RuntimeException("Could not resolve public method start " + method.def.name + ".");
		}
	}

	@Override
	public void setMainMethod(NCompilableMethod method) {
		//No main methods here
	}

	@Override
	public void addInstructions(List<NTRInstructionCall> instructions) {
		Map<NTRInstructionCall, NCompilableMethod> initialInstructionHooks = new HashMap<>();
		Map<NTRInstructionCall, NCompilableMethod> actionInstructionHooks = new HashMap<>();

		for (Map.Entry<NCompilableMethod, NTRInstructionCall> e : codeFactory.methods.entrySet()) {
			NCompilableMethod m = e.getKey();
			initialInstructionHooks.put(e.getValue(), m);
			if (m.hasModifier(Modifier.META)) {
				if (m.metaHandler instanceof VActionSeqHandler) {
					actionInstructionHooks.put(e.getValue(), m);
				}
			}
		}

		boolean putAsActionSeq = false;

		for (NTRInstructionCall i : instructions) {
			if (initialInstructionHooks.containsKey(i)) {
				putAsActionSeq = false;
				if (actionInstructionHooks.containsKey(i)) {
					putAsActionSeq = true;
				}
			}

			(putAsActionSeq ? exec.actions : exec.instructions).add(i);
		}
	}

	@Override
	public void calcInstructionPointers() {
		exec.updatePtrs();
	}

	@Override
	public void linkJump(NTRInstructionCall source, NTRInstructionCall target) {
		source.args[source.args.length - 1] = target.pointer - (source.pointer + source.getSize());
	}

	@Override
	public void linkCall(NTRInstructionCall source, NTRInstructionCall target) {
		source.args[source.args.length - 1] = target.pointer - (source.pointer + source.getSize());
	}

	@Override
	public void linkJumpTable(NTRInstructionCall instruction, AbstractCodeFactory.TempJumptable jumptable) {
		//Jumptables aren't used, replaced with plain jumps instead
	}

	@Override
	public void linkNative(NTRInstructionCall caller, InboundDefinition target) {
		//Natives needn't be linked
	}

	@Override
	public void finishLinking() {
		exec.setUpLinks(false);
	}

	@Override
	public void optimize(LangCompiler.CompilerArguments args) {
		for (int i = 0; i < args.optimizationPassCount; i++) {
			VAsmOptimizer.optimize(exec, args.optimizationLevel);
		}
		for (NTRInstructionCall c : exec.instructions) {
			System.out.println(c);
		}
	}
}
