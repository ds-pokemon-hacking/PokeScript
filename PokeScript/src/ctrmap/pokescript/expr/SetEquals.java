package ctrmap.pokescript.expr;

import ctrmap.pokescript.types.DataType;
import ctrmap.pokescript.instructions.ctr.instructions.CTRInstruction;
import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.stage0.EffectiveLine;
import ctrmap.pokescript.stage0.Modifier;
import ctrmap.pokescript.stage1.NCompileGraph;
import java.util.ArrayList;
import java.util.List;

public class SetEquals extends Operator {

	private VariableThroughput input;

	@Override
	public DataType getInputTypeLHS() {
		return DataType.ANY_VAR;
	}

	@Override
	public DataType getInputTypeRHS() {
		return DataType.ANY;
	}

	@Override
	public DataType getOutputType() {
		return input.type;
	}

	@Override
	public List<AInstruction> getOperation(Throughput left, Throughput right, EffectiveLine line, NCompileGraph cg) {
		if (!(left instanceof VariableThroughput)) {
			if (line != null) {
				line.throwException("Cannot assign a constant value.");
				return new ArrayList<>();
			} else {
				throw new UnsupportedOperationException("Cannot change a constant value.");
			}
		}
		input = (VariableThroughput) left;

		List<AInstruction> l = new ArrayList<>();

		//the variable throughput (always left) has to be set to the result of the right (any) throughput
		l.addAll(right.getCode(getInputTypeRHS()));
		//all code from right now on the left
		// -> its result in PRI
		if (input.var.hasModifier(Modifier.FINAL)) {
			line.throwException("Can not assign a final variable.");
		} else {
			l.add(input.getWriteIns(cg)); //write the PRI value to left

		}

		return l;
	}

	@Override
	protected List<AInstruction> createOperation(NCompileGraph cg) {
		throw new UnsupportedOperationException("This method should never be called on this class.");
	}

	@Override
	public Priority getPriority() {
		return Priority.NORMAL;
	}
}
