/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ctrmap.pokescript.expr;

import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.stage0.EffectiveLine;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.pokescript.types.DataType;
import ctrmap.pokescript.types.TypeDef;
import java.util.ArrayList;
import java.util.List;

public class InitHeapObj extends Operator {

	public TypeDef typeDef;

	@Override
	public TypeDef getInputTypeLHS() {
		return DataType.VOID.typeDef();
	}

	@Override
	public TypeDef getInputTypeRHS() {
		return DataType.CLASS.typeDef();
	}

	@Override
	public TypeDef getOutputType() {
		return typeDef;
	}

	@Override
	public Priority getPriority() {
		return Priority.NEGATE;
	}

	@Override
	public OperatorOperation getOperationType() {
		return OperatorOperation.NEW;
	}

	@Override
	public List<AInstruction> getOperation(EffectiveLine line, NCompileGraph cg, Throughput... inputs) {
		return new ArrayList<>();
	}
}
