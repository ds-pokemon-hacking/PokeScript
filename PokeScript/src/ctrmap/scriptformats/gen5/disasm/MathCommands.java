package ctrmap.scriptformats.gen5.disasm;

import ctrmap.pokescript.instructions.gen5.VOpCode;
import java.util.HashMap;
import java.util.Map;

public enum MathCommands {
	SET_VAR(VOpCode.VarUpdateVar, null),
	SET_CONST(VOpCode.VarUpdateConst, null),
	SET_FLEX(VOpCode.VarUpdateFlex, null),
	
	OP_ADD(VOpCode.VarUpdateAdd, "+"),
	OP_SUB(VOpCode.VarUpdateSub, "-"),
	OP_MUL(VOpCode.VarUpdateMul, "*"),
	OP_DIV(VOpCode.VarUpdateDiv, "/"),
	OP_MOD(VOpCode.VarUpdateMod, "%"),
	
	BIT_OR(VOpCode.VarUpdateOR, "|"),
	BIT_AND(VOpCode.VarUpdateAND, "&")
	;
	
	private static final Map<Integer, MathCommands> map = new HashMap<>();
	
	static {
		for (MathCommands cmd : values()){
			map.put(cmd.opCode.ordinal(), cmd);
		}
	}
	
	public final VOpCode opCode;
	public final String operator;

	private MathCommands(VOpCode opCode, String operator) {
		this.opCode = opCode;
		this.operator = operator;
	}
	
	public static MathCommands valueOf(int i){
		return map.get(i);
	}
}
