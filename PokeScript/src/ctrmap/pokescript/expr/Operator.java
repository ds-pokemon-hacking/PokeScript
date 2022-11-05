package ctrmap.pokescript.expr;

import ctrmap.pokescript.types.DataType;
import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.stage0.EffectiveLine;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.pokescript.types.TypeDef;
import java.util.List;

public abstract class Operator {

	public OperatorType getType() {
		if (getInputTypeLHS().baseType == DataType.VOID) {
			return OperatorType.UNARY;
		}
		return OperatorType.BINARY;
	}
	
	public abstract OperatorOperation getOperationType();
	
	public abstract TypeDef getInputTypeLHS();

	public abstract TypeDef getInputTypeRHS();

	public abstract TypeDef getOutputType();

	public abstract Priority getPriority();
	
	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	public boolean checkCast(Throughput left, Throughput right, EffectiveLine line){
		boolean b = true;
		if (left != null){
			b &= left.checkImplicitCast(getInputTypeLHS(), line);
		}
		if (right != null){
			b &= right.checkImplicitCast(getInputTypeRHS(), line);
		}
		return b;
	}
	
	public void attachOperands(NCompileGraph cg, Throughput... operands) {
		
	}
	
	public abstract List<AInstruction> getOperation(EffectiveLine line, NCompileGraph cg, Throughput... inputs);

	public static enum Priority {
		GROUP,
		ASSIGNMENT,
		BOOLOPS,
		COMPARE,
		BITSHIFT,
		NORMAL,
		ALG_MULT,
		NEGATE,
		//NEW,
		CALL,
		DEREF
		//TYPE_CAST
	}
}
