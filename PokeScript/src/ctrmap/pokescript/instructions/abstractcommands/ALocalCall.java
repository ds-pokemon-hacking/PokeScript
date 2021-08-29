package ctrmap.pokescript.instructions.abstractcommands;

import ctrmap.pokescript.OutboundDefinition;
import ctrmap.pokescript.expr.Throughput;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.pokescript.stage1.NExpression;
import ctrmap.pokescript.types.DataType;
import ctrmap.stdlib.util.ArraysEx;
import java.util.List;

public abstract class ALocalCall extends AInstruction {

	public OutboundDefinition call;

	public ALocalCall(String target, NExpression[] arguments, NCompileGraph graph) {
		DataType[] argsTypes = new DataType[arguments.length];
		Throughput[] args = new Throughput[arguments.length];
		for (int i = 0; i < argsTypes.length; i++) {
			args[i] = arguments[i].toThroughput(graph);
		}
		call = new OutboundDefinition(target, args);
	}

	public ALocalCall(OutboundDefinition out) {
		call = out;
	}
	
	public int getArgCount() {
		return call.args.length;
	}
	
	
	@Override
	public List<AInstruction> getAllInstructions() {
		List<AInstruction> l = ArraysEx.asList(this);
		for (int j = 0; j < call.args.length; j++) {
			for (AInstruction i : call.args[j].getCode(DataType.ANY.typeDef())) {
				l.addAll(i.getAllInstructions());
			}
		}
		return l;
	}
}
