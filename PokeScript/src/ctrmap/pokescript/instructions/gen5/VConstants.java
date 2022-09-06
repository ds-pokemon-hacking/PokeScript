
package ctrmap.pokescript.instructions.gen5;

/**
 *
 */
public class VConstants {
	public static int WKVAL_START = 0x4000;
	public static int WKVAL_GP_START = 0x8000;
	public static int WKVAL_END = 0xC000;
	
	public static int GP_REG_PRI = 0x8000;
	public static int GP_REG_ALT = 0x8001;
	public static int GP_REG_3 = 0x8002;
	
	public static int VAR_START_LOCAL = 0x8003;
	public static int VAR_TOP_GLOBAL = 0x8054;
	
	public static boolean isWk(int value) {
		return value >= WKVAL_START && value < WKVAL_END;
	}
	
	public static boolean isLowWk(int value) {
		return value >= WKVAL_START && value < WKVAL_GP_START;
	}
	
	public static boolean isHighWk(int value) {
		return value >= WKVAL_GP_START && value < WKVAL_END;
	}
}
