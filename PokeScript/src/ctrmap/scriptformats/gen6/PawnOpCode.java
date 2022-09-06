package ctrmap.scriptformats.gen6;

import xstandard.text.StringEx;

public enum PawnOpCode {
	NONE,
	LOAD_PRI(1),
	LOAD_ALT(1),
	LOAD_S_PRI(1),
	LOAD_S_ALT(1),
	LREF_PRI(1),
	LREF_ALT(1),
	LREF_S_PRI(1),
	LREF_S_ALT(1),
	LOAD_I,
	LODB_I,
	CONST_PRI(1),
	CONST_ALT(1),
	ADDR_PRI(1),
	ADDR_ALT(1),
	STOR_PRI(1),
	STOR_ALT(1),
	STOR_S_PRI(1),
	STOR_S_ALT(1),
	SREF_PRI(1),
	SREF_ALT(1),
	SREF_S_PRI(1),
	SREF_S_ALT(1),
	STOR_I,
	STRB_I(1),
	LIDX,
	LIDX_B(1),
	IDXADDR,
	IDXADDR_B(1),
	ALIGN_PRI(1),
	ALIGN_ALT(1),
	LCTRL(1),
	SCTRL(1),
	MOVE_PRI,
	MOVE_ALT,
	XCHG,
	PUSH_PRI,
	PUSH_ALT,
	PICK(1),
	PUSH_C(1),
	PUSH(1),
	PUSH_S(1),
	PPRI,
	PALT,
	STACK(1),
	HEAP(1),
	PROC,
	RET,
	RETN,
	CALL(1),
	CALL_PRI,
	JUMP(1),
	JREL(1),
	JZER(1),
	JNZ(1),
	JEQ(1),
	JNEQ(1),
	JLESS(1),
	JLEQ(1),
	JGRTR(1),
	JGEQ(1),
	JSLESS(1),
	JSLEQ(1),
	JSGRTR(1),
	JSGEQ(1),
	SHL,
	SHR,
	SSHR,
	SHL_C_PRI(1),
	SHL_C_ALT(1),
	SHR_C_PRI(1),
	SHR_C_ALT(1),
	SMUL,
	SDIV,
	SDIV_ALT,
	UMUL,
	UDIV,
	UDIV_ALT,
	ADD,
	SUB,
	SUB_ALT,
	AND,
	OR,
	XOR,
	NOT,
	NEG,
	INVERT,
	ADD_C(1),
	SMUL_C(1),
	ZERO_PRI,
	ZERO_ALT,
	ZERO(1),
	ZERO_S(1),
	SIGN_PRI,
	SIGN_ALT,
	EQ,
	NEQ,
	LESS,
	LEQ,
	GRTR,
	GEQ,
	SLESS,
	SLEQ,
	SGRTR,
	SGEQ,
	EQ_C_PRI(1),
	EQ_C_ALT(1),
	INC_PRI,
	INC_ALT,
	INC(1),
	INC_S(1),
	INC_I,
	DEC_PRI,
	DEC_ALT,
	DEC(1),
	DEC_S(1),
	DEC_I,
	MOVS(1),
	CMPS(1),
	FILL(1),
	HALT(1),
	BOUNDS(1),
	SYSREQ_PRI,
	SYSREQ_C(1),
	FILE(3),
	LINE(2),
	SYMBOL(4),
	SRANGE(2),
	JUMP_PRI,
	SWITCH(1),
	CASETBL,
	SWAP_PRI,
	SWAP_ALT,
	PUSH_ADR(1),
	NOP,
	SYSREQ_N(2),
	SYMTAG(1),
	BREAK,
	PUSH2_C(2),
	PUSH2(2),
	PUSH2_S(2),
	PUSH2_ADR(2),
	PUSH3_C(3),
	PUSH3(3),
	PUSH3_S(3),
	PUSH3_ADR(3),
	PUSH4_C(4),
	PUSH4(4),
	PUSH4_S(4),
	PUSH4_ADR(4),
	PUSH5_C(5),
	PUSH5(5),
	PUSH5_S(5),
	PUSH5_ADR(5),
	LOAD_BOTH(2),
	LOAD_S_BOTH(2),
	CONST(2),
	CONST_S(2),
	/* overlay instructions */
	ICALL(1),
	IRETN,
	ISWITCH(1),
	ICASETBL,
	/* packed instructions */
	LOAD_P_PRI(LOAD_PRI),
	LOAD_P_ALT(LOAD_ALT),
	LOAD_P_S_PRI(LOAD_S_PRI),
	LOAD_P_S_ALT(LOAD_S_ALT),
	LREF_P_PRI(LREF_PRI),
	LREF_P_ALT(LREF_ALT),
	LREF_P_S_PRI(LREF_S_PRI),
	LREF_P_S_ALT(LREF_S_ALT),
	LODB_P_I(LODB_I),
	CONST_P_PRI(CONST_PRI),
	CONST_P_ALT(CONST_ALT),
	ADDR_P_PRI(ADDR_PRI),
	ADDR_P_ALT(ADDR_ALT),
	STOR_P_PRI(STOR_PRI),
	STOR_P_ALT(STOR_ALT),
	STOR_P_S_PRI(STOR_S_PRI),
	STOR_P_S_ALT(STOR_S_ALT),
	SREF_P_PRI(SREF_PRI),
	SREF_P_ALT(SREF_ALT),
	SREF_P_S_PRI(SREF_S_PRI),
	SREF_P_S_ALT(SREF_S_ALT),
	STRB_P_I(STRB_I),
	LIDX_P_B(LIDX_B),
	IDXADDR_P_B(IDXADDR_B),
	ALIGN_P_PRI(ALIGN_PRI),
	ALIGN_P_ALT(ALIGN_ALT),
	PUSH_P_C(PUSH_C),
	PUSH_P(PUSH),
	PUSH_P_S(PUSH_S),
	STACK_P(STACK),
	HEAP_P(HEAP),
	SHL_P_C_PRI(SHL_C_PRI),
	SHL_P_C_ALT(SHL_C_ALT),
	SHR_P_C_PRI(SHR_C_PRI),
	SHR_P_C_ALT(SHR_C_ALT),
	ADD_P_C(ADD_C),
	SMUL_P_C(SMUL_C),
	ZERO_P(ZERO),
	ZERO_P_S(ZERO_S),
	EQ_P_C_PRI(EQ_C_PRI),
	EQ_P_C_ALT(EQ_C_ALT),
	INC_P(INC),
	INC_P_S(INC_S),
	DEC_P(DEC),
	DEC_P_S(DEC_S),
	MOVS_P(MOVS),
	CMPS_P(CMPS),
	FILL_P(FILL),
	HALT_P(HALT),
	BOUNDS_P(BOUNDS),
	PUSH_P_ADR(PUSH_ADR),
	SYSREQ_D(1),
	SYSREQ_ND(2),
	
