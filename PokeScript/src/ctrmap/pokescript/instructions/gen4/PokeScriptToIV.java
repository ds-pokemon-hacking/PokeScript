package ctrmap.pokescript.instructions.gen4;

import ctrmap.pokescript.instructions.ntr.NTRInstructionCall;
import ctrmap.pokescript.instructions.abstractcommands.APlainOpCode;
import ctrmap.pokescript.instructions.ntr.instructions.PlainNTRInstruction;
import static ctrmap.pokescript.instructions.gen4.IVOpCode.*;

/**
 *
 */
public class PokeScriptToIV {

	public static final int NTRVM_PREFIX = 0x8000;

	public static final int PRI_LOC_INC = 0;
	public static final int ALT_LOC_INC = 1;
	public static final int FRM_LOC_INC = 2;
	public static final int STK_LOC_INC = 3;

	public static final int NTR_PRI = NTRVM_PREFIX + PRI_LOC_INC;
	public static final int NTR_ALT = NTRVM_PREFIX + ALT_LOC_INC;

	public static final int NTR_STACKFRM = NTRVM_PREFIX + FRM_LOC_INC;
	public static final int NTR_STACKPTR = NTRVM_PREFIX + STK_LOC_INC;

	public static final int NTR_VAR_MAX = NTRVM_PREFIX + 0xE;

	public static final int PSVM_PREFIX = 0xC000;
	public static final int PSVM_VAR_MAX = PSVM_PREFIX + 0xE;

	public static final int PSVM_DEREFPRI = PSVM_PREFIX + PRI_LOC_INC;
	public static final int PSVM_DEREFALT = PSVM_PREFIX + ALT_LOC_INC;
	public static final int PSVM_GETSTACKFRM = PSVM_PREFIX + FRM_LOC_INC;
	public static final int PSVM_GETSTACKPTR = PSVM_PREFIX + STK_LOC_INC;

