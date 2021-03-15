package ctrmap.pokescript.expr;

import ctrmap.pokescript.instructions.ctr.instructions.CTRInstruction;
import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.stage0.EffectiveLine;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.pokescript.types.AbstractTypeHandler;
import ctrmap.pokescript.types.DataType;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class TypeCast extends Operator {

	public DataType targetType;

	public TypeCast(DataType target) {
		targetType = target;
	}

	@Override
	public DataType getInputTypeLHS() {
		return DataType.ANY;
	}

	@Override
	public DataType getInputTypeRHS() {
		return DataType.ANY;
	}

	@Override
	public DataType getOutputType() {
		return targetType;
	}
	
	@Override
	public List<AInstruction> getOperation(Throughput left, Throughput right, EffectiveLine line, NCompileGraph cg) {
		//only accepts the right throughput
		Throughput toCast = right;
		AbstractTypeHandler.CastResult rsl = toCast.type.getBaseType().requestHandler().getInstructionForCast(targetType, cg);
		List<AInstruction> l = new ArrayList<>();
		if (rsl.success) {
			l.addAll(right.getCode(right.type));
			if (toCast.type != targetType) {
				l.addAll(rsl.instructions);
			}
		}
		else {
			if (line != null){
				line.throwException(rsl.exception);
			}
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
	protected List<AInstruction> createOperation(NCompileGraph cg) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public Priority getPriority() {
		return Priority.NORMAL;
	}

}