	NUM_OPCODES;
	
	public static final PawnOpCode[] OPCODES = values();
	
	public final String compactName;
	
	public final boolean packed;
	public final int argumentCount;
	
	private PawnOpCode packedEquivalent;
	private PawnOpCode unpackedEquivalent;
	
	public boolean isMacro(){
		return argumentCount > 1;
	}
	
	public PawnOpCode getPackedEquivalent() {
		return packedEquivalent == null ? this : packedEquivalent;
	}
	
	public PawnOpCode getUnpackedEquivalent() {
		return unpackedEquivalent;
	}
	
	private PawnOpCode(){
		this(false, 0);
	}
	
	private PawnOpCode(boolean packed, int argCount) {
		this.packed = packed;
		argumentCount = argCount;
		unpackedEquivalent = this;
		compactName = StringEx.deleteAllChars(name(), '_');
	}
	
	private PawnOpCode(boolean packed) {
		this(packed, packed ? 1 : 0);
		if (packed) {
			packedEquivalent = this;
		}
		else {
			unpackedEquivalent = this;
		}
	}
	
	private PawnOpCode(PawnOpCode unpackedEquiv) {
		this(true);
		unpackedEquivalent = unpackedEquiv;
		unpackedEquiv.packedEquivalent = this;
	}
	
	private PawnOpCode(int argumentCount) {
		this(false, argumentCount);
		this.unpackedEquivalent = this;
	}
	
	public boolean isJump(){
		switch (this) {
			case JEQ:
			case JGEQ:
			case CALL:
			case CALL_PRI:
			case JUMP:
			case JUMP_PRI:
			case SWITCH:
			case JNZ:
			case JLEQ:
			case JNEQ:
			case JREL:
			case JZER:
			case JGRTR:
			case JLESS:
			case JSGEQ:
			case JSLEQ:
			case JSGRTR:
			case JSLESS:
				return true;
		}
		return false;
	}
	
	public boolean isCasetbl(){
		return this == CASETBL;
	}
}
