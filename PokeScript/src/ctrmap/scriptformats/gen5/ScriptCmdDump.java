package ctrmap.scriptformats.gen5;

import ctrmap.pokescript.instructions.ntr.NTRDataType;
import xstandard.formats.yaml.Key;
import xstandard.formats.yaml.Yaml;
import xstandard.formats.yaml.YamlListElement;
import xstandard.formats.yaml.YamlNode;
import xstandard.fs.FSFile;
import xstandard.fs.accessors.DiskFile;
import xstandard.io.base.impl.ext.data.DataIOStream;
import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import xstandard.fs.FSUtil;
import xstandard.io.base.iface.ReadableStream;
import xstandard.io.base.impl.ext.data.DataInStream;

/**
 *
 */
public class ScriptCmdDump {

	public static final int STR_PLUS0x20_BITS = 0b0110001000000000;
	public static final int STR_PLUS0x14_BITS = 0b0110000101000000;
	public static final int STR_MASK = 0b1111111111000000;

	public static final int BX_LR = 0x4770;
	public static final int BX = 0x4700;
	public static final int BX_MASK = 0xFF00;

	public static final int POP_BITS = 0b1011110000000000;
	public static final int POP_MASK = 0b1111111000000000;

	public static final int BRANCH_LOW_BITS = (0b11111 << 11);
	public static final int BRANCH_HIGH_BITS = (0b11110 << 11);
	public static final int BRANCH_ANY_MASK = (0b11110 << 11);
	public static final int BRANCH_MASK = (0b11111 << 11);

	public static final int OFFS_BASE_PLUGINS = 0x021E5800;

	public static final Config CONFIG_W2U = new Config(
		new ReadRoutines(
			0x020159E8,
			0x02154928,
			0x02154950,
			0x02015A04,
			false
		),
		new OverlayInfo(12),
		new CommandSetInfo(0x216B578, 755),
		0x216C1EC,
		17,
		new OverlayInfo(36)
	);
	
	public static final Config CONFIG_W1U = new Config(
		new ReadRoutines(
			0x02011330,
			0x02159B08,
			0x02159B30,
			0x0201134C,
			true
		),
		new OverlayInfo(10),
		new CommandSetInfo(0x217064C, 609),
		new OverlayInfo(21)
	);

	public static class Config {

		public final ReadRoutines routines;

		public final OverlayInfo mainOverlay;
		public final CommandSetInfo mainCommandSet;

		public final int pluginInfoOffset;
		public final int pluginCount;

		public OverlayInfo[] additionalResidentOverlays;

		public Config(ReadRoutines routines, OverlayInfo mainOverlay, CommandSetInfo mainCommandSet, OverlayInfo... additionalResidentOverlays) {
			this(routines, mainOverlay, mainCommandSet, -1, 0, additionalResidentOverlays);
		}

		public Config(ReadRoutines routines, OverlayInfo mainOverlay, CommandSetInfo mainCommandSet, int pluginInfoOffset, int pluginCount, OverlayInfo... additionalResidentOverlays) {
			this.routines = routines;
			this.mainOverlay = mainOverlay;
			this.mainCommandSet = mainCommandSet;
			this.pluginInfoOffset = pluginInfoOffset;
			this.pluginCount = pluginCount;
			this.additionalResidentOverlays = additionalResidentOverlays;
		}
	}

	public static class OverlayInfo {

		public final int id;
		private int baseAddress;

		public OverlayInfo(int id) {
			this.id = id;
		}
	}

	public static class CommandSetInfo {

		public final int cmdTableOffs;
		public final int cmdCount;
		public final int idBase;

		public CommandSetInfo(int cmdTableOffs, int cmdCount) {
			this(cmdTableOffs, cmdCount, 0);
		}

		public CommandSetInfo(int cmdTableOffs, int cmdCount, int idBase) {
			this.cmdTableOffs = cmdTableOffs;
			this.cmdCount = cmdCount;
			this.idBase = idBase;
		}
	}

	public static class ReadRoutines {

		public final int read16Offs;
		public final int readVarOffs;
		public final int readAnyOffs;
		public final int read32Offs;
		public final boolean read8Is0x14;

		public ReadRoutines(int read16, int readVar, int readAny, int read32, boolean read8Is0x14) {
			this.read16Offs = read16;
			this.readVarOffs = readVar;
			this.readAnyOffs = readAny;
			this.read32Offs = read32;
			this.read8Is0x14 = read8Is0x14;
		}
	}

	public static class OvlScrData {

