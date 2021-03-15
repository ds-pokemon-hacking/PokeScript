package ctrmap.pokescript.expr;

import ctrmap.pokescript.types.DataType;
import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.instructions.abstractcommands.APlainOpCode;
import ctrmap.pokescript.stage0.EffectiveLine;
import ctrmap.pokescript.stage1.NCompileGraph;
import java.util.ArrayList;
import java.util.List;

public class Neg extends Operator {

	@Override
	public DataType getInputTypeLHS() {
		return DataType.VOID;
	}
	
	@Override
	public DataType getInputTypeRHS() {
		return DataType.INT;
	}

	@Override
	public DataType getOutputType() {
		return DataType.INT;
	}

	@Override
	protected List<AInstruction> createOperation(NCompileGraph cg) {
		throw new UnsupportedOperationException("This method should never have been called on this class.");
	}

	@Override
	public List<AInstruction> getOperation(Throughput left, Throughput right, EffectiveLine line, NCompileGraph cg) {
		List<AInstruction> r = new ArrayList<>();
		//neg only takes one input, the parser has adjusted that to always be right
		r.addAll(right.getCode(getInputTypeRHS()));

		if (right.type == DataType.FLOAT) {
			//float now in pri
			r.add(cg.getPlain(APlainOpCode.PUSH_PRI));
			r.add(cg.getPlain(APlainOpCode.CONST_ALT, 1));
			r.add(cg.getPlain(APlainOpCode.AND));
			r.add(cg.getPlain(APlainOpCode.NOT)); //flip the signum of the float
			r.add(cg.getPlain(APlainOpCode.MOVE_PRI_TO_ALT));
			r.add(cg.getPlain(APlainOpCode.POP_PRI));
			r.add(cg.getPlain(APlainOpCode.PUSH_ALT));
			r.add(cg.getPlain(APlainOpCode.CONST_ALT, 0xFFFFFFFE));
			r.add(cg.getPlain(APlainOpCode.AND));
			r.add(cg.getPlain(APlainOpCode.POP_ALT));
			r.add(cg.getPlain(APlainOpCode.OR));
		} else {
			r.add(cg.getPlain(APlainOpCode.NEGATE));
		}
		return r;
	}

	@Override
	public Priority getPriority() {
		return Priority.NEGATE;
	}
}
