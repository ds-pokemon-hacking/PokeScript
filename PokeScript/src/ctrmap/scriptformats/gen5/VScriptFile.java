
package ctrmap.scriptformats.gen5;

import ctrmap.pokescript.instructions.ntr.NTRInstructionCall;
import ctrmap.pokescript.instructions.ntr.NTRInstructionLink;
import ctrmap.stdlib.io.LittleEndianDataOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VScriptFile {
	public static final int V_SCR_MAGIC = 0xFD13;
	
	public List<NTRInstructionLink> publics = new ArrayList<>();
	public List<NTRInstructionCall> instructions = new ArrayList<>();
	
	public void updatePtrs(){
		int ptr = publics.size() * Integer.BYTES + Short.BYTES;
		for (NTRInstructionCall call : instructions){
			call.pointer = ptr;
			ptr += call.getSize();
		}
	}
	
	public void updateLinks(){
		updatePtrs();
		for (NTRInstructionCall call : instructions){
			if (call.link != null){
				call.link.updateSourceArg();
			}
		}
	}
	
	public void addPublic(NTRInstructionCall firstInstruction){
		publics.add(new NTRInstructionLink(null, firstInstruction, -1));
	}
	
	public NTRInstructionCall getInstructionByPtr(int ptr){
		for (NTRInstructionCall ins : instructions){
			if (ins.pointer == ptr){
				return ins;
			}
		}
		return null;
	}
	
	public byte[] getBinaryData(){
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			LittleEndianDataOutputStream dos = new LittleEndianDataOutputStream(out);
			
			for (NTRInstructionLink publicMethod : publics){
				dos.writeInt(publicMethod.target.pointer - (dos.getPosition() + 4));
			}
			
			dos.writeShort(V_SCR_MAGIC);
			
			for (NTRInstructionCall call : instructions){
				call.write(dos);
			}
			
			dos.close();
			return out.toByteArray();
		} catch (IOException ex) {
			Logger.getLogger(VScriptFile.class.getName()).log(Level.SEVERE, null, ex);
		}
		return null;
	}
	
	public String getASCII(){
		StringBuilder sb = new StringBuilder();
		
		for (NTRInstructionCall call : instructions){
			sb.append(call.toString());
			sb.append("\n");
		}
		
		return sb.toString();
	}
}
