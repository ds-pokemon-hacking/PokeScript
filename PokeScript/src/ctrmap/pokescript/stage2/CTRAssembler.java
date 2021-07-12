package ctrmap.pokescript.stage2;

import ctrmap.scriptformats.gen6.GFLPawnScript;
import ctrmap.scriptformats.gen6.GFScrHash;
import ctrmap.scriptformats.gen6.PawnInstruction;
import ctrmap.scriptformats.gen6.PawnOptimizer;
import ctrmap.scriptformats.gen6.PawnPrefixEntry;
import ctrmap.pokescript.CompilerLogger;
import ctrmap.pokescript.LangCompiler;
import ctrmap.pokescript.data.Variable;
import ctrmap.pokescript.instructions.ctr.instructions.PAccessGlobal;
import ctrmap.pokescript.instructions.abstractcommands.ACompiledInstruction;
import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.instructions.abstractcommands.ALocalCall;
import ctrmap.pokescript.instructions.abstractcommands.APlainOpCode;
import ctrmap.pokescript.stage0.Modifier;
import ctrmap.pokescript.stage1.NCompilableMethod;
import ctrmap.pokescript.stage1.NCompileGraph;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class CTRAssembler {

	public static void processTimesUsed(NCompilableMethod method, NCompileGraph graph) {
		method.def.timesUsed++;
		for (AInstruction ins : method.body) {
			for (AInstruction sub : ins.getAllInstructions()) {
				if (sub instanceof ALocalCall) {
					NCompilableMethod m = graph.getMethodByDef(((ALocalCall) sub).call);
					if (m != null) {
						processTimesUsed(m, graph);
					}
				} else if (sub instanceof PAccessGlobal) {
					Variable glb = graph.findGVar(((PAccessGlobal) sub).gName);
					if (glb != null) {
						glb.timesUsed++;
					}
				}
			}
		}
	}

	public static void assemble(NCompileGraph graph, GFLPawnScript target) {
		List<AInstruction> allInstructions = new ArrayList<>();
		allInstructions.add(0, graph.getPlain(APlainOpCode.ABORT_EXECUTION));

		target.data.clear();
		target.instructions.clear();
		target.natives.clear();
		target.publics.clear();
		target.publicVars.clear();
		target.nameTable.clear();
		target.libraries.clear();
		target.tags.clear();
		target.overlays.clear();

		NCompilableMethod main = graph.getMainMethod();

		for (NCompilableMethod m : graph.methods) {
			if (m == main || m.hasModifier(Modifier.PUBLIC)) {
				processTimesUsed(m, graph);
			}
		}

		List<NCompilableMethod> unusedMethods = new ArrayList<>();
		List<Variable> unusedGlobals = new ArrayList<>();
		for (NCompilableMethod m : graph.methods) {
			if (m.def.timesUsed == 0 && !m.hasModifier(Modifier.PUBLIC) && m != main) {
				//System.out.println("unused method " + m.def);
				unusedMethods.add(m);
			}
		}
		for (Variable g : graph.globals) {
			if ((g.hasModifier(Modifier.FINAL) || g.timesUsed == 0) && ((Variable.Global)g).hasModifier(Modifier.PUBLIC)) {
				//final variables are already optimized into constants by the compiler, can be safely removed
				unusedGlobals.add(g);
			}
		}
		graph.methods.removeAll(unusedMethods);
		graph.globals.removeAll(unusedGlobals, graph);
		
		graph.prepareNativeTable();

		//first, initialize globals in main
		for (Variable v : graph.globals.variables) {
			Variable.Global g = (Variable.Global) v;

			if (g.hasModifier(Modifier.PUBLIC)) {
				target.publicVars.add(new PawnPrefixEntry(8, PawnPrefixEntry.Type.PUBLIC_VAR, new int[]{
					g.getPointer(graph),
					GFScrHash.getHash(g.name)
				}));
			}

			target.data.add(new PawnInstruction(g.getImmediateValue())); //0 if not immediate
			if (!g.isImmediate()) {
				if (main == null) {
					throw new UnsupportedOperationException("Cannot initialize globals in declaration in files that have no main() method.");
				}
				main.insertInstructions(g.init_from); //adds them to beginning
				main.insertInstruction(g.getWriteIns(graph));
			}
		}

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

		target.mainEntryPoint = (main != null ? main.getPointer() : -1);

		for (AInstruction i : allInstructions) {
			List<? extends ACompiledInstruction> compiled = i.compile(graph);
			for (ACompiledInstruction ci : compiled){
				if (ci instanceof PawnInstruction){
					target.instructions.add((PawnInstruction)ci);
				}
				else {
					throw new UnsupportedOperationException("The Pawn assembler was fed a non-Pawn instruction. Consult your instruction provider.");
				}
			}
		}

		//determine stack top
		//we can't unfortunately run the emulator and determine it since that would differ with various args etc
		int stpIdx = 0;
		for (PawnInstruction ins : target.instructions) {
			switch (PawnInstruction.cmdList.get(ins.getCommand())) {
				case PUSH:
				case PUSH_ADR:
				case PUSH_ALT:
				case PUSH_C:
				case PUSH_P:
				case PUSH_PRI:
				case PUSH_P_ADR:
				case PUSH_P_C:
				case PUSH_P_S:
				case PUSH_S:
					stpIdx++;
			}
			if (ins.getCommand() >= PawnInstruction.Commands.PUSH2_C.ordinal()
					&& ins.getCommand() <= PawnInstruction.Commands.PUSH5_ADR.ordinal()) {
				int amount = (ins.getCommand() - PawnInstruction.Commands.PUSH2_C.ordinal()) / 4;

				stpIdx += 2 + amount;
			}
		}
		target.stackSize = Math.max(4096, stpIdx * 4);
		
		for (String nativeName : graph.natives) {
			if (nativeName.contains(".")) {
				//remove namespace
				nativeName = nativeName.substring(nativeName.lastIndexOf(".") + 1);
			}
			PawnPrefixEntry natv = new PawnPrefixEntry(8, PawnPrefixEntry.Type.NATIVE, new int[]{
				0x00000000,
				GFScrHash.getHash(nativeName)
			});
			target.natives.add(natv);
		}

		for (NCompilableMethod m : graph.methods) {
			if (m.hasModifier(Modifier.PUBLIC) && !m.hasModifier(Modifier.NATIVE) && m != main) {
				target.publics.add(new PawnPrefixEntry(8, PawnPrefixEntry.Type.PUBLIC, new int[]{
					m.getPointer(),
					GFScrHash.getHash(m.def.name)
				}));
			}
		}

		LangCompiler.CompilerArguments args = graph.getArgs();
		
		args.logger.println(CompilerLogger.LogLevel.INFO, "Beginning optimization...");
		for (int i = 0; i < args.optimizationPassCount; i++) {
			args.logger.println(CompilerLogger.LogLevel.INFO, "Performing optimization pass " + (i + 1) + " of " + args.optimizationPassCount);
			PawnOptimizer.optimize(target);
		}

		target.updateRaw();
	}
}
