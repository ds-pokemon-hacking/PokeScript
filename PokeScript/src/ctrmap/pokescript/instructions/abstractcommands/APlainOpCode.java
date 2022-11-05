package ctrmap.pokescript.instructions.abstractcommands;

public enum APlainOpCode {
	//VM ops
	BEGIN_METHOD,		//Notify the VM of subroutine start
	RETURN,				//Pop call stack, branch to parent routine
	ABORT_EXECUTION,	//Force-halt the VM
	RESIZE_STACK,		//Adjust the VM's stack pointer (relative)

	//Stack ops
	PUSH_PRI,			//Push the primary register to stack top
	POP_PRI,			//Pop the topmost stack element to the primary register
	PUSH_ALT,			//Push the alternate register to stack top
	POP_ALT,			//Pop the topmost stack element to the alternate register

	//Register ops
	ZERO_PRI,			//Set the primary register's value to zero
	ZERO_ALT,			//Set the alternate register's value to zero
	CONST_PRI,			//Set the primary register's value to a constant
	CONST_ALT,			//Set the alternate register's value to a constant
	MOVE_PRI_TO_ALT,	//Set the alternate register's value to the primary register's value
	MOVE_ALT_TO_PRI,	//Set the primary register's value to the alternate register's value

	//Comparison
	EQUAL,				//PRI = (PRI == ALT) ? 1 : 0
	NEQUAL,				//PRI = (PRI != ALT) ? 1 : 0
	LEQUAL,				//PRI = (PRI <= ALT) ? 1 : 0
	GEQUAL,				//PRI = (PRI >= ALT) ? 1 : 0
	LESS,				//PRI = (PRI <  ALT) ? 1 : 0
	GREATER,			//PRI = (PRI >  ALT) ? 1 : 0

	//Math
	NEGATE,				//PRI = -PRI
	ADD,				//PRI = PRI + ALT
	SUBTRACT,			//PRI = PRI - ALT
	DIVIDE,				//PRI = PRI / ALT
	MULTIPLY,			//PRI = PRI * ALT
	MODULO,				//ALT = PRI % ALT

	//Bitwise
	AND,				//PRI = PRI & ALT
	OR,					//PRI = PRI | ALT
	XOR,				//PRI = PRI ^ ALT
	NOT,				//PRI = !PRI
	SHL,				//PRI = PRI << ALT
	SHR,				//PRI = PRI >> ALT
	SHL_C,				//PRI = PRI << <constant>
	SHR_C,				//PRI = PRI >> <constant>

	//Jumps
	JUMP,				//Branch to a label unconditionally
	JUMP_IF_ZERO,		//Branch to a label if (PRI == 0)
	SWITCH				//Branch to a case table
}