	public static PlainNTRInstruction getPlainNTRForOpCode(APlainOpCode opCode, int[] instructionArgs) {
		PlainNTRInstruction ins = new PlainNTRInstruction(opCode, instructionArgs);

		switch (opCode) {
			//Instructions that are NOP-ified since they are not needed on NTRstack. Can be optimized out after assembly.
			case BEGIN_METHOD:
				ins.instructions.add(Nop.createCall());
				break;

			//Instructions that are straight up unsupported by the NTRVM. Sorry:(
			case DIVIDE:
			case MULTIPLY:
			case NEGATE: //this could work by subtracting the value twice from itself, but...
				//NTRVM types are unsigned, rendering number negation essentially useless anyway
				throw new UnsupportedOperationException("Invalid OpCode " + opCode + " for platform NTRVM.");

			//Instructions implemented in more or less a working way
			case ABORT_EXECUTION:
				mergeInstructions(ins, End.createCall());
				break;
			case ADD:
				mergeInstructions(ins, AddVar.createCall(NTR_PRI, NTR_ALT));
				break;
			case SUBTRACT:
				mergeInstructions(ins, SubVar.createCall(NTR_PRI, NTR_ALT));
				break;
			case AND:
				//The inputs are 1 or 0 in PRI and ALT
				//The output should be 1 or 0 in PRI based on whether or not they are both 1
				//We can do that the following way:
				//AddVar(PRI, ALT);			- sum of PRI and ALT
				//IfVarValue(PRI, 2);		- the sum will be 2 if the AND operation is successful (1 + 1)
				//JumpIf(16);				- way less most likely. In Pawn, it would be 8; jump to the thing that sets PRI to 1
				//SetVar(PRI, 0);			- if the previous instruction did not jump, set PRI to 0
				//Jump(8);					- jump over the success instruction
				//SetVar(PRI, 1);			- set PRI to 1 on success
				//end of sector

				//scratch that, method backported from OR is way faster
				mergeInstructions(ins,
						IfVarValue.createCall(NTR_PRI, 0), //If it is 1, PRI allows passthrough and the AND value can be set to ALT
						JumpIf.createCall(IVVMCmpResult.EQUAL, 13), //If it is 0, we want to leave 0 in PRI and skip the ALT copy
						CopyVar.createCall(NTR_ALT, NTR_PRI)
				);

				//end of sector
				break;
			case OR:
				//The inputs are 1 or 0 in PRI and ALT
				//The output should be 1 if either PRI or ALT is 1
				//Two somewhat optimized methods to do that come to mind:
				// 1) add the variables together, set PRI to 1 if it's 2, leave as is otherwise
				// 2) do nothing if PRI is 1, copy ALT to PRI if otherwise

				mergeInstructions(ins,
						IfVarValue.createCall(NTR_PRI, 1),
						JumpIf.createCall(IVVMCmpResult.EQUAL, JumpIf.getSize() + CopyVar.getSize()),
						CopyVar.createCall(NTR_ALT, NTR_PRI)
				);

				break;
			case XOR:
				mergeInstructions(ins,
						IfVarValue.createCall(NTR_PRI, 1),
						JumpIf.createCall(IVVMCmpResult.EQUAL, JumpIf.getSize() + CopyVar.getSize() + Jump.getSize()),
						CopyVar.createCall(NTR_PRI, NTR_ALT),
						Jump.createCall(getNOTSize()) //jump over the NOT
				);
				mergeNOT(ins);

				break;

			case CONST_PRI:
				mergeInstructions(ins, SetVarConst.createCall(NTR_PRI, instructionArgs[0]));
				break;
			case CONST_ALT:
				mergeInstructions(ins, SetVarConst.createCall(NTR_ALT, instructionArgs[0]));
				break;
			case EQUAL:
				createCommonCmpCall(ins, IVVMCmpResult.EQUAL);
				break;
			case NEQUAL:
				createCommonCmpCall(ins, IVVMCmpResult.NEQUAL);
				break;
			case GEQUAL:
				createCommonCmpCall(ins, IVVMCmpResult.GEQUAL);
				break;
			case GREATER:
				createCommonCmpCall(ins, IVVMCmpResult.GREATER);
				break;
			case LEQUAL:
				createCommonCmpCall(ins, IVVMCmpResult.LEQUAL);
				break;
			case LESS:
				createCommonCmpCall(ins, IVVMCmpResult.LESS);
				break;
			case JUMP:
			case JUMP_IF_ZERO:
			case SWITCH:
				throw new UnsupportedOperationException("The JUMP instructions should be used exclusively by the ConditionJump class ! !");
			case NOT:
				mergeNOT(ins);
				break;
			case ZERO_PRI:
				mergeInstructions(ins, SetVarConst.createCall(NTR_PRI, 0));
				break;
			case ZERO_ALT:
				mergeInstructions(ins, SetVarConst.createCall(NTR_ALT, 0));
				break;
			case RETURN:
				mergeInstructions(ins, Return.createCall());
				//props to you, game freak, you did something right for once
				break;
			case MOVE_PRI_TO_ALT:
				mergeInstructions(ins, CopyVar.createCall(NTR_ALT, NTR_PRI));
				break;

			//Stack operations
			case PUSH_PRI:
				adjustStackPtr(ins, 1);
				mergeInstructions(ins, CopyVar.createCall(PSVM_GETSTACKPTR, NTR_PRI));
				break;
			case POP_ALT:
				mergeInstructions(ins, CopyVar.createCall(NTR_ALT, PSVM_GETSTACKPTR));
				adjustStackPtr(ins, -1);
				break;
			case POP_PRI:
				mergeInstructions(ins, CopyVar.createCall(NTR_PRI, PSVM_GETSTACKPTR));
				adjustStackPtr(ins, -1);
				break;
			case PUSH_ALT:
				adjustStackPtr(ins, 1);
				mergeInstructions(ins, CopyVar.createCall(PSVM_GETSTACKPTR, NTR_ALT));
				break;
			case RESIZE_STACK:
				adjustStackPtr(ins, instructionArgs[0]);
				break;
			case LOAD_STACK_PRI:
				mergeInstructions(ins,
						AddVar.createCall(NTR_PRI, NTR_STACKFRM),
						CopyVar.createCall(NTR_PRI, PSVM_DEREFPRI)
				);
				break;
			case STORE_PRI_STACK:
				mergeInstructions(ins,
						CopyVar.createCall(NTR_ALT, NTR_PRI),
						AddVar.createCall(NTR_ALT, NTR_STACKFRM),
						CopyVar.createCall(NTR_PRI, PSVM_DEREFALT)
				);
				break;
		}

		return ins;
	}

	public static int getNOTSize(){
		return IfVarValue.getSize() + JumpIf.getSize() + SetVarConst.getSize() * 2 + Jump.getSize();
	}
	
	private static void mergeNOT(PlainNTRInstruction ins) {
		mergeInstructions(ins,
				IfVarValue.createCall(NTR_PRI, 0),
				JumpIf.createCall(IVVMCmpResult.EQUAL, 16),
				SetVarConst.createCall(NTR_PRI, 0),
				Jump.createCall(8),
				SetVarConst.createCall(NTR_PRI, 1)
		);
	}

	private static void adjustStackPtr(PlainNTRInstruction ins, int amount) {
		IVOpCode op = amount > 0 ? AddVar : IVOpCode.SubVar;
		mergeInstructions(ins, op.createCall(NTR_STACKPTR, Math.abs(amount)));
	}

	private static void createCommonCmpCall(PlainNTRInstruction ins, int cmpCond) {
		mergeInstructions(ins,
				IfVarVar.createCall(NTR_PRI, NTR_ALT),
				//We need to store the result to PRI as well though as per convention. Can and SHOULD be optimized out.
				JumpIf.createCall(cmpCond, JumpIf.getSize() + SetVarConst.getSize() + Jump.getSize()),
				SetVarConst.createCall(NTR_PRI, 0),
				Jump.createCall(Jump.getSize() + SetVarConst.getSize()),
				SetVarConst.createCall(NTR_PRI, 1)
		);
	}

	private static void mergeInstructions(PlainNTRInstruction ins, NTRInstructionCall... calls) {
		for (NTRInstructionCall c : calls) {
			ins.instructions.add(c);
		}
	}
}
