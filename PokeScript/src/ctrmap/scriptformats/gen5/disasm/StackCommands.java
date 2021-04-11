package ctrmap.scriptformats.gen5.disasm;

import ctrmap.pokescript.instructions.gen5.VOpCode;
import java.util.HashMap;
import java.util.Map;

public enum StackCommands {
	POP_AND_DEREF(VOpCode.PopStackAndReadVar),
	POP(VOpCode.PopAndDiscard),
	POP_TO(VOpCode.PopToVar),
	
	PUSH_CONST(VOpCode.PushConst),
	PUSH_VAR(VOpCode.PushVar),
	PUSH_FLAG(VOpCode.PushEventFlag),
	
	ADD(VOpCode.AddPriAlt),
	SUB(VOpCode.SubPriAlt),
	MUL(VOpCode.MulPriAlt),
	DIV(VOpCode.DivPriAlt),
	
	CMP(VOpCode.CmpPriAlt)
	;
	
	private static final Map<Integer, StackCommands> map = new HashMap<>();
	
	static {
		for (StackCommands cmd : values()){
			map.put(cmd.opCode.ordinal(), cmd);
		}
	}
	
	public final VOpCode opCode;

	private StackCommands(VOpCode opCode) {
		this.opCode = opCode;
	}
	
	public static StackCommands valueOf(int i){
		return map.get(i);
	}
}
