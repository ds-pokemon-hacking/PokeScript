package ctrmap.pokescript.instructions.gen5.instructions;

import ctrmap.scriptformats.gen6.PawnInstruction;
import ctrmap.pokescript.OutboundDefinition;
import ctrmap.pokescript.data.LocalDataGraph;
import ctrmap.pokescript.data.Variable;
import ctrmap.pokescript.instructions.abstractcommands.ACompiledInstruction;
import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.instructions.abstractcommands.ALocalCall;
import ctrmap.pokescript.instructions.gen5.VConstants;
import ctrmap.pokescript.instructions.gen5.VOpCode;
import ctrmap.pokescript.instructions.ntr.NTRInstructionCall;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.pokescript.types.DataType;
import ctrmap.stdlib.util.ArraysEx;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class VLocalCall extends ALocalCall {

	public VLocalCall(OutboundDefinition out) {
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

		//First, push the currently used variables to the stack
		//This could potentially result in bugs if any of the vars were changed in the args creation. Let's just hope noone does that (using things like i++ etc.)
		
		LocalDataGraph locals = g.getCurrentMethod().locals;
		for (int i = 0; i < locals.variables.size(); i++) {
			l.add(VOpCode.PushVar.createCall(VConstants.VAR_START_LOCAL + i));
		}

		//Compile the input and store it onto the stack
		for (int i = 0; i < getArgCount(); i++) {
			l.addAll(NTRInstructionCall.compileIL(call.args[i].getCode(DataType.ANY), g));

			//the result is now in the primary GPR
			l.add(VOpCode.PushVar.createCall(VConstants.GP_REG_PRI));
			//push the GPR to the stack
		}

		//All variables are in their final state and both them and the args have been pushed to the stack. Now we just need to get the args back from the stack
		for (int i = getArgCount() - 1; i >= 0; i--){
			l.add(VOpCode.PopToVar.createCall(VConstants.VAR_START_LOCAL + i));
		}
		
		int ptr = pointer;
		for (ACompiledInstruction i : l) {
			ptr += i.getSize();
		}
		ptr += VOpCode.Call.getSize();
		
		//Done! Args are now in the first N variables. We can safely call the method now.
		l.add(VOpCode.Call.createCall(g.getMethodByDef(call).getPointer() - ptr));
		
		//Pop back the variables' original values
		for (int i = locals.variables.size() - 1; i >= 0; i--){
			l.add(VOpCode.PopToVar.createCall(VConstants.VAR_START_LOCAL + i));
		}
		
		return l;
	}
}
