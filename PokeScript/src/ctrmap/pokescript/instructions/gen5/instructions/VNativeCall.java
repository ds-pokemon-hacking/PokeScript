package ctrmap.pokescript.instructions.gen5.instructions;

import ctrmap.pokescript.OutboundDefinition;
import ctrmap.pokescript.data.LocalDataGraph;
import ctrmap.pokescript.data.Variable;
import ctrmap.pokescript.instructions.abstractcommands.ACompiledInstruction;
import ctrmap.pokescript.instructions.abstractcommands.ANativeCall;
import ctrmap.pokescript.instructions.gen5.VConstants;
import ctrmap.pokescript.instructions.gen5.VOpCode;
import ctrmap.pokescript.instructions.ntr.NTRInstructionCall;
import ctrmap.pokescript.instructions.ntr.NTRInstructionConstructor;
import ctrmap.pokescript.stage0.Modifier;
import ctrmap.pokescript.stage1.NCompilableMethod;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.pokescript.types.DataType;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class VNativeCall extends ANativeCall {

	public VNativeCall(OutboundDefinition out) {
		super(out);
	}

	@Override
	public int getAllocatedPointerSpace(NCompileGraph g) {
		int ptr = 0;
		List<? extends ACompiledInstruction> precomp = compile(g);
		for (ACompiledInstruction i : precomp) {
			ptr += i.getSize();
		}
		return ptr;
	}

	@Override
	public List<? extends ACompiledInstruction> compile(NCompileGraph g) {
		List<ACompiledInstruction> l = new ArrayList<>();

		//This essentially compiles into a NTRInstructionCall with the given opCode
		//I think it's better to forbid calling the internal functions defined in VOpCode and rather use ones provided by the CG
		LocalDataGraph locals = g.getCurrentMethod().locals;
		int localsCount = locals.variables.size();
		int usedExtraLocalsNum = 0;

		NCompilableMethod def = g.getMethodByDef(call);

		int[] nativeArgs = new int[getArgCount()];

		//Compile the input and store it onto the stack
		for (int i = 0; i < getArgCount(); i++) {
			if (def.def.args[i].requestedModifiers.contains(Modifier.FINAL)) {
				if (!call.args[i].isImmediate()) {
					g.currentCompiledLine.throwException("Constant argument required.");
				} else {
					nativeArgs[i] = call.args[i].getImmediateValue();
				}
			} else {
				if (call.args[i].isImmediate()) {
					nativeArgs[i] = call.args[i].getImmediateValue();
				} else if (call.args[i].isVariable()) {
					Variable var = call.args[i].getVariable();
					nativeArgs[i] = var.index + VConstants.VAR_START_LOCAL;
				} else {
					l.addAll(NTRInstructionCall.compileIL(call.args[i].getCode(DataType.ANY), g));

					//the result is now in the primary GPR
					int destVar = VConstants.VAR_START_LOCAL + localsCount + usedExtraLocalsNum;
					l.add(VOpCode.VarUpdateVar.createCall(destVar, VConstants.GP_REG_PRI));
					nativeArgs[i] = destVar;
					usedExtraLocalsNum++;
				}
			}
		}

		l.add(new NTRInstructionCall(NTRInstructionConstructor.constructFromMethodHeader(g, def.def), nativeArgs));

		return l;
	}
}
