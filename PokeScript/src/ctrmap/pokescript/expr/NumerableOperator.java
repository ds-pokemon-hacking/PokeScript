package ctrmap.pokescript.expr;

import ctrmap.pokescript.instructions.abstractcommands.AFloatOpCode;
import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.pokescript.types.DataType;
import ctrmap.pokescript.types.TypeDef;
import java.util.List;

public abstract class NumerableOperator extends Operator {

	protected TypeDef highestNumberType;

	public boolean useFloatingOps() {
		return highestNumberType.baseType == DataType.FLOAT;
	}
	
	protected void addOperandCode(List<AInstruction> target, List<AInstruction> operand, NCompileGraph cg) {
		if (operand != null) {
			target.addAll(operand);
			addFloatCvtIfNeeded(target, cg);
		}
	}
	
	protected void addFloatCvtIfNeeded(List<AInstruction> target, NCompileGraph cg) {
		if (useFloatingOps()) {
			target.add(cg.getPlainFloat(AFloatOpCode.VCVT_TOFLOAT));
		}
	}
	
	@Override
	public void attachOperands(NCompileGraph cg, Throughput... inputs) {
		DataType highestType = DataType.INT;
		for (Throughput t : inputs) {
			if (t != null && t.type != null && t.type.baseType == DataType.FLOAT) {
				highestType = DataType.FLOAT;
				break;
			}
		}
		highestNumberType = highestType.typeDef();
	}
}
