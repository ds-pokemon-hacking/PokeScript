package ctrmap.scriptformats.gen6;

import ctrmap.stdlib.io.LittleEndianDataInputStream;
import ctrmap.stdlib.io.LittleEndianDataOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class PawnPrefixEntry {
	public Type type;
	public int[] data;
	
	public PawnPrefixEntry(int defsize, Type type, LittleEndianDataInputStream source) throws IOException{
		this.type = type;
		data = new int[defsize / 4];
		for (int i = 0; i < data.length; i++){
			data[i] = source.readInt();
		}
	}
	
	public PawnPrefixEntry(int defsize, Type type, int[] data){
		this.type = type;
		if (data == null){
			this.data = new int[defsize];
		}
		else {
			this.data = data;
		}
	}
	
	public void write(LittleEndianDataOutputStream out) throws IOException{
		for (int i = 0; i < data.length; i++){
			out.writeInt(data[i]);
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
