package ctrmap.scriptformats.gen5;

import ctrmap.pokescript.instructions.gen5.VOpCode;
import ctrmap.pokescript.instructions.ntr.NTRArgument;
import ctrmap.pokescript.instructions.ntr.NTRDataType;
import ctrmap.pokescript.instructions.ntr.NTRInstructionPrototype;
import ctrmap.stdlib.formats.yaml.Yaml;
import ctrmap.stdlib.formats.yaml.YamlNode;
import ctrmap.stdlib.fs.FSFile;
import ctrmap.stdlib.fs.FSUtil;
import ctrmap.stdlib.text.FormattingUtils;
import ctrmap.stdlib.text.StringEx;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VCommandDataBase {

	private Map<Integer, VCommand> commands = new HashMap<>();

	public VCommandDataBase(FSFile fsf) {
		Yaml yml = new Yaml(fsf);

		String defPkg = FormattingUtils.getStrWithoutNonAlphanumeric(StringEx.deleteAllChars(FSUtil.getFileNameWithoutExtension(fsf.getName()), ' '));

		for (YamlNode func : yml.root.children) {
			VCommand cmd = new VCommand(func, defPkg);
			commands.put(cmd.def.opCode, cmd);
		}
	}

	public VCommandDataBase(List<VCommandDataBase> sources) {
		for (VCommandDataBase db : sources) {
			if (db != null) {
				commands.putAll(db.commands);
			}
		}
	}
	
	public int getCommandMax() {
		int max = 0;
		for (VCommand c : commands.values()) {
			if (c.def.opCode > max) {
				max = c.def.opCode;
			}
		}
		return max;
	}

	public VCommand getCommandProto(int opCode) {
		return commands.get(opCode);
	}

	public static class VCommand {

		public String name;
		public NTRInstructionPrototype def;

		public CommandType type;
		public boolean callsExtern;
		public boolean readsCmpFlag;
		public boolean writesCmpFlag;

		public String[] methodNames;
		public String classPath;
		public String[] paramNames;

		public VCommand(String name, NTRInstructionPrototype def, CommandType type, boolean isConditional, boolean setsCmpFlag) {
			this.name = name;
			this.def = def;
			this.type = type;
			this.readsCmpFlag = isConditional;
			this.writesCmpFlag = setsCmpFlag;
		}

		public VCommand(YamlNode node, String defaultPackage) {
			int opCode = node.getKeyInt();
			name = node.getChildByName("Name").getValue();

			YamlNode paramsList = node.getChildByName("Parameters");

			YamlNode psNameNode = node.getChildByName("PSName");
			YamlNode psPkgNode = node.getChildByName("PSPackage");
			methodNames = (psNameNode != null ? psNameNode.getValue() : name).split("/");
			classPath = psPkgNode != null ? psPkgNode.getValue() : defaultPackage;

			List<NTRArgument> args = new ArrayList<>();
			List<String> argNames = new ArrayList<>();

			if (paramsList != null) {
				int rcb = 0;
				for (int i = 0; i < paramsList.children.size(); i++) {
					YamlNode pn = paramsList.children.get(i);

					NTRArgument arg = new NTRArgument(parseNTRDT(pn.getChildValue("Type")));
					if (pn.getChildBoolValue("IsReturn")) {
						arg.returnCallBackIndex = rcb;
						rcb++;
					}
					argNames.add(pn.getChildValue("Name"));
					args.add(arg);
				}
			}
			paramNames = argNames.toArray(new String[argNames.size()]);
			
			def = new NTRInstructionPrototype(opCode, args.toArray(new NTRArgument[args.size()]));

			type = CommandType.REGULAR;

			readsCmpFlag = node.getChildBoolValue("HasCondition");
			writesCmpFlag = node.getChildBoolValue("WritesCondition");
			callsExtern = node.getChildBoolValue("ExternCall");

			String cmdType = node.getChildValue("CommandType");

			if (cmdType != null) {
				switch (cmdType) {
					case "Jump":
						type = CommandType.JUMP;
						break;
					case "End":
						type = CommandType.HALT;
						break;
					case "Return":
						type = CommandType.RETURN;
						break;
					case "Call":
						type = CommandType.CALL;
						break;
					case "CallActionSeq":
						type = CommandType.ACTION_JUMP;
						break;
				}
			}
		}

		public boolean isDecompPrintable() {
			return !(writesCmpFlag || def.opCode == VOpCode.CmpPriAlt.ordinal());
		}

		public String getPKSCallName() {
			return FSUtil.getFileName(classPath.replace('.', '/')) + "." + methodNames[0];
		}
		
		public String getClassName() {
			int idx = classPath.lastIndexOf('.');
			return classPath.substring(idx + 1, classPath.length());
		}
		
		public String getPackageName() {
			if (classPath.contains(".")) {
				return classPath.substring(0, classPath.lastIndexOf('.'));
			}
			return null;
		}

		public boolean isBranchEnd() {
			return type == CommandType.HALT || type == CommandType.RETURN || (type == CommandType.JUMP && !readsCmpFlag);
		}

		private static NTRDataType parseNTRDT(String csharpType) {
			NTRDataType type = NTRDataType.FLEX;
			switch (csharpType) {
				case "const ushort":
					type = NTRDataType.U16;
					break;
				case "ushort":
					type = NTRDataType.FLEX;
					break;
				case "int":
					type = NTRDataType.S32;
					break;
				case "byte":
					type = NTRDataType.U8;
					break;
				case "bool":
					type = NTRDataType.BOOL;
					break;
				case "ref ushort":
					type = NTRDataType.VAR;
					break;
				case "fx16":
					type = NTRDataType.FX16;
					break;
				case "fx32":
					type = NTRDataType.FX32;
					break;
			}
			return type;
		}
	}

	public static enum CommandType {
		REGULAR,
		HALT,
		CALL,
		RETURN,
		JUMP,
		ACTION_JUMP
	}
}
