package ctrmap.pokescript.instructions.abstractcommands;


public enum AInstructionType {
	PLAIN,
	PLAIN_FLOAT,
	JUMP,
	CASE_TABLE,
	
	SET_VARIABLE,
	GET_VARIABLE,
	
	CALL_LOCAL,
	CALL_NATIVE,
	CALL_META
}
