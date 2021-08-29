package ctrmap.pokescript.expr;

import ctrmap.pokescript.types.DataType;
import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.instructions.abstractcommands.APlainOpCode;
import ctrmap.pokescript.stage0.EffectiveLine;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.pokescript.types.TypeDef;
import java.util.ArrayList;
import java.util.List;

public class Not extends Operator{
	
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
	protected List<AInstruction> createOperation(NCompileGraph cg) {
		throw new UnsupportedOperationException("This method should never have been called on this class.");
	}
		
	@Override
	public List<AInstruction> getOperation(Throughput left, Throughput right, EffectiveLine line, NCompileGraph cg) {
		List<AInstruction> r = new ArrayList<>();
		//not only takes one input, the parser has adjusted that to always be right
		r.addAll(right.getCode(getInputTypeRHS()));
		r.add(cg.getPlain(APlainOpCode.NOT));
		return r;
	}

	@Override
	public Priority getPriority() {
		return Priority.NORMAL;
	}
}
