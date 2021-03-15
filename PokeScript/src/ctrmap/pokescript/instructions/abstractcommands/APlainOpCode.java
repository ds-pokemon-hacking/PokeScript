
package ctrmap.pokescript.instructions.abstractcommands;

public enum APlainOpCode {
	//VM ops
	BEGIN_METHOD,
	RETURN,
	ABORT_EXECUTION,
	RESIZE_STACK,
	
	//Stack ops
	PUSH_PRI,
	POP_PRI,
	PUSH_ALT,
	POP_ALT,
	
	//Register ops
	ZERO_PRI,
	ZERO_ALT,
	CONST_PRI,
	CONST_ALT,
	LOAD_STACK_PRI,
	STORE_PRI_STACK,
	MOVE_PRI_TO_ALT,
	MOVE_ALT_TO_PRI,
	
	//Comparison
	EQUAL,
	NEQUAL,
	LEQUAL,
	GEQUAL,
	LESS,
	GREATER,
	
	//Math
	NEGATE,
	ADD,
	SUBTRACT,
	DIVIDE,
	MULTIPLY,
	MODULO,
	
	//Bitwise
	AND, //These might be slightly harder on NTROpCodeProvider
	OR,
	XOR,
	NOT,
	
	//Jumps
	JUMP,
	JUMP_IF_ZERO,
	SWITCH
}
