package ctrmap.pokescript.instructions.ctr.instructions;

import ctrmap.pokescript.instructions.abstractcommands.ACaseTable;
import ctrmap.pokescript.instructions.ctr.PawnPlainInstruction;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.scriptformats.gen6.PawnInstruction;
import ctrmap.scriptformats.gen6.PawnOpCode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PCaseTable extends ACaseTable {

	@Override
	public int getAllocatedPointerSpace(NCompileGraph g) {
		return 12 + targets.size() * 8; //opcode + casecount + defaultcase + sizeof(case + label) * casecount + jump
	}

	@Override
	public List<PawnInstruction> compile(NCompileGraph g) {
		int count = targets.size();
		int len = count * 2 + 2;
		int[] args = new int[len];
		args[0] = count;
		args[1] = g.getInstructionByLabel(defaultCase).pointer;

		List<Map.Entry<Integer, String>> l = new ArrayList<>();
		l.addAll(targets.entrySet());
		Collections.sort(l, (Map.Entry<Integer, String> o1, Map.Entry<Integer, String> o2) -> o1.getKey() - o2.getKey());

		for (int i = 2, j = 0; j < count; i += 2, j++) {
			Map.Entry<Integer, String> e = l.get(j);
			args[i] = e.getKey();
			args[i + 1] = g.getInstructionByLabel(e.getValue()).pointer;
		}

		for (int i = 1; i < len; i += 2) {
			args[i] = relocatePointer(args[i], i);
		}

		List<PawnInstruction> r = new ArrayList<>();
		r.add(new PawnInstruction(PawnOpCode.CASETBL, PawnPlainInstruction.int2LongArray(args)));
		return r;
	}

	private int relocatePointer(int ptr, int argIdx) {
		return ptr - (pointer + 4 * argIdx);
	}
}
