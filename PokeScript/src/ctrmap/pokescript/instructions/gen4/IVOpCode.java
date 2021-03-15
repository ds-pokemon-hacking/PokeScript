
package ctrmap.pokescript.instructions.gen4;

import ctrmap.pokescript.instructions.ntr.NTRInstructionPrototype;
import ctrmap.pokescript.instructions.ntr.NTRInstructionCall;
import ctrmap.pokescript.instructions.ntr.NTRDataType;
import ctrmap.pokescript.instructions.ntr.NTRArgument;

/**
 *
 */
public enum IVOpCode {
	Nop(),
	Dummy,
	End(),
	WaitTime,
	RegValueSet,
	RegDataSet,
	RegAddrSet,
	AddrValueSet,
	AddrRegSet,
	RegRegSet,
	AddrAddrSet,
	IfRegReg,
	IfRegValue,
	IfRegAddr,
	IfAddrReg,
	IfAddrValue,
	IfAddrAddr,
	IfVarValue(new NTRInstructionPrototype(new NTRArgument(NTRDataType.VAR), new NTRArgument(NTRDataType.U16))),
	IfVarVar(new NTRInstructionPrototype(new NTRArgument(NTRDataType.VAR), new NTRArgument(NTRDataType.VAR))),
	Unknown_013,
	GlobalCall,
	LocalCall,
	Jump(new NTRInstructionPrototype(new NTRArgument(NTRDataType.S32))),
	JumpIfObjId,
	JumpIfBgId,
	JumpIfPlayerDir,
	CallFunc,
	Return(),
	JumpIf(new NTRInstructionPrototype(new NTRArgument(NTRDataType.U8), new NTRArgument(NTRDataType.S32))),
	CallIf,
	FlagSet,
	FlagReset,
	FlagGet,
	FlagGetVar,
	FlagSetVar,
	TrainerFlagSet,
	TrainerFlagReset,
	TrainerFlagGet,
	AddVar(new NTRInstructionPrototype(new NTRArgument(NTRDataType.VAR), new NTRArgument(NTRDataType.FLEX))),
	SubVar(new NTRInstructionPrototype(new NTRArgument(NTRDataType.VAR), new NTRArgument(NTRDataType.FLEX))),
	SetVarConst(new NTRInstructionPrototype(new NTRArgument(NTRDataType.VAR), new NTRArgument(NTRDataType.U16))),
	CopyVar(new NTRInstructionPrototype(new NTRArgument(NTRDataType.VAR), new NTRArgument(NTRDataType.VAR))),
	SetVarFlex;
	
	public final NTRInstructionPrototype proto;
	
	private IVOpCode(){
		proto = new NTRInstructionPrototype();
	}
	
	private IVOpCode(NTRInstructionPrototype proto){
		this.proto = proto;
		proto.opCode = ordinal();
	}
	
	public NTRInstructionCall createCall(int... args){
		return new NTRInstructionCall(proto, args);
	}
	
	public int getSize(){
		return proto.getSize();
	}
}
