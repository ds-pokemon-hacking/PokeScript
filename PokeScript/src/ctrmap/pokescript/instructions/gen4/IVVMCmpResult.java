
package ctrmap.pokescript.instructions.gen4;

/**
 *
 */
public class IVVMCmpResult {
	//The conditions are left to right, i.e. CmpVarVar will return Var1 RESULT Var2
	public static final int LESS = 0;
	public static final int EQUAL = 1;
	public static final int GREATER = 2;
	public static final int LEQUAL = 3;
	public static final int GEQUAL = 4;
	public static final int NEQUAL = 5;
}
