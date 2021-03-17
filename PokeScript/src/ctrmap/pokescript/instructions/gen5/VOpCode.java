package ctrmap.pokescript.instructions.gen5;

import ctrmap.pokescript.instructions.ntr.NTRArgument;
import ctrmap.pokescript.instructions.ntr.NTRDataType;
import ctrmap.pokescript.instructions.ntr.NTRInstructionCall;
import ctrmap.pokescript.instructions.ntr.NTRInstructionPrototype;

public enum VOpCode {
	Nop(),
	Dummy(),
	Halt(),
	Suspend(NTRDataType.U16),
	Call(NTRDataType.S32),
	Return(),
	ReadVar(),
	PopStackAndReadVar(),
	PushConst(NTRDataType.U16),
	PushVar(NTRDataType.FLEX),
	PopToVar(NTRDataType.FLEX),
	PopAndDiscard(),
	//Pri and Alt are the topmost and second from the top elements of the stack respectively
	//Subtraction and division are done in the way that Pri OP Alt (not Alt OP Pri)

	AddPriAlt(),
	SubPriAlt(),
	MulPriAlt(),
	DivPriAlt(),
	PushEventFlag(NTRDataType.FLEX),
	CmpPriAlt(NTRDataType.U16), //using VStackCmpOpRequest, PRI = result

	VarUpdateAND(NTRDataType.VAR, NTRDataType.FLEX), //VAR = VAR & FLEX
	VarUpdateOR(NTRDataType.VAR, NTRDataType.FLEX), //VAR = VAR | FLEX

	VMVarSetU8(NTRDataType.U8, NTRDataType.U8),
	VMVarSetU32(NTRDataType.U8, NTRDataType.S32),
	VMVarSetVMVar(NTRDataType.U8, NTRDataType.U8), //Var[a1] = Var[a2]
	CmpVMVarVMVar(NTRDataType.U8, NTRDataType.U8), //CmpResult = Var[a1] OP Var[a2]
	CmpVMVarConst(NTRDataType.U8, NTRDataType.U16), //CmpResult = Var[a1] OP Const

	CmpVarConst(NTRDataType.VAR, NTRDataType.U16),
	CmpVarVar(NTRDataType.VAR, NTRDataType.VAR),
	VMAddNew(NTRDataType.U16),
	GlobalCall(NTRDataType.U16),
	Halt2(),
	Jump(NTRDataType.S32),
	JumpOnCmp(NTRDataType.U8, NTRDataType.S32), //VCmpResultRequest, BranchTgt
	CallOnCmp(NTRDataType.U8, NTRDataType.S32), //VCmpResultRequest, BranchTgt

	SetMapEventStatusFlag(NTRDataType.U16),
	StoreMapTypeChange(NTRDataType.VAR),
	FlagSet(NTRDataType.FLEX),
	FlagReset(NTRDataType.FLEX),
	VarSetFromFlag(NTRDataType.VAR, NTRDataType.FLEX),
	VarUpdateAdd(NTRDataType.VAR, NTRDataType.FLEX),
	VarUpdateSub(NTRDataType.VAR, NTRDataType.FLEX),
	VarUpdateConst(NTRDataType.VAR, NTRDataType.U16),
	VarUpdateVar(NTRDataType.VAR, NTRDataType.VAR),
	VarUpdateFlex(NTRDataType.VAR, NTRDataType.FLEX),
	VarUpdateMul(NTRDataType.VAR, NTRDataType.FLEX),
	VarUpdateDiv(NTRDataType.VAR, NTRDataType.FLEX),
	VarUpdateMod(NTRDataType.VAR, NTRDataType.FLEX),;

	public final NTRInstructionPrototype proto;

	private VOpCode(NTRDataType... argTypes) {
		NTRArgument[] args = new NTRArgument[argTypes.length];
		for (int i = 0; i < argTypes.length; i++) {
			args[i] = new NTRArgument(argTypes[i]);
		}
		this.proto = new NTRInstructionPrototype(ordinal(), args);
		proto.debugName = toString();
	}

	public NTRInstructionCall createCall(int... args) {
		return new NTRInstructionCall(proto, args);
	}

	public int getSize() {
		return proto.getSize();
	}
	
	public static VOpCode parse(String str){
		for (VOpCode opc : values()){
			if (opc.name().equals(str)){
				return opc;
			}
		}
		return null;
	}
}
