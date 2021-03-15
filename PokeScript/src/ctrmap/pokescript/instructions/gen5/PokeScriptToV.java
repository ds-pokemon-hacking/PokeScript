package ctrmap.pokescript.instructions.gen5;

import ctrmap.pokescript.instructions.abstractcommands.APlainOpCode;
import ctrmap.pokescript.instructions.ntr.NTRInstructionCall;
import ctrmap.pokescript.instructions.ntr.instructions.PlainNTRInstruction;
import static ctrmap.pokescript.instructions.gen5.VOpCode.*;
import static ctrmap.pokescript.instructions.gen5.VConstants.*;

/**
 *
 */
public class PokeScriptToV {

	public static PlainNTRInstruction getPlainNTRForOpCode(APlainOpCode opCode, int[] instructionArgs) {
		PlainNTRInstruction ins = new PlainNTRInstruction(opCode, instructionArgs);

		switch (opCode) {
			case ABORT_EXECUTION:
				mergeInstructions(ins, Halt.createCall());
				break;
			case ADD:
				setArithmetic(ins, VarUpdateAdd);
				break;
			case SUBTRACT:
				setArithmetic(ins, VarUpdateSub);
				break;
			case AND:
				setArithmetic(ins, VarUpdateAND);
				break;
			case OR:
				setArithmetic(ins, VarUpdateOR);
				break;
			case DIVIDE:
				setArithmetic(ins, VarUpdateDiv);
				break;
			case MULTIPLY:
				setArithmetic(ins, VarUpdateMul);
				break;
			case MODULO:
				setArithmetic(ins, VarUpdateMod);
				//PokeScript is based on Pawn which expects the modulo result in the ALT register
				//This will move the result into ALT, but the subsequent movement back into PRI should be optimized out along with it
				mergeInstructions(ins, VarUpdateVar.createCall(GP_REG_ALT, GP_REG_PRI));
				break;
			case BEGIN_METHOD:
				//Nothing like this is needed in Gen V
				break;
			case CONST_ALT:
				mergeInstructions(ins, VarUpdateConst.createCall(GP_REG_ALT, instructionArgs[0]));
				break;
			case CONST_PRI:
				mergeInstructions(ins, VarUpdateConst.createCall(GP_REG_PRI, instructionArgs[0]));
				break;
			case ZERO_ALT:
				mergeInstructions(ins, VarUpdateConst.createCall(GP_REG_ALT, 0));
				break;
			case ZERO_PRI:
				mergeInstructions(ins, VarUpdateConst.createCall(GP_REG_PRI, 0));
				break;
			case STORE_PRI_STACK:
				mergeInstructions(ins, VarUpdateVar.createCall(VAR_START_LOCAL + instructionArgs[0], GP_REG_PRI));
				break;
			case LOAD_STACK_PRI:
				mergeInstructions(ins, VarUpdateVar.createCall(GP_REG_PRI, VAR_START_LOCAL + instructionArgs[0]));
				break;
			case RESIZE_STACK:
				//Stack is fixed size in Gen V
				break;
			case RETURN:
				mergeInstructions(ins, Return.createCall());
				break;
			case PUSH_PRI:
				mergeInstructions(ins, PushVar.createCall(GP_REG_PRI));
				break;
			case PUSH_ALT:
				mergeInstructions(ins, PushVar.createCall(GP_REG_ALT));
				break;
			case POP_PRI:
				mergeInstructions(ins, PopToVar.createCall(GP_REG_PRI));
				break;
			case POP_ALT:
				mergeInstructions(ins, PopToVar.createCall(GP_REG_ALT));
				break;
			case NEGATE:
				mergeInstructions(ins, VarUpdateMul.createCall(GP_REG_PRI, -1)); //NOT SURE IF THIS WILL WORK. I'm pretty sure 99% of stuff is unsigned.
				break;
			case MOVE_ALT_TO_PRI:
				mergeInstructions(ins, VarUpdateVar.createCall(GP_REG_PRI, GP_REG_ALT));
				break;
			case MOVE_PRI_TO_ALT:
				mergeInstructions(ins, VarUpdateVar.createCall(GP_REG_ALT, GP_REG_PRI));
				break;
			case NOT:
				mergeInstructions(ins,
						PushVar.createCall(GP_REG_PRI, 0),
						PushConst.createCall(1),
						CmpPriAlt.createCall(VStackCmpOpRequest.EQUAL),
						PopToVar.createCall(GP_REG_PRI) //the CmpPriAlt will push 0 to the stack if they are equal, effectively negating them.
				);
				break;
			case EQUAL:
				//For whatever reason, the value that the stack is set to on success is 0, not 1. For most ops, we can just invert the condition.
				createCommonCmpCall(ins, VStackCmpOpRequest.NEQUAL);
				break;
			case GEQUAL:
				createCommonCmpCall(ins, VStackCmpOpRequest.LESS);
				break;
			case GREATER:
				createCommonCmpCall(ins, VStackCmpOpRequest.LEQUAL);
				break;
			case LEQUAL:
				createCommonCmpCall(ins, VStackCmpOpRequest.GREATER);
				break;
			case NEQUAL:
				createCommonCmpCall(ins, VStackCmpOpRequest.EQUAL);
				break;
			case LESS:
				createCommonCmpCall(ins, VStackCmpOpRequest.GEQUAL);
				break;
			case JUMP:
			case JUMP_IF_ZERO:
			case SWITCH:
				throw new UnsupportedOperationException("These should only be created through VConditionJump!");
			case XOR:
				throw new UnsupportedOperationException("Can not XOR in Gen V");
		}

		return ins;
	}
	
	private static void createCommonCmpCall(PlainNTRInstruction ins, int cmpCond) {
		mergeInstructions(ins,
				PushVar.createCall(GP_REG_ALT),
				PushVar.createCall(GP_REG_PRI),
				//We need to store the result to PRI as well though as per convention. Can and SHOULD be optimized out.
				CmpPriAlt.createCall(cmpCond),
				PopToVar.createCall(GP_REG_PRI)
		);
	}

	private static void setArithmetic(PlainNTRInstruction ins, VOpCode opCode) {
		ins.instructions.add(opCode.createCall(GP_REG_PRI, GP_REG_ALT));
	}

	private static void mergeInstructions(PlainNTRInstruction ins, NTRInstructionCall... calls) {
		for (NTRInstructionCall c : calls) {
			ins.instructions.add(c);
		}
	}
}
