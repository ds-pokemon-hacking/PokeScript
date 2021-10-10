package ctrmap.scriptformats.gen5.disasm;

import java.util.ArrayList;
import java.util.List;

public class DisassembledMethod {
	public boolean isPublic = false;
	public int ptr;
	
	public List<DisassembledCall> instructions = new ArrayList<>();
	
	public DisassembledMethod(int ptr){
		this.ptr = ptr;
	}
	
	public String getName(){
		if (instructions.isEmpty()) {
			return "nullsub_" + Integer.toHexString(ptr);
		}
		return instructions.get(0).label;
	}
}
