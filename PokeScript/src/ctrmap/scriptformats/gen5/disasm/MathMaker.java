/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ctrmap.scriptformats.gen5.disasm;

/**
 *
 */
public class MathMaker {

	public static MathCommands handleInstruction(DisassembledCall call) {
		int opcode = call.definition.opCode;

		MathCommands cmd = MathCommands.valueOf(opcode);

		return cmd;
	}
}
