package ctrmap.scriptformats.gen6;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

public class PawnPrefixEntry {
	public Type type;
	public long[] data;
	
	public PawnPrefixEntry(int defsize, int cellSize, Type type, DataInput source) throws IOException{
		this.type = type;
		/*data = new long[defsize / cellSize];
		for (int i = 0; i < data.length; i++){
			data[i] = source.readInt();
		}*/
		if (defsize != cellSize + 4) {
			throw new RuntimeException("Unexpected defsize");
		}
		data = new long[2];
		data[0] = PawnInstruction.readCell(source, cellSize); //ucell address
		data[1] = source.readInt(); //nameofs
	}
	
	public PawnPrefixEntry(int defsize, Type type, long... data){
		this.type = type;
		if (data == null){
			this.data = new long[defsize];
		}
		else {
			this.data = data;
		}
	}
	
	public void write(DataOutput out, int cellSize) throws IOException{
		if (data.length == 2) {
			PawnInstruction.writeCell(out, cellSize, data[0]); //ucell address
			out.writeInt((int)data[1]); //nameofs
		}
		else {
			throw new RuntimeException();
		}
	}
	
	@Override
	public boolean equals(Object o){
		if (o != null && o instanceof PawnPrefixEntry){
			PawnPrefixEntry p = (PawnPrefixEntry)o;
			return Arrays.equals(p.data, data) && p.type == type;
		}
		return false;
	}
	
	public enum Type{
		PUBLIC,
		NATIVE,
		LIBRARY,
		PUBLIC_VAR,
		TAG,
		OVERLAY
	}
}
