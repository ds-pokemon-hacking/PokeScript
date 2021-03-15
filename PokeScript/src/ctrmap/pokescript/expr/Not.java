package ctrmap.pokescript.expr;

import ctrmap.pokescript.types.DataType;
import ctrmap.scriptformats.gen6.PawnInstruction;
import ctrmap.pokescript.instructions.ctr.instructions.CTRInstruction;
import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.instructions.abstractcommands.APlainOpCode;
import ctrmap.pokescript.stage0.EffectiveLine;
import ctrmap.pokescript.stage1.NCompileGraph;
import java.util.ArrayList;
import java.util.List;

public class Not extends Operator{
	
	@Override
	public DataType getInputTypeLHS() {
		return DataType.VOID;
	}
	
	@Override
	public DataType getInputTypeRHS() {
		return DataType.BOOLEAN;
	}

	@Override
	public DataType getOutputType() {
		return DataType.BOOLEAN;
	}

	@Override
	protected List<AInstruction> createOperation(NCompileGraph cg) {
		throw new UnsupportedOperationException("This method should never have been called on this class.");
	}
		
	@Override
	public List<AInstruction> getOperation(Throughput left, Throughput right, EffectiveLine line, NCompileGraph cg) {
		List<AInstruction> r = new ArrayList<>();
		//not only takes one input, the parser has adjusted that to always be right
		r.addAll(right.getCode(getInputTypeRHS()));
		r.add(cg.getPlain(APlainOpCode.NOT));
		return r;
	}

	@Override
	public Priority getPriority() {
		return Priority.NORMAL;
	}
}
