package ctrmap.pokescript.expr;

import ctrmap.pokescript.types.DataType;
import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.stage0.EffectiveLine;
import ctrmap.pokescript.stage0.Modifier;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.pokescript.types.TypeDef;
import java.util.ArrayList;
import java.util.List;

public class SetEquals extends Operator {

	private VariableThroughput input;

	@Override
	public TypeDef getInputTypeLHS() {
		return DataType.ANY_VAR.typeDef();
	}

	@Override
	public TypeDef getInputTypeRHS() {
		return DataType.ANY.typeDef();
	}

	@Override
	public TypeDef getOutputType() {
		if (input == null) {
			return DataType.VOID.typeDef();
		}
		return input.type;
	}

	@Override
	public OperatorOperation getOperationType() {
		return OperatorOperation.ASSIGN;
	}

	@Override
	public List<AInstruction> getOperation(EffectiveLine line, NCompileGraph cg, Throughput... inputs) {
		if (!(inputs[0] instanceof VariableThroughput)) {
			if (line != null) {
				line.throwException("Cannot assign a constant value.");
				return new ArrayList<>();
			} else {
				throw new UnsupportedOperationException("Cannot change a constant value.");
			}
		}
		input = (VariableThroughput) inputs[0];

		List<AInstruction> l = new ArrayList<>();
		
		Throughput right = inputs[1];

		if (right != null) {
			//the variable throughput (always left) has to be set to the result of the right (any) throughput
			l.addAll(right.getCode(getInputTypeRHS()));
			//all code from right now on the left
			// -> its result in PRI
			if (input.var.hasModifier(Modifier.FINAL)) {
				line.throwException("Can not assign a final variable.");
			} else {
				/*for (AInstruction ins : right.getCode(getInputTypeRHS())) {
					if (ins instanceof APlainInstruction) {
						System.out.println(((APlainInstruction)ins).opCode);
					}
					else {
						System.out.println(ins);
					}
				}*/
				//System.out.println("assigned variable " + input.var.name);
				l.add(input.getWriteIns(cg)); //write the PRI value to left
			}
		}

		return l;
	}

	@Override
	public Priority getPriority() {
		return Priority.ASSIGNMENT;
	}
}
