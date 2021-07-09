package ctrmap.pokescript.stage2;

import ctrmap.pokescript.LangCompiler;
import ctrmap.pokescript.data.Variable;
import ctrmap.pokescript.instructions.abstractcommands.ACompiledInstruction;
import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.instructions.abstractcommands.APlainInstruction;
import ctrmap.pokescript.instructions.abstractcommands.APlainOpCode;
import ctrmap.pokescript.instructions.gen5.VOpCode;
import ctrmap.pokescript.instructions.gen5.metahandlers.VMovementFuncHandler;
import ctrmap.pokescript.instructions.ntr.NTRInstructionCall;
import ctrmap.pokescript.instructions.ntr.instructions.PlainNTRInstruction;
import ctrmap.pokescript.stage0.Modifier;
import ctrmap.pokescript.stage1.NCompilableMethod;
import ctrmap.pokescript.stage1.NCompileGraph;
import static ctrmap.pokescript.stage2.CTRAssembler.processTimesUsed;
import ctrmap.scriptformats.gen5.VScriptFile;
import ctrmap.scriptformats.gen5.optimizer.VAsmOptimizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class VAssembler {

	public static VScriptFile assemble(NCompileGraph graph) {
		VScriptFile scr = new VScriptFile();
		assemble(graph, scr);
		return scr;
	}

	public static void assemble(NCompileGraph graph, VScriptFile scr) {
		scr.instructions.clear();
		scr.movements.clear();
		scr.publics.clear();

		for (NCompilableMethod m : graph.methods) {
			if (m.hasModifier(Modifier.PUBLIC)) {
				processTimesUsed(m, graph);
			}
		}

		List<NCompilableMethod> unusedMethods = new ArrayList<>();
		List<Variable> unusedGlobals = new ArrayList<>();
		for (NCompilableMethod m : graph.methods) {
			if (m.def.timesUsed == 0 && !m.hasModifier(Modifier.PUBLIC)) {
				unusedMethods.add(m);
			}
		}
		for (Variable g : graph.globals) {
			if ((g.hasModifier(Modifier.FINAL) || g.timesUsed == 0) && ((Variable.Global) g).hasModifier(Modifier.PUBLIC)) {
				//final variables are already optimized into constants by the compiler, can be safely removed
				unusedGlobals.add(g);
			}
		}
		graph.methods.removeAll(unusedMethods);
		graph.globals.removeAll(unusedGlobals, graph);

		List<AInstruction> allInstructions = new ArrayList<>();
		for (NCompilableMethod m : graph.methods) {
			if (!m.hasModifier(Modifier.NATIVE)) {
				allInstructions.addAll(m.body);
			}
		}

		int ptr = 0;
		for (AInstruction i : allInstructions) {
			i.pointer = ptr;
			ptr += i.getAllocatedPointerSpace(graph);
		}

		Map<AInstruction, NCompilableMethod> publicsInstructionHooks = new HashMap<>();
		for (NCompilableMethod m : graph.methods) {
			if ((m.hasModifier(Modifier.PUBLIC) && m.hasModifier(Modifier.STATIC) && !m.hasModifier(Modifier.NATIVE)) || m.hasModifier(Modifier.META)) {
				publicsInstructionHooks.put(m.body.get(0), m);
			}
		}

		boolean putPublicNext = false;
		boolean putAsMovement = false;
		for (AInstruction i : allInstructions) {
			List<? extends ACompiledInstruction> compiled = i.compile(graph);

			if (publicsInstructionHooks.containsKey(i)) {
				NCompilableMethod m = publicsInstructionHooks.get(i);
				putAsMovement = false;
				if (m.hasModifier(Modifier.META)) {
					if (m.metaHandler instanceof VMovementFuncHandler) {
						putAsMovement = true;
					}
				} else {
					putPublicNext = true;
				}
			}
			for (ACompiledInstruction ci : compiled) {
				if (ci instanceof NTRInstructionCall) {
					if (putAsMovement) {
						scr.movements.add((NTRInstructionCall) ci);
					} else {
						scr.instructions.add((NTRInstructionCall) ci);
					}

					if (putPublicNext) {
						scr.addPublic((NTRInstructionCall) ci);
						putPublicNext = false;
					}
				} else {
					throw new UnsupportedOperationException("The Gen V assembler was fed a non-NTR instruction. Consult your instruction provider.");
				}
			}
		}

		scr.setUpLinks();

		VAsmOptimizer.optimize(scr, graph.getArgs().optimizationLevel);
	}
}
