package ctrmap.scriptformats.gen5.disasm;

import xstandard.io.base.impl.ext.data.DataIOStream;
import java.io.IOException;

public class LinkPrototype {	
	public int sourceOffset;
	public int targetOffset;
	
	public LinkPrototype(DataIOStream input, int value) throws IOException {
		sourceOffset = input.getPosition() - 4;
		targetOffset = value + input.getPosition();
	}
	
	public LinkPrototype(DataIOStream input) throws IOException{
		sourceOffset = input.getPosition();
		targetOffset = input.readInt() + input.getPosition();
	}
}
