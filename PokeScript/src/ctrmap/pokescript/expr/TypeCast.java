package ctrmap.pokescript.expr;

import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.stage0.EffectiveLine;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.pokescript.types.AbstractTypeHandler;
import ctrmap.pokescript.types.DataType;
import ctrmap.pokescript.types.TypeDef;
import java.util.ArrayList;
import java.util.List;

public class TypeCast extends Operator {

	public TypeDef targetType;

	public TypeCast(TypeDef target) {
		targetType = target;
	}

	@Override
	public String toString() {
		return "Cast:" + targetType;
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
		return targetType;
	}

	@Override
	public OperatorOperation getOperationType() {
		return OperatorOperation.CAST;
	}

	@Override
	public List<AInstruction> getOperation(EffectiveLine line, NCompileGraph cg, Throughput... inputs) {
		//only accepts the right throughput
		Throughput toCast = inputs[0];
		List<AInstruction> l = new ArrayList<>();
		if (toCast != null) {
			AbstractTypeHandler.CastResult rsl = toCast.type.baseType.getBaseType().requestHandler().getInstructionForCast(targetType.baseType, cg);
			if (rsl.success) {
				l.addAll(toCast.getCode(toCast.type));
				if (toCast.type != targetType) {
					l.addAll(rsl.instructions);
				}
			} else {
				if (line != null) {
					line.throwException(rsl.exception);
				}
			}
		} else {
			line.throwException("Null cast operand.");
		}
		return l;
	}

	public static boolean checkCast(DataType source, DataType target) {
		switch (target.getBaseType()) {
			case INT:
			case FLOAT:
				return source.isNumber();
			case BOOLEAN:
				return source.getBaseType() == DataType.BOOLEAN;
		}
		return false;
	}

	@Override
	public Priority getPriority() {
		return Priority.NEGATE;
	}

}
