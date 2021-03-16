
package ctrmap.pokescript.instructions.gen4;

import ctrmap.pokescript.instructions.ntr.NTRInstructionPrototype;
import ctrmap.pokescript.instructions.ntr.NTRInstructionCall;
import ctrmap.pokescript.instructions.ntr.NTRDataType;
import ctrmap.pokescript.instructions.ntr.NTRArgument;
import static ctrmap.pokescript.instructions.ntr.NTRDataType.*;

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
	IfVarValue(VAR, U16),
	IfVarVar(VAR, VAR),
	Unknown_013,
	GlobalCall,
	LocalCall,
	Jump(S32),
	JumpIfObjId,
	JumpIfBgId,
	JumpIfPlayerDir,
	CallFunc,
	Return(),
	JumpIf(U8, S32),
	CallIf,
	FlagSet,
	FlagReset,
	FlagGet,
	FlagGetVar,
	FlagSetVar,
	TrainerFlagSet,
	TrainerFlagReset,
	TrainerFlagGet,
	AddVar(VAR, FLEX),
	SubVar(VAR, FLEX),
	SetVarConst(VAR, U16),
	CopyVar(VAR, VAR),
	SetVarFlex;
	
	public final NTRInstructionPrototype proto;
	
	private IVOpCode(){
		proto = null;
	}
	
	private IVOpCode(NTRDataType... argTypes) {
		NTRArgument[] args = new NTRArgument[argTypes.length];
		for (int i = 0; i < argTypes.length; i++) {
			args[i] = new NTRArgument(argTypes[i]);
		}
		this.proto = new NTRInstructionPrototype(ordinal(), args);
		proto.debugName = toString();
	}
	
	public NTRInstructionCall createCall(int... args){
		return new NTRInstructionCall(proto, args);
	}
	
	public int getSize(){
		return proto.getSize();
	}
}
