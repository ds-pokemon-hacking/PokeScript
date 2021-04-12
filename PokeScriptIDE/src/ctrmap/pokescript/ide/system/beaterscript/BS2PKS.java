package ctrmap.pokescript.ide.system.beaterscript;

import ctrmap.pokescript.LangConstants;
import ctrmap.pokescript.instructions.ntr.NTRArgument;
import ctrmap.pokescript.instructions.ntr.NTRDataType;
import ctrmap.pokescript.types.DataType;
import ctrmap.stdlib.formats.yaml.Key;
import ctrmap.stdlib.formats.yaml.Yaml;
import ctrmap.stdlib.formats.yaml.YamlNode;
import ctrmap.stdlib.cli.ArgumentBuilder;
import ctrmap.stdlib.cli.ArgumentPattern;
import ctrmap.stdlib.cli.ArgumentType;
import ctrmap.stdlib.fs.FSFile;
import ctrmap.stdlib.fs.FSUtil;
import ctrmap.stdlib.fs.accessors.DiskFile;
import ctrmap.stdlib.gui.FormattingUtils;
import ctrmap.stdlib.net.FileDownloader;
import ctrmap.stdlib.util.ArraysEx;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class BS2PKS {

	public static final String BSYML_URL_FORMAT = "https://raw.githubusercontent.com/PlatinumMaster/BeaterScript/master/%s.yml";

	private static final ArgumentPattern[] BS2PKS_ARGS = new ArgumentPattern[]{
		new ArgumentPattern("input", "An input file or the name of the source YAML in BeaterScript.", ArgumentType.STRING, "C:\\Users\\Čeněk\\eclipse-workspace\\BsYmlGen\\B2W2.yml", "-i, --input"),
		new ArgumentPattern("root", "A root directory for the output packages.", ArgumentType.STRING, "include", "-r, --output-root")
	};

	public static void main(String[] args) {
		ArgumentBuilder bld = new ArgumentBuilder(BS2PKS_ARGS);
		bld.parse(args);

		File incRoot = new File(bld.getContent("root").stringValue());
		String input = bld.getContent("input").stringValue();

		Yaml yaml = null;
		File inFile = new File(input);
		if (!inFile.exists()) {
			String url = String.format(BSYML_URL_FORMAT, input);

			yaml = new Yaml(FileDownloader.getNetworkStream(url), FSUtil.getFileName(url));
		} else {
			yaml = new Yaml(new DiskFile(inFile));
		}

		makePKSIncludes(yaml, incRoot);
	}

	public static void makePKSIncludes(Yaml beaterScriptYml, File includesRoot) {
		DiskFile irdf = new DiskFile(includesRoot);

		List<BSFunc> funcs = new ArrayList<>();

		/*
		Read the YAML into a function list
		 */
		for (YamlNode funcNode : beaterScriptYml.root.children) {
			int op = funcNode.getKeyInt();

			String name = funcNode.getChildByName("Name").getValue();
			String className = FSUtil.getFileNameWithoutExtension(beaterScriptYml.documentName);
			List<String> paramTypes = new ArrayList<>();
			List<String> paramNames = new ArrayList<>();

			YamlNode params = funcNode.getChildByName("Parameters");
			if (params != null) {
				for (YamlNode param : params.children) {
					paramTypes.add(param.getValue());
				}
			}

			YamlNode paramNamesNode = funcNode.getChildByName("ParamNames");
			if (paramNamesNode != null) {
				for (YamlNode paramName : paramNamesNode.children) {
					paramNames.add(paramName.getValue());
				}
			}

			List<String> returnParamNames = new ArrayList<>();
			List<NTRDataType> pksReturnTypes = new ArrayList<>();
			YamlNode returnParams = funcNode.getChildByName("ReturnParams");
			if (returnParams != null) {
				for (YamlNode ch : returnParams.children) {
					returnParamNames.add(ch.getValue());
				}
			}
			YamlNode returnParamTypes = funcNode.getChildByName("ReturnTypes");
			if (returnParamTypes != null) {
				for (YamlNode ch : returnParamTypes.children) {
					pksReturnTypes.add(parseNTRDT(ch.getValue()));
				}
			}

			YamlNode pksName = funcNode.getChildByName("PSName");
			if (pksName != null) {
				name = pksName.getValue();
			}

			YamlNode pksPackageAndClass = funcNode.getChildByName("PSPackage");
			if (pksPackageAndClass != null) {
				className = pksPackageAndClass.getValue();
			}

			BSFunc f = new BSFunc();
			f.opCode = op;
			if (name == null) {
				name = "CMD_" + Integer.toHexString(op).toUpperCase();
			}
			f.names = name.split("/");
			f.packageAndClass = className;
			f.returnTypes = pksReturnTypes;
			f.returnArgNames = returnParamNames;
			f.args = new NTRArgument[paramTypes.size()];
			f.argNames = new String[paramTypes.size()];
			for (int i = 0; i < paramTypes.size(); i++) {
				if (i < paramNames.size()) {
					f.argNames[i] = paramNames.get(i);
				} else {
					f.argNames[i] = "a" + (i + 1);
				}
				NTRArgument arg = new NTRArgument(parseNTRDT(paramTypes.get(i)), returnParamNames.contains(f.argNames[i]) ? 0 : -1);
				f.args[i] = arg;
			}
			if (f.returnTypes.isEmpty()) {
				f.returnTypes.add(NTRDataType.VOID);
			}
			funcs.add(f);
		}

		/*
		Splits the functions into lists per package/class
		 */
		Map<String, List<BSFunc>> funcsPerPackage = new HashMap<>();
		for (BSFunc f : funcs) {
			if (!funcsPerPackage.containsKey(f.packageAndClass)) {
				funcsPerPackage.put(f.packageAndClass, ArraysEx.asList(f));
			} else {
				funcsPerPackage.get(f.packageAndClass).add(f);
			}
		}

		for (Map.Entry<String, List<BSFunc>> e : funcsPerPackage.entrySet()) {
			String classPath = e.getKey().replace('.', '/');
			String className = FSUtil.getFileName(classPath);
			FSFile target = irdf.getChild(classPath + LangConstants.NATIVE_DEFINITION_EXTENSION);

			target.getParent().mkdirs();

			PrintStream out = new PrintStream(target.getOutputStream());

			out.println("/**======================================================");
			out.println("*");
			out.print("*    ");
			out.print(className);
			out.println(" native class function definition");
			out.print("*    Rev. ");
			out.println(FormattingUtils.getCommonFormattedDate());
			out.print("*    Source file: ");
			out.println(beaterScriptYml.documentName);
			out.println("*");
			out.println("*    This file was auto-generated by BS2PKS.");
			out.println("*");
			out.println("========================================================*/");

			out.println();
			out.print("public class ");
			out.print(className);
			out.println(" {");

			for (BSFunc f : e.getValue()) {
				out.println();
				out.print(f.getDecl(1));
			}

			out.println("}");

			out.close();
		}
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