		int cmdTableSrcOffs;
		int mapNoTableStart;
		int mapNoCount;
		int overlayNo;
		int overlayNo2;

		public OvlScrData(DataInput in) throws IOException {
			cmdTableSrcOffs = in.readInt();
			if (cmdTableSrcOffs != 0) {
				cmdTableSrcOffs -= 0x40;
			}
			mapNoTableStart = in.readInt();
			mapNoCount = in.readInt();
			overlayNo = in.readInt();
			overlayNo2 = in.readInt();
		}
	}

	private static FSFile getOverlay(FSFile ovlRoot, int ovlNo) {
		return ovlRoot.getChild(String.format("overlay_%04d.bin", ovlNo));
	}

	private static void dumpZonePluginTable(FSFile outRoot, FSFile ovlRoot, Config cfg) throws IOException {
		if (cfg.pluginCount == 0) {
			return;
		}
		DataIOStream io = getOverlay(ovlRoot, cfg.mainOverlay.id).getDataIOStream();

		Yaml yml = new Yaml();

		List<OvlScrData> osd = new ArrayList<>();
		io.setBase(cfg.mainOverlay.baseAddress);
		io.seek(cfg.pluginInfoOffset);
		for (int i = 0; i < cfg.pluginCount; i++) {
			osd.add(new OvlScrData(io));
		}

		for (OvlScrData o : osd) {
			if (o.cmdTableSrcOffs == 0) {
				continue;
			}
			io.seek(o.mapNoTableStart);
			YamlNode node = new YamlNode(new Key("ovl" + o.overlayNo + (o.overlayNo2 != -1 ? ("_ovl" + o.overlayNo2) : "")));
			for (int i = 0; i < o.mapNoCount; i++) {
				YamlNode elem = new YamlNode(new YamlListElement());
				elem.addChild(new YamlNode(String.valueOf(io.readShort())));
				node.addChild(elem);
			}
			yml.root.addChild(node);
		}
		yml.writeToFile(outRoot.getChild("MapsPerPlugin.yml"));
	}

	private static DataIOStream ramInit() {
		DataIOStream io = new DataIOStream(new byte[0x200000]); //2 MiB
		io.setBase(0x02000000);
		return io;
	}

	private static void ramLoadOverlay(DataIOStream ram, OverlayInfo info, FSFile ovlRoot) throws IOException {
		ram.seek(info.baseAddress);
		ReadableStream in = getOverlay(ovlRoot, info.id).getInputStream();
		FSUtil.transferStreams(in, ram);
		in.close();
	}
	
	private static int getOvlBaseAddr(FSFile ovlTable, int ovlId) {
		try {
			DataInStream in = ovlTable.getDataInputStream();
			in.seekNext(ovlId * 0x20 + 4);
			int addr = in.readInt();
			in.close();
			return addr;
		} catch (IOException ex) {
			Logger.getLogger(ScriptCmdDump.class.getName()).log(Level.SEVERE, null, ex);
		}
		return -1;
	}
	
	private static void resolveOverlay(OverlayInfo info, FSFile ovlTable) {
		info.baseAddress = getOvlBaseAddr(ovlTable, info.id);
	}

