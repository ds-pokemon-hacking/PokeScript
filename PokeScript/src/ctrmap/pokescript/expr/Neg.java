package ctrmap.pokescript.expr;

import ctrmap.pokescript.types.DataType;
import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.instructions.abstractcommands.APlainOpCode;
import ctrmap.pokescript.stage0.EffectiveLine;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.pokescript.types.TypeDef;
import java.util.ArrayList;
import java.util.List;

public class Neg extends Operator {

	@Override
	public TypeDef getInputTypeLHS() {
		return DataType.VOID.typeDef();
	}
	
	@Override
	public TypeDef getInputTypeRHS() {
		return DataType.FLOAT.typeDef();
	}

	@Override
	public TypeDef getOutputType() {
		return DataType.INT.typeDef();
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

		if (right.type.baseType == DataType.FLOAT && !cg.provider.getFloatingPointHandler().isFixedPoint()) {
			//IEEE-754 floats have a simple sign bit
			r.add(cg.getPlain(APlainOpCode.CONST_ALT, 0x80000000));
			r.add(cg.getPlain(APlainOpCode.XOR));
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
