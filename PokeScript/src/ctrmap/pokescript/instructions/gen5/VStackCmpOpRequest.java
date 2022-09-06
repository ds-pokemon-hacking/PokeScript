
package ctrmap.pokescript.instructions.gen5;

/**
 *
 */
public class VStackCmpOpRequest {
	public static final int GEQUAL = 0;
	public static final int NEQUAL = 1;
	public static final int LEQUAL = 2;
	public static final int GREATER = 3;
	public static final int LESS = 4;
	public static final int EQUAL = 5;
	public static final int BOOL_AND = 6;
	public static final int BOOL_OR = 7;

	public static String getStrOperator(int stkCmpReq){
		switch (stkCmpReq){
			case GEQUAL:
				return ">=";
			case NEQUAL:
				return "!=";
			case LESS:
				return "<";
			case LEQUAL:
				return "<=";
			case GREATER:
				return ">";
			case EQUAL:
				return "==";
			case BOOL_AND:
				return "&&";
			case BOOL_OR:
				return "||";
		}
		return "[ERR - " + stkCmpReq + "]";
	}
	
	public static int getVmCmpReqForStkCmpReq(int stkCmpReq){
		switch (stkCmpReq){
			case GEQUAL:
				return VCmpResultRequest.GEQUAL;
			case NEQUAL:
				return VCmpResultRequest.NEQUAL;
			case LESS:
				return VCmpResultRequest.LESS;
			case LEQUAL:
				return VCmpResultRequest.LEQUAL;
			case GREATER:
				return VCmpResultRequest.GREATER;
			case EQUAL:
				return VCmpResultRequest.EQUAL;
		}
		return stkCmpReq;
	}
}
