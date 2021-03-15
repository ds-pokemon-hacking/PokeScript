
package ctrmap.pokescript.instructions.ctr;

import ctrmap.scriptformats.gen6.PawnInstruction;
import ctrmap.pokescript.instructions.abstractcommands.APlainOpCode;

/**
 *
 */
public class PokeScriptToPawnOpCode {
	public static PawnInstruction.Commands getOpCode(APlainOpCode plain){
		switch (plain){
			case ABORT_EXECUTION:
				return PawnInstruction.Commands.HALT;
			case BEGIN_METHOD:
				return PawnInstruction.Commands.PROC;
			case CONST_ALT:
				return PawnInstruction.Commands.CONST_ALT;
			case CONST_PRI:
				return PawnInstruction.Commands.CONST_PRI;
			case LOAD_STACK_PRI:
				return PawnInstruction.Commands.LOAD_S_PRI;
			case POP_ALT:
				return PawnInstruction.Commands.PALT;
			case POP_PRI:
				return PawnInstruction.Commands.PPRI;
			case PUSH_ALT:
				return PawnInstruction.Commands.PUSH_ALT;
			case PUSH_PRI:
				return PawnInstruction.Commands.PUSH_PRI;
			case RESIZE_STACK:
				return PawnInstruction.Commands.STACK;
			case RETURN:
				return PawnInstruction.Commands.RETN;
			case STORE_PRI_STACK:
				return PawnInstruction.Commands.STOR_S_PRI;
			case ZERO_ALT:
				return PawnInstruction.Commands.ZERO_ALT;
			case ZERO_PRI:
				return PawnInstruction.Commands.ZERO_PRI;
			case ADD:
				return PawnInstruction.Commands.ADD;
			case AND:
				return PawnInstruction.Commands.AND;
			case DIVIDE:
				return PawnInstruction.Commands.SDIV_ALT;
			case EQUAL:
				return PawnInstruction.Commands.EQ;
			case GEQUAL:
				return PawnInstruction.Commands.SGEQ;
			case GREATER:
				return PawnInstruction.Commands.SGRTR;
			case LEQUAL:
				return PawnInstruction.Commands.SLEQ;
			case LESS:
				return PawnInstruction.Commands.SLESS;
			case MOVE_PRI_TO_ALT:
				return PawnInstruction.Commands.MOVE_ALT;
			case MOVE_ALT_TO_PRI:
				return PawnInstruction.Commands.MOVE_PRI;
			case MULTIPLY:
				return PawnInstruction.Commands.SMUL;
			case MODULO:
				return PawnInstruction.Commands.SDIV_ALT;
			case NEGATE:
				return PawnInstruction.Commands.NEG;
			case NEQUAL:
				return PawnInstruction.Commands.NEQ;
			case NOT:
				return PawnInstruction.Commands.NOT;
			case OR:
				return PawnInstruction.Commands.OR;
			case SUBTRACT:
				return PawnInstruction.Commands.SUB_ALT;
			case XOR:
				return PawnInstruction.Commands.XOR;
			case JUMP:
				return PawnInstruction.Commands.JUMP;
			case JUMP_IF_ZERO:
				return PawnInstruction.Commands.JZER;
			case SWITCH:
				return PawnInstruction.Commands.SWITCH;
		}
		return null;
	}
}
