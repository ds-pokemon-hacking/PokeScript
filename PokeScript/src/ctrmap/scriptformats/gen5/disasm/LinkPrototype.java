package ctrmap.scriptformats.gen5.disasm;

import ctrmap.stdlib.io.iface.PositionedDataInput;
import java.io.IOException;

public class LinkPrototype {	
	public int sourceOffset;
	public int targetOffset;
	
	public LinkPrototype(PositionedDataInput input, int value) throws IOException {
		sourceOffset = input.getPosition() - 4;
		targetOffset = value + input.getPosition();
	}
	
	public LinkPrototype(PositionedDataInput input) throws IOException{
		sourceOffset = input.getPosition();
		targetOffset = input.readInt() + input.getPosition();
	}
}
