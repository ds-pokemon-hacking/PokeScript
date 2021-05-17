package ctrmap.scriptformats.gen5;

import ctrmap.pokescript.instructions.gen5.VOpCode;
import ctrmap.pokescript.instructions.ntr.NTRArgument;
import ctrmap.pokescript.instructions.ntr.NTRDataType;
import ctrmap.pokescript.instructions.ntr.NTRInstructionPrototype;
import ctrmap.stdlib.formats.yaml.Yaml;
import ctrmap.stdlib.formats.yaml.YamlNode;
import ctrmap.stdlib.fs.FSFile;
import ctrmap.stdlib.fs.FSUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VCommandDataBase {

	private Map<Integer, VCommand> commands = new HashMap<>();

	public VCommandDataBase(FSFile fsf) {
		Yaml yml = new Yaml(fsf);
		
		String defPkg = FSUtil.getFileNameWithoutExtension(fsf.getName());

		for (YamlNode func : yml.root.children) {
			VCommand cmd = new VCommand(func, defPkg);
			commands.put(cmd.def.opCode, cmd);
		}
	}
	
	public VCommand getCommandProto(int opCode){
		return commands.get(opCode);
	}

	public static class VCommand {

		public String name;
		public NTRInstructionPrototype def;

		public CommandType type;
		public boolean isConditional;
		public boolean setsCmpFlag;
		
		public String[] methodNames;
		public String classPath;
		
		public VCommand(String name, NTRInstructionPrototype def, CommandType type, boolean isConditional, boolean setsCmpFlag){
			this.name = name;
			this.def = def;
			this.type = type;
			this.isConditional = isConditional;
			this.setsCmpFlag = setsCmpFlag;
		}

		public VCommand(YamlNode node, String defaultPackage) {
			int opCode = node.getKeyInt();
			name = node.getChildByName("Name").getValue();
			YamlNode paramsList = node.getChildByName("Parameters");
			YamlNode paramNamesNode = node.getChildByName("ParamNames");
			List<String> paramNames = new ArrayList<>();
			if (paramNamesNode != null) {
				paramNames.addAll(paramNamesNode.getChildValuesAsListStr());
			}
			YamlNode returnTypesNode = node.getChildByName("ReturnParams");
			List<String> returnables = new ArrayList<>();
			if (returnTypesNode != null){
				returnables.addAll(returnTypesNode.getChildValuesAsListStr());
			}
			
			YamlNode psNameNode = node.getChildByName("PSName");
			YamlNode psPkgNode = node.getChildByName("PSPackage");
			methodNames = (psNameNode != null ? psNameNode.getValue() : name).split("/");
			classPath = psPkgNode != null ? psPkgNode.getValue() : defaultPackage;

			List<NTRArgument> args = new ArrayList<>();
			
			if (paramsList != null) {
				int rcb = 0;
				for (int i = 0; i < paramsList.children.size(); i++) {
					YamlNode pn = paramsList.children.get(i);
					NTRArgument arg = new NTRArgument(parseNTRDT(pn.getValue()));
					if (i < paramNames.size() && returnables.contains(paramNames.get(i))){
						arg.returnCallBackIndex = rcb;
						rcb++;
					}
					args.add(arg);
				}
			}
			def = new NTRInstructionPrototype(opCode, args.toArray(new NTRArgument[args.size()]));
			
			type = CommandType.REGULAR;
			
			boolean isEnd = node.getChildBoolValue("IsEnd");
			boolean isScrEnd = node.getChildBoolValue("IsScriptEnd");
			boolean isMove = node.getChildBoolValue("HasMovement");
			boolean isFunc = node.getChildBoolValue("HasFunction");
			isConditional = node.getChildBoolValue("HasCondition");
			setsCmpFlag = node.getChildBoolValue("WritesCondition");
			boolean isJump = node.getChildBoolValue("IsJump");
						
			if (isMove){
				type = CommandType.MOVEMENT_JUMP;
			}
			else if (isEnd){
				type = isScrEnd ? CommandType.HALT : CommandType.RETURN;
			}
			else if (isFunc){
				type = isJump ? CommandType.JUMP : CommandType.CALL;
			}
		}
		
		public boolean isDecompPrintable(){
			return !(setsCmpFlag || def.opCode == VOpCode.CmpPriAlt.ordinal());
		}
		
		public String getPKSCallName(){
			return FSUtil.getFileName(classPath.replace('.', '/')) + "." + methodNames[0];
		}
		
		public boolean isBranchEnd(){
			return type == CommandType.HALT || type == CommandType.RETURN || (type == CommandType.JUMP && !isConditional);
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
		MOVEMENT_JUMP
	}
}
