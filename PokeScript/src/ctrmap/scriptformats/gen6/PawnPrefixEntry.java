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
		data = new long[defsize / cellSize];
		for (int i = 0; i < data.length; i++){
			data[i] = source.readInt();
		}
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
		for (int i = 0; i < data.length; i++){
			PawnInstruction.writeCell(out, cellSize, data[i]);
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
