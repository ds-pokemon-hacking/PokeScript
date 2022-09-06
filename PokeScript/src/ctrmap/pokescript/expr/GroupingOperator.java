package ctrmap.pokescript.expr;

import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.stage0.EffectiveLine;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.pokescript.types.DataType;
import ctrmap.pokescript.types.TypeDef;
import java.util.List;

public class GroupingOperator extends Operator {

	@Override
	public TypeDef getInputTypeLHS() {
		return DataType.ANY.typeDef();
	}

	@Override
	public TypeDef getInputTypeRHS() {
		return DataType.ANY.typeDef();
	}

	@Override
	public OperatorType getType() {
		return OperatorType.SEQUENTIAL;
	}

	@Override
	public TypeDef getOutputType() {
		return DataType.ANY.typeDef();
	}

	@Override
	public Priority getPriority() {
		return Priority.GROUP;
	}

	@Override
	public OperatorOperation getOperationType() {
		return OperatorOperation.GROUP;
	}

	@Override
	public List<AInstruction> getOperation(EffectiveLine line, NCompileGraph cg, Throughput... inputs) {
		throw new UnsupportedOperationException("Not supported yet.");
	}
}
