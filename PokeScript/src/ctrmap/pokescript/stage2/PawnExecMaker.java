/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ctrmap.pokescript.stage2;

import ctrmap.pokescript.InboundDefinition;
import ctrmap.pokescript.LangCompiler;
import ctrmap.pokescript.data.Variable;
import ctrmap.pokescript.stage1.NCompilableMethod;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.scriptformats.gen6.GFLPawnScript;
import ctrmap.scriptformats.gen6.PawnInstruction;
import ctrmap.scriptformats.gen6.PawnOpCode;
import ctrmap.scriptformats.gen6.PawnOptimizer;
import ctrmap.scriptformats.gen6.PawnPrefixEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PawnExecMaker extends AbstractExecMaker<GFLPawnScript, PawnInstruction, PawnCodeFactory> {

	private final PawnExecType execType;

	public PawnExecMaker(PawnExecType execType) {
		this.execType = execType;
	}
	
	public static void compilePawnScript(NCompileGraph cg, GFLPawnScript target) {
		PawnExecMaker maker = new PawnExecMaker(PawnExecType.byCellSize(target.getCellSize()));
		maker.bindCG(cg).compileCode().compileCode().openExecutable(target).linkCode();
	}

	@Override
	protected boolean hasSeparateMainMethod() {
		return true;
	}

	@Override
	public void resetExec() {
		exec.data.clear();
		exec.instructions.clear();
		exec.natives.clear();
		exec.publics.clear();
		exec.publicVars.clear();
		exec.nameTable.clear();
		exec.libraries.clear();
		exec.tags.clear();
		exec.overlays.clear();
		exec.mainEntryPointDummy = null;
	}

	@Override
	public void addGlobal(Variable.Global g) {
		exec.data.add(new PawnInstruction(g.getImmediateValue()));
	}

	@Override
	public void addNative(String name) {
		exec.natives.add(
			new PawnPrefixEntry(
				exec.defsize,
				PawnPrefixEntry.Type.NATIVE,
				0x00000000,
				exec.hashName(name)
			)
		);
	}

	@Override
	public void addLibrary(String libraryName) {
		exec.libraries.add(
			new PawnPrefixEntry(
				exec.defsize,
				PawnPrefixEntry.Type.LIBRARY,
				0x00000000,
				exec.hashName(libraryName)
			)
		);
	}

	@Override
	public void addPubVar(Variable.Global g) {
		exec.publicVars.add(
			new PawnPrefixEntry(
				exec.defsize,
				PawnPrefixEntry.Type.PUBLIC_VAR,
				codeFactory.getVariablePointer(g),
				exec.hashName(g.getNameWithoutNamespace())
			)
		);
	}

	@Override
	public void addPublic(NCompilableMethod method) {
		PawnInstruction methodIns = codeFactory.methods.get(method);

		if (methodIns != null) {
			exec.publics.add(
				new PawnPrefixEntry(
					exec.defsize,
					PawnPrefixEntry.Type.PUBLIC,
					methodIns.pointer,
					exec.hashName(method.def.getNameWithoutNamespace())
				)
			);
		}
	}

	@Override
	public void setMainMethod(NCompilableMethod method) {
		PawnInstruction methodIns;
		if (method != null && (methodIns = codeFactory.methods.get(method)) != null) {
			exec.mainEntryPoint = methodIns.pointer;
		} else {
			exec.mainEntryPoint = -1;
		}
	}

	@Override
	public void addInstructions(List<PawnInstruction> instructions) {
		exec.instructions.addAll(instructions);
	}

	@Override
	public void calcInstructionPointers() {
		exec.setPtrsByIndex();
	}

	@Override
	public void linkJump(PawnInstruction source, PawnInstruction target) {
		source.arguments[0] = target.pointer - source.pointer; //pawn jumps always have only one argument
	}

	@Override
	public void linkCall(PawnInstruction source, PawnInstruction target) {
		source.arguments[0] = target.pointer - source.pointer;
	}

	@Override
	public void linkJumpTable(PawnInstruction casetbl, AbstractCodeFactory.TempJumptable jumptable) {
		casetbl.arguments[0 + 0] = jumptable.cases.size(); //count
		casetbl.arguments[0 + 1] = getJumptableEntryRelativePointer(casetbl, jumptable.defaultCase, 0, codeFactory.cellSize);

		List<Map.Entry<Integer, String>> sortedCases = new ArrayList<>(jumptable.cases.entrySet());
		sortedCases.sort(Map.Entry.comparingByKey());

		for (int inCaseIdx = 0, recordIdx = 1, cellIdx = 2; inCaseIdx < sortedCases.size(); inCaseIdx++, recordIdx++, cellIdx += 2) {
			Map.Entry<Integer, String> entry = sortedCases.get(inCaseIdx);
			casetbl.arguments[cellIdx + 0] = entry.getKey();
			casetbl.arguments[cellIdx + 1] = getJumptableEntryRelativePointer(casetbl, entry.getValue(), recordIdx, codeFactory.cellSize);
		}
	}

	private int getJumptableEntryRelativePointer(PawnInstruction casetbl, String targetLabel, int recordIdx, int cellSize) {
		PawnInstruction targetIns = codeFactory.labels.get(targetLabel);
		if (targetIns == null) {
			return 0;
		}
		return targetIns.pointer - (casetbl.pointer + cellSize * (recordIdx * 2 + 1)); //recordIdx * size of record + opcode size
	}

	@Override
	public void linkNative(PawnInstruction caller, InboundDefinition target) {
		int index = exec.getPrefixEntryIdxByName(exec.natives, target.getNameWithoutNamespace());
		caller.arguments[0] = index;
	}

	@Override
	public void finishLinking() {
		exec.setInstructionListeners();

		//determine stack top
		//we can't unfortunately run the emulator and determine it since that would differ with various args etc
		int stpIdx = 0;
		for (PawnInstruction ins : exec.instructions) {
			switch (ins.opCode) {
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
					break;
				case SYSREQ_N:
					stpIdx++;
					break;
				case CALL:
					stpIdx += 3;
					break;
			}
			if (ins.opCode.ordinal() >= PawnOpCode.PUSH2_C.ordinal()
				&& ins.opCode.ordinal() <= PawnOpCode.PUSH5_ADR.ordinal()) {
				int amount = (ins.opCode.ordinal() - PawnOpCode.PUSH2_C.ordinal()) / 4;

				stpIdx += 2 + amount;
			}
		}
		exec.stackSize = Math.max(4096, stpIdx * 4);
	}

	@Override
	public GFLPawnScript newExecutable() {
		return GFLPawnScript.createExecutableGFLScript(execType.cellSize);
	}

	@Override
	public PawnCodeFactory createCodeFactory(NCompileGraph cg) {
		return new PawnCodeFactory(cg, execType.cellSize);
	}

	@Override
	public void optimize(LangCompiler.CompilerArguments args) {
		for (int i = 0; i < args.optimizationPassCount; i++) {
			PawnOptimizer.optimize(exec);
		}
	}

	public static enum PawnExecType {
		PAWN16(Short.BYTES),
		PAWN32(Integer.BYTES),
		PAWN64(Long.BYTES);
		
		public static final PawnExecType[] VALUES = values();

		public final int cellSize;

		private PawnExecType(int cellSize) {
			this.cellSize = cellSize;
		}
		
		public static PawnExecType byCellSize(int cellSize) {
			for (PawnExecType t : VALUES) {
				if (t.cellSize == cellSize) {
					return t;
				}
			}
			return null;
		}
	}
}
