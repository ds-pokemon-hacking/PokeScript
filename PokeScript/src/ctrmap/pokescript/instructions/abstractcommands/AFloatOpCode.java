package ctrmap.pokescript.instructions.abstractcommands;

import static ctrmap.pokescript.instructions.abstractcommands.APlainOpCode.*;

public enum AFloatOpCode {
	//Floating point conversion
	VCONST_PRI(CONST_PRI),		//Loads a compile-time Java float value to the primary register in a platform-defined format
	VCVT_TOFLOAT(null),			//Converts the integer value of the primary register to a platform-defined float representation and stores it back into the register
	VCVT_FROMFLOAT(null),		//Rounds the value in the primary register to zero and stores it as an integer back into the register

	//Floating point math
	VNEGATE(NEGATE),			//fPRI = -fPRI
	VADD(ADD),					//fPRI = fPRI + fALT
	VSUB(SUBTRACT),				//fPRI = fPRI - fALT
	VMULTIPLY(MULTIPLY),		//fPRI = fPRI * fALT
	VDIVIDE(DIVIDE),			//fPRI = fPRI / fALT
	VMODULO(MODULO),			//fALT = fPRI % fALT

	//Floating point comparison
	VEQUAL(EQUAL),				//PRI = (fPRI == fALT) ? 1 : 0
	VNEQUAL(NEQUAL),			//PRI = (fPRI != fALT) ? 1 : 0
	VLEQUAL(LEQUAL),			//PRI = (fPRI <= fALT) ? 1 : 0
	VGEQUAL(GEQUAL),			//PRI = (fPRI >= fALT) ? 1 : 0
	VLESS(LESS),				//PRI = (fPRI <  fALT) ? 1 : 0
	VGREATER(GREATER);			//PRI = (fPRI >  fALT) ? 1 : 0

	public APlainOpCode integerEquivalent;

	private AFloatOpCode(APlainOpCode intOpcode) {
		this.integerEquivalent = intOpcode;
	}
}
