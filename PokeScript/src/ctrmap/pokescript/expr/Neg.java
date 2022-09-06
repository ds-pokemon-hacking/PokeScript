package ctrmap.pokescript.expr;

import ctrmap.pokescript.instructions.abstractcommands.AFloatOpCode;
import ctrmap.pokescript.types.DataType;
import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.instructions.abstractcommands.APlainOpCode;
import ctrmap.pokescript.stage0.EffectiveLine;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.pokescript.types.TypeDef;
import java.util.ArrayList;
import java.util.List;

public class Neg extends NumerableOperator {
	
	private TypeDef outputType;

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
		return outputType;
	}

	@Override
	public OperatorOperation getOperationType() {
		return OperatorOperation.NEG;
	}
	
	@Override
	public void attachOperands(NCompileGraph cg, Throughput... inputs) {
		DataType lowestType = DataType.INT;
		for (Throughput t : inputs) {
			if (t.type.baseType == DataType.FLOAT) {
				lowestType = DataType.FLOAT;
				break;
			}
		}
		outputType = lowestType.typeDef();
	}
	
	@Override
	public List<AInstruction> getOperation(EffectiveLine line, NCompileGraph cg, Throughput... inputs) {
		List<AInstruction> l = new ArrayList<>();
		//neg only takes one input, the parser has adjusted that to always be right
		l.addAll(inputs[0].getCode(getInputTypeRHS()));
		addFloatCvtIfNeeded(l, cg);
		
		if (inputs[0].type.baseType == DataType.FLOAT) {
			l.add(cg.getPlainFloat(AFloatOpCode.VNEGATE));
		}
		else {
			l.add(cg.getPlain(APlainOpCode.NEGATE));
		}

		/*if (inputs[0].type.baseType == DataType.FLOAT && !cg.provider.getFloatingPointHandler().isFixedPoint()) {
			//IEEE-754 floats have a simple sign bit
			r.add(cg.getPlain(APlainOpCode.CONST_ALT, 0x80000000));
			r.add(cg.getPlain(APlainOpCode.XOR));
		} else {
			r.add(cg.getPlain(APlainOpCode.NEGATE));
		}*/
		return l;
	}

	@Override
	public Priority getPriority() {
		return Priority.NEGATE;
	}
}
