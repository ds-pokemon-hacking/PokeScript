package ctrmap.scriptformats.gen5;

import ctrmap.pokescript.instructions.ntr.NTRDataType;
import ctrmap.stdlib.formats.yaml.Key;
import ctrmap.stdlib.formats.yaml.Yaml;
import ctrmap.stdlib.formats.yaml.YamlListElement;
import ctrmap.stdlib.formats.yaml.YamlNode;
import ctrmap.stdlib.fs.accessors.DiskFile;
import ctrmap.stdlib.io.base.LittleEndianIO;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class ScriptCmdDump {

	public static final int CMD_TABLE_OFFS = 0x1B178;
	public static final int CMD_COUNT = 755;

	public static final int SUB_LOAD_VARADDR = 0x154928;
	public static final int SUB_DEREF_VAR = 0x154950;
	public static final int SUB_SCR_READ16 = 0x0159E8;
	public static final int SUB_SCR_READ32 = 0x015A04;

	public static final int STR_PLUS0x20_BITS = 0b0110001000000000;
	public static final int STR_PLUS0x20_MASK = 0b1111111111000000;

	public static final int BX_LR = 0x4770;
	public static final int BX = 0x4700;
	public static final int BX_MASK = 0xFF00;

	public static final int POP_BITS = 0b1011110000000000;
	public static final int POP_MASK = 0b1111111000000000;

	public static final int BRANCH_LOW_BITS = (0b11111 << 11);
	public static final int BRANCH_HIGH_BITS = (0b11110 << 11);
	public static final int BRANCH_ANY_MASK = (0b11110 << 11);
	public static final int BRANCH_MASK = (0b11111 << 11);

	public static final int OFFS_BASE = 0x150400;
	public static final int OFFS_RELOCATOR = -OFFS_BASE;

	public static void main(String[] args) {
		try {
			LittleEndianIO io = new DiskFile("D:\\_REWorkspace\\pokescript_genv\\overlay_0012.bin").getIO();

			io.seek(CMD_TABLE_OFFS);
			int[] commands = new int[CMD_COUNT];
			for (int i = 0; i < CMD_COUNT; i++) {
				commands[i] = io.readInt();
			}

			List<FuncData> funcs = new ArrayList<>();

			int len = io.length() - 2;
			for (int i = 0; i < CMD_COUNT; i++) {
				if (commands[i] == 0) {
					FuncData fd = new FuncData();
					fd.cmd = i;
					fd.exists = false;
					funcs.add(fd);
					continue;
				}

				int tgt = commands[i] + OFFS_RELOCATOR - 0x02000000;
				if ((tgt & 1) != 0) {
					tgt--;
				}
				io.seek(tgt);

				FuncData fd = new FuncData();
				fd.cmd = i;

				if (io.getPosition() > len || io.readInt() == 0) {
					fd.outOfRange = true;
				}
				io.seek(io.getPosition() - 4);

				while (io.getPosition() < len) {
					int opcode = io.readShort();

					if (opcode == BX_LR) {
						break;
					} else if ((opcode & POP_MASK) == POP_BITS) {
						break;
					} else if ((opcode & BX_MASK) == BX){
						fd.badExit = true;
						break;
					} else {
						if ((opcode & STR_PLUS0x20_MASK) == STR_PLUS0x20_BITS) {
							fd.args.add(NTRDataType.U8);
						} else if ((opcode & BRANCH_ANY_MASK) == BRANCH_HIGH_BITS) {
							int branchHigh = opcode;
							int branchLow = io.readShort();
							if ((opcode & BRANCH_MASK) == BRANCH_LOW_BITS) {
								int temp = branchLow;
								branchLow = branchHigh;
								branchHigh = temp;
							}

							int branchTargetRel = (branchLow & 0x7FF) | ((branchHigh & 0x7FF) << 11);
							branchTargetRel = (branchTargetRel << 10 >> 10) * 2; //this should sign extend..?
							int branchTargetAbs = branchTargetRel + io.getPosition() + OFFS_BASE;

							switch (branchTargetAbs) {
								case SUB_DEREF_VAR:
									fd.args.add(NTRDataType.FLEX);
									break;
								case SUB_SCR_READ32:
									fd.args.add(NTRDataType.S32);
									break;
								case SUB_SCR_READ16:
									fd.args.add(NTRDataType.U16);
									break;
								case SUB_LOAD_VARADDR:
									fd.args.add(NTRDataType.VAR);
									break;
							}
						}
					}
				}
				funcs.add(fd);
			}

			Yaml yml = new Yaml();
			for (FuncData fd : funcs) {
				String nodeName = "0x" + Integer.toHexString(fd.cmd).toUpperCase();
				YamlNode fn = yml.getEnsureRootNodeKeyNode(nodeName);
				fn.addChild(new YamlNode("Name", "CMD_" + Integer.toHexString(fd.cmd).toUpperCase()));

				if (fd.exists && !fd.outOfRange && !fd.args.isEmpty()) {
					YamlNode params = new YamlNode(new Key("Parameters"));
					fn.addChild(params);

					for (NTRDataType a : fd.args) {
						YamlNode elem = new YamlNode(new YamlListElement());

						String name = null;
						switch (a) {
							case FLEX:
								name = "ushort";
								break;
							case S32:
								name = "int";
								break;
							case U8:
								name = "byte";
								break;
							case U16:
								name = "const ushort";
								break;
							case VAR:
								name = "ref ushort";
								break;
						}
						elem.addChild(new YamlNode(name));

						params.addChild(elem);
					}
				}
				else {
					if (!fd.exists){
						fn.addChild(new YamlNode("Exists", "false"));
					}
					if (fd.outOfRange){
						fn.addChild(new YamlNode("OutsideOfOvl12", "true"));
					}
					if (fd.badExit){
						fn.addChild(new YamlNode("BadExit", "true"));
					}
				}
			}
			yml.writeToFile(new DiskFile("D:\\_REWorkspace\\pokescript_genv\\decomp\\binfuncs.yml"));

			io.close();
		} catch (IOException ex) {
			Logger.getLogger(ScriptCmdDump.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private static class FuncData {

		int cmd;
		boolean exists = true;
		boolean outOfRange;
		boolean badExit = false;

		List<NTRDataType> args = new ArrayList<>();
	}
}
