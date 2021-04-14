package ctrmap.scriptformats.gen5;

import ctrmap.pokescript.instructions.ntr.NTRDataType;
import ctrmap.stdlib.formats.yaml.Key;
import ctrmap.stdlib.formats.yaml.Yaml;
import ctrmap.stdlib.formats.yaml.YamlListElement;
import ctrmap.stdlib.formats.yaml.YamlNode;
import ctrmap.stdlib.fs.FSFile;
import ctrmap.stdlib.fs.accessors.DiskFile;
import ctrmap.stdlib.io.base.LittleEndianIO;
import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class ScriptCmdDump {

	public static final int MAIN_CMD_TABLE_OFFS = 0x216B578;
	public static final int SCR_PLUGIN_INFO_ADDRESSES = 0x216C1EC;
	public static final int SCR_PLUGIN_INFO_COUNT = 17;
	public static final int MAIN_CMD_COUNT = 755;

	public static final int SUB_LOAD_VARADDR = 0x02154928;
	public static final int SUB_DEREF_VAR = 0x02154950;
	public static final int SUB_SCR_READ16 = 0x020159E8;
	public static final int SUB_SCR_READ32 = 0x02015A04;

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

	public static final int OFFS_BASE_OVL12 = 0x02150400;
	public static final int OFFS_BASE_PLUGINS = 0x021E5800;

	public static final int OVL_RAM_LOCATION = 0x0;
	public static final int PTR_TO_CMD_TABLE_IN_OVL = 0x0;
	public static final int PTR_TO_CMD_TABLE_IN_MAIN = 0x0;

	public static class OvlScrData {

		int cmdTableSrcOffs;
		int mapNoTableStart;
		int mapNoCount;
		int overlayNo;
		int overlayNo2;

		public OvlScrData(DataInput in) throws IOException {
			cmdTableSrcOffs = in.readInt();
			if (cmdTableSrcOffs != 0){
				cmdTableSrcOffs -= 0x40;
			}
			mapNoTableStart = in.readInt();
			mapNoCount = in.readInt();
			overlayNo = in.readInt();
			overlayNo2 = in.readInt();
		}
	}

	private static FSFile getOverlay(DiskFile ovlRoot, int ovlNo) {
		return ovlRoot.getChild(String.format("overlay_%04d.bin", ovlNo));
	}

	private static void dumpZonePluginTable() throws IOException {
		DiskFile decompressedOverlayRoot = new DiskFile("D:\\_REWorkspace\\pokescript_genv\\overlays_dec");
		LittleEndianIO io = getOverlay(decompressedOverlayRoot, 12).getIO();
		
		Yaml yml = new Yaml();

		List<OvlScrData> osd = new ArrayList<>();
		io.setBase(OFFS_BASE_OVL12);
		io.seek(SCR_PLUGIN_INFO_ADDRESSES);
		for (int i = 0; i < SCR_PLUGIN_INFO_COUNT; i++) {
			osd.add(new OvlScrData(io));
		}
		
		for (OvlScrData o : osd){
			if (o.cmdTableSrcOffs == 0){
				continue;
			}
			io.seek(o.mapNoTableStart);
			YamlNode node = new YamlNode(new Key("ovl" + o.overlayNo + (o.overlayNo2 != -1 ? ("_ovl" + o.overlayNo2) : "")));
			for (int i = 0; i < o.mapNoCount; i++){
				YamlNode elem = new YamlNode(new YamlListElement());
				elem.addChild(new YamlNode(String.valueOf(io.readShort())));
				node.addChild(elem);
			}
			yml.root.addChild(node);
		}
		yml.writeToFile(new DiskFile("D:\\_REWorkspace\\pokescript_genv\\decomp\\MapsPerPlugin.yml"));
	}

	public static void main(String[] args) {
		DiskFile decompressedOverlayRoot = new DiskFile("D:\\_REWorkspace\\pokescript_genv\\overlays_dec");

		try {
			dumpZonePluginTable();
			List<OvlScrData> osd = new ArrayList<>();

			LittleEndianIO io = getOverlay(decompressedOverlayRoot, 12).getIO();

			io.setBase(OFFS_BASE_OVL12);
			io.seek(SCR_PLUGIN_INFO_ADDRESSES);
			for (int i = 0; i < SCR_PLUGIN_INFO_COUNT; i++) {
				osd.add(new OvlScrData(io));
			}

			io.close();

			List<FuncData> funcs = new ArrayList<>();
			funcs.addAll(readFuncData(getOverlay(decompressedOverlayRoot, 12), OFFS_BASE_OVL12, MAIN_CMD_TABLE_OFFS, MAIN_CMD_TABLE_OFFS, MAIN_CMD_COUNT, -1, -1));
			for (OvlScrData d : osd) {
				if (d.mapNoTableStart != 0) {
					funcs.addAll(readFuncData(getOverlay(decompressedOverlayRoot, d.overlayNo), OFFS_BASE_PLUGINS, d.cmdTableSrcOffs, d.mapNoTableStart, -1, d.overlayNo, d.overlayNo2));
				}
			}

			Map<String, Yaml> ymls = new HashMap<>();
			for (FuncData fd : funcs) {
				String ymlKey = (fd.ovlNo != -1 ? ("ovl" + fd.ovlNo) : "") + (fd.ovl2No != -1 ? ("_ovl" + fd.ovl2No) : "");
				if (ymlKey.isEmpty()) {
					ymlKey = "base";
				}
				Yaml yml = ymls.get(ymlKey);
				if (yml == null) {
					yml = new Yaml();
					ymls.put(ymlKey, yml);
				}

				String nodeName = "0x" + Integer.toHexString(fd.cmd).toUpperCase();
				YamlNode fn = yml.getEnsureRootNodeKeyNode(nodeName);
				if (fn != null || fd.ovlNo != -1) {
					//fn = yml.getEnsureRootNodeKeyNode(nodeName + "_ovl" + fd.ovlNo + (fd.ovl2No == -1 ? "" : ("_ovl" + fd.ovl2No)));
				} else {
					//fn = yml.getEnsureRootNodeKeyNode(nodeName);
				}
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
				} else {
					if (!fd.exists) {
						fn.addChild(new YamlNode("Exists", "false"));
					}
					if (fd.outOfRange) {
						fn.addChild(new YamlNode("OutsideOfOvl12", "true"));
					}
					if (fd.badExit) {
						fn.addChild(new YamlNode("BadExit", "true"));
					}
				}
			}
			//yml.writeToFile(new DiskFile("D:\\_REWorkspace\\pokescript_genv\\decomp\\binfuncs.yml"));
			for (Map.Entry<String, Yaml> e : ymls.entrySet()) {
				e.getValue().writeToFile(new DiskFile("D:\\_REWorkspace\\pokescript_genv\\decomp\\" + e.getKey() + ".yml"));
			}

			io.close();
		} catch (IOException ex) {
			Logger.getLogger(ScriptCmdDump.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private static List<FuncData> readFuncData(FSFile bin, int binaryOffsetBase, int commandTableOffset, int commandTableTargetOffset, int commandCount, int ovlNo, int ovl2No) throws IOException {
		System.out.println("Reading function data " + bin);
		LittleEndianIO io = bin.getIO();

		io.setBase(binaryOffsetBase);

		io.seek(commandTableOffset);
		Integer[] commands;
		if (commandCount != -1) {
			commands = new Integer[commandCount];
			for (int i = 0; i < commandCount; i++) {
				commands[i] = io.readInt();
			}
		} else {
			List<Integer> cmdList = new ArrayList<>();
			while (true) {
				int cmd = io.readInt();
				if (cmd == -1) {
					break;
				} else {
					cmdList.add(cmd);
				}
			}
			commandCount = cmdList.size();
			commands = cmdList.toArray(new Integer[commandCount]);
		}

		List<FuncData> funcs = new ArrayList<>();

		int cmdNumReloc = (commandTableTargetOffset - MAIN_CMD_TABLE_OFFS) / 4;
		cmdNumReloc = cmdNumReloc > 0 ? 1000 : 0;

		int len = io.length() - 2 + binaryOffsetBase;
		for (int i = 0; i < commandCount; i++) {
			FuncData fd = new FuncData();
			fd.ovl2No = ovl2No;
			fd.cmd = i + cmdNumReloc;
			fd.ovlNo = ovlNo;

			if (commands[i] == 0) {
				fd.exists = false;
				funcs.add(fd);
				continue;
			}

			int tgt = commands[i];
			if ((tgt & 1) != 0) {
				tgt--;
			}
			io.seek(tgt);

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
				} else if ((opcode & BX_MASK) == BX) {
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
						int branchTargetAbs = branchTargetRel + io.getPosition();
						if (ovlNo != -1) {
							if ((branchTargetAbs & 0xFFFF0000) == 0x02150000) {
								branchTargetAbs += 0x40;
							}
						}

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
		return funcs;
	}

	private static class FuncData {

		public int ovlNo;
		public int ovl2No;
		int cmd;
		boolean exists = true;
		boolean outOfRange;
		boolean badExit = false;

		List<NTRDataType> args = new ArrayList<>();
	}
}
