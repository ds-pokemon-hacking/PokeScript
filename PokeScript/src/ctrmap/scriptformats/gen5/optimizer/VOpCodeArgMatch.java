package ctrmap.scriptformats.gen5.optimizer;

import ctrmap.pokescript.instructions.gen5.VConstants;

public enum VOpCodeArgMatch {
	PRIMARY_GPR(VConstants.GP_REG_PRI, CmpType.EQUAL),
	ALTERNATIVE_GPR(VConstants.GP_REG_ALT, CmpType.EQUAL),
	TERTIARY_GPR(VConstants.GP_REG_3, CmpType.EQUAL),
	ANY_GPR(new int[]{VConstants.GP_REG_PRI, VConstants.GP_REG_3}, new CmpType[]{CmpType.GEQUAL, CmpType.LEQUAL}),

	VARIABLE(VConstants.GP_REG_PRI, CmpType.GEQUAL),
	CONSTANT(VConstants.WKVAL_START, CmpType.LESS),
	ZERO(0, CmpType.EQUAL),
	ONE(1, CmpType.EQUAL),
	ANY(-1, CmpType.NOTEQUAL);

	public final int[] checkValues;
	public final CmpType[] cmps;

	private VOpCodeArgMatch(int numericValue, CmpType cmp) {
		this.checkValues = new int[]{numericValue};
		this.cmps = new CmpType[]{cmp};
	}

	private VOpCodeArgMatch(int[] numericValues, CmpType[] cmps) {
		this.checkValues = numericValues;
		this.cmps = cmps;
	}

	public boolean matches(int test) {
		boolean b = true;
		for (int i = 0; i < cmps.length; i++) {
			int checkValue = checkValues[i];
			CmpType cmp = cmps[i];
			switch (cmp) {
				case EQUAL:
					b &= test == checkValue;
					break;
				case GEQUAL:
					b &= test >= checkValue;
					break;
				case GREATER:
					b &= test > checkValue;
					break;
				case LEQUAL:
					b &= test <= checkValue;
					break;
				case LESS:
					b &= test < checkValue;
					break;
				case NOTEQUAL:
					b &= test != checkValue;
					break;
			}
			if (!b){
				break;
			}
		}
		return b;
	}

	public enum CmpType {
		EQUAL,
		GREATER,
		LESS,
		NOTEQUAL,
		GEQUAL,
		LEQUAL
	}
}
