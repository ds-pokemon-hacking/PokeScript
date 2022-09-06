package ctrmap.pokescript.expr;

import ctrmap.pokescript.InboundDefinition;
import ctrmap.pokescript.OutboundDefinition;
import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.instructions.abstractcommands.ALocalCall;
import ctrmap.pokescript.stage0.EffectiveLine;
import ctrmap.pokescript.stage0.Modifier;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.pokescript.types.DataType;
import ctrmap.pokescript.types.TypeDef;
import java.util.ArrayList;
import java.util.List;

public class CallOperator extends Operator {

	public final String funcName;
	private InboundDefinition def;

	public CallOperator(String funcName) {
		this.funcName = funcName;
	}

	@Override
	public String toString() {
		return "Call:" + funcName;
	}

	@Override
	public TypeDef getInputTypeLHS() {
		return DataType.VOID.typeDef();
	}

	@Override
	public TypeDef getInputTypeRHS() {
		return DataType.ANY.typeDef();
	}

	@Override
	public TypeDef getOutputType() {
		if (def != null) {
			return def.retnType;
		}
		return DataType.ANY.typeDef();
	}

	@Override
	public OperatorOperation getOperationType() {
		return OperatorOperation.CALL;
	}

	@Override
	public Priority getPriority() {
		return Priority.CALL;
	}

	@Override
	public List<AInstruction> getOperation(EffectiveLine line, NCompileGraph cg, Throughput... inputs) {
		List<AInstruction> l = new ArrayList<>();

		OutboundDefinition def = new OutboundDefinition(funcName, inputs);
		InboundDefinition m = cg.resolveMethod(def);

		if (m != null) {
			ALocalCall ins = cg.getMethodCall(def, m);
			ins.init(cg);
			l.add(ins);
		} else {
			InboundDefinition close = cg.findBestMatchingMethod(def);
			List<String> reasons = new ArrayList<>();
			if (close != null) {
				for (int i = 0; i < close.args.length; i++) {
					String argName = close.args[i].name;
					if (def.args[i] == null) {
						reasons.add("Argument '" + argName + "' resolved to null (internal error).");
					}
					else if (close.args[i].requestedModifiers.contains(Modifier.FINAL) && !def.args[i].isImmediate()) {
						reasons.add("Argument '" + argName + "' is not final.");
					}
					else if (!def.args[i].type.acceptsIncoming(close.args[i].typeDef)) {
						reasons.add("Argument '" + argName + "' type " + def.args[i].type + " can not be cast to " + close.args[i].typeDef);
					}
				}
			}
			else {
				InboundDefinition byName = cg.findMethodMatch(def.name);
				if (byName != null) {
					line.throwException(byName.name + " requires the following parameters: " + byName);
				}
			}
			for (String reason : reasons) {
				line.throwException(reason);
			}
			line.throwException("Could not resolve method: " + def);
		}
		this.def = m;

		return l;
	}
}
