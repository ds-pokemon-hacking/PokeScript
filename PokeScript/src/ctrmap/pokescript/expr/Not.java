package ctrmap.pokescript.expr;

import ctrmap.pokescript.types.DataType;
import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.instructions.abstractcommands.APlainOpCode;
import ctrmap.pokescript.stage0.EffectiveLine;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.pokescript.types.TypeDef;
import java.util.ArrayList;
import java.util.List;

public class Not extends Operator {

	@Override
	public TypeDef getInputTypeLHS() {
		return DataType.VOID.typeDef();
	}

	@Override
	public TypeDef getInputTypeRHS() {
		return DataType.BOOLEAN.typeDef();
	}

	@Override
	public TypeDef getOutputType() {
		return DataType.BOOLEAN.typeDef();
	}

	@Override
	public OperatorOperation getOperationType() {
		return OperatorOperation.NOT;
	}

	@Override
	public List<AInstruction> getOperation(EffectiveLine line, NCompileGraph cg, Throughput... inputs) {
		List<AInstruction> r = new ArrayList<>();
		//not only takes one input, the parser has adjusted that to always be right
		if (inputs.length > 0 && inputs[0] != null) {
			r.addAll(inputs[0].getCode(getInputTypeRHS()));
		}
		r.add(cg.getPlain(APlainOpCode.NOT));
		return r;
	}

	@Override
	public Priority getPriority() {
		return Priority.NEGATE;
	}
}
