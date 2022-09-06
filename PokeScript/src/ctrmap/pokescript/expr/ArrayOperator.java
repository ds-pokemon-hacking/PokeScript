package ctrmap.pokescript.expr;

import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.stage0.EffectiveLine;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.pokescript.types.DataType;
import ctrmap.pokescript.types.TypeDef;
import java.util.List;

public class ArrayOperator extends Operator {

	public ArrayOperator() {
	}

	@Override
	public OperatorType getType() {
		return OperatorType.BINARY_INV;
	}
	
	@Override
	public String toString() {
		return "Array";
	}

	@Override
	public TypeDef getInputTypeLHS() {
		return DataType.ANY.typeDef();
	}

	@Override
	public TypeDef getInputTypeRHS() {
		return DataType.ANY.typeDef();
	}

	@Override
	public TypeDef getOutputType() {
		return DataType.ANY.typeDef();
	}

	@Override
	public OperatorOperation getOperationType() {
		return OperatorOperation.CALL;
	}

	@Override
	public Priority getPriority() {
		return Priority.DEREF;
	}

	@Override
	public List<AInstruction> getOperation(EffectiveLine line, NCompileGraph cg, Throughput... inputs) {
		return null;
	}
}