	public static void main(String[] args) {
		Config cfg = CONFIG_W1U;
		FSFile outRoot = new DiskFile("D:\\_REWorkspace\\pokescript_genv\\dump_w1");
		FSFile ovlRoot = new DiskFile("D:\\_REWorkspace\\pokescript_genv\\dump_w1\\overlay");
		FSFile ovlTable = new DiskFile("D:\\_REWorkspace\\pokescript_genv\\dump_w1\\y9.bin");
		DataIOStream ram = ramInit();

		try {
			dumpZonePluginTable(outRoot, ovlRoot, cfg);
			List<OvlScrData> osd = new ArrayList<>();
			
			resolveOverlay(cfg.mainOverlay, ovlTable);

			ramLoadOverlay(ram, cfg.mainOverlay, ovlRoot);
			for (OverlayInfo additional : cfg.additionalResidentOverlays) {
				resolveOverlay(additional, ovlTable);
				ramLoadOverlay(ram, additional, ovlRoot);
			}

			if (cfg.pluginCount != 0) {
				ram.seek(cfg.pluginInfoOffset);
				for (int i = 0; i < cfg.pluginInfoOffset; i++) {
					osd.add(new OvlScrData(ram));
				}

				ram.close();
			}

			List<FuncData> funcs = new ArrayList<>();
			funcs.addAll(readFuncData(ram, cfg.mainCommandSet, cfg.routines));
			for (OvlScrData d : osd) {
				if (d.mapNoTableStart != 0) {
					CommandSetInfo cmdSet = new CommandSetInfo(d.cmdTableSrcOffs, -1);
					OverlayInfo[] ovlInfos = new OverlayInfo[2];
					ovlInfos[0] = new OverlayInfo(d.overlayNo);
					ovlInfos[0] = new OverlayInfo(d.overlayNo2);
					for (OverlayInfo i : ovlInfos) {
						if (i.id != -1) {
							resolveOverlay(i, ovlTable);
							ramLoadOverlay(ram, i, ovlRoot);
						}
					}
					funcs.addAll(readFuncData(ram, cmdSet, cfg.routines, ovlInfos));
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
				e.getValue().writeToFile(outRoot.getChild(e.getKey() + ".yml"));
			}

			ram.close();
		} catch (IOException ex) {
			Logger.getLogger(ScriptCmdDump.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private static List<FuncData> readFuncData(
		DataIOStream ram,
		CommandSetInfo cmdSet,
		ReadRoutines routines,
		OverlayInfo... overlays
	) throws IOException {
		System.out.print("Reading function data ");
		if (overlays.length > 0) {
			System.out.print(String.valueOf(overlays[0].id));
		}
		System.out.println();

		ram.seek(cmdSet.cmdTableOffs);
		Integer[] commands;
		int commandCount = cmdSet.cmdCount;
		if (commandCount != -1) {
			commands = new Integer[commandCount];
			for (int i = 0; i < commandCount; i++) {
				commands[i] = ram.readInt();
			}
		} else {
			List<Integer> cmdList = new ArrayList<>();
			while (true) {
				int cmd = ram.readInt();
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

		int len = ram.getLength() - 2;
		for (int i = 0; i < commandCount; i++) {
			FuncData fd = new FuncData();
			fd.cmd = i + cmdSet.idBase;
			for (int j = 0; j < 2; j++) {
				int id = -1;
				if (j < overlays.length) {
					id = overlays[j].id;
				}
				if (j == 0) {
					fd.ovlNo = id;
				} else {
					fd.ovl2No = id;
				}
			}

			if (commands[i] == 0) {
				fd.exists = false;
				funcs.add(fd);
				continue;
			}

			int tgt = commands[i] & 0xFFFFFFFE; //discard Thumb bit
			ram.seek(tgt);

			if (ram.getPosition() > len || ram.readInt() == 0) {
				fd.outOfRange = true;
			}
			ram.seek(ram.getPosition() - 4);

			int read8Bits = routines.read8Is0x14 ? STR_PLUS0x14_BITS : STR_PLUS0x20_BITS;
			
			while (ram.getPosition() < len) {
				int opcode = ram.readUnsignedShort();

				if (opcode == BX_LR) {
					break;
				} else if ((opcode & POP_MASK) == POP_BITS) {
					break;
				} else if ((opcode & BX_MASK) == BX) {
					fd.badExit = true;
					break;
				} else {
					if ((opcode & STR_MASK) == read8Bits) {
						fd.args.add(NTRDataType.U8);
					} else if ((opcode & BRANCH_ANY_MASK) == BRANCH_HIGH_BITS) {
						int branchHigh = opcode;
						int branchLow = ram.readShort();
						if ((opcode & BRANCH_MASK) == BRANCH_LOW_BITS) {
							int temp = branchLow;
							branchLow = branchHigh;
							branchHigh = temp;
						}

						int branchTargetRel = (branchLow & 0x7FF) | ((branchHigh & 0x7FF) << 11);
						branchTargetRel = (branchTargetRel << 10 >> 10) * 2; //this should sign extend..?
						int branchTargetAbs = branchTargetRel + ram.getPosition();
						/*if (ovlNo != -1) {
							//leftover from B2 overlays being used in W2
							if ((branchTargetAbs & 0xFFFF0000) == 0x02150000) {
								branchTargetAbs += 0x40;
							}
						}*/

						NTRDataType type = null;
						if (branchTargetAbs == routines.read16Offs) {
							type = NTRDataType.U16;
						} else if (branchTargetAbs == routines.read32Offs) {
							type = NTRDataType.S32;
						} else if (branchTargetAbs == routines.readAnyOffs) {
							type = NTRDataType.FLEX;
						} else if (branchTargetAbs == routines.readVarOffs) {
							type = NTRDataType.VAR;
						}

						if (type != null) {
							fd.args.add(type);
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
