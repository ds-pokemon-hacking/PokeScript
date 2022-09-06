package ctrmap.pokescript.ide.system.beaterscript;

import ctrmap.pokescript.LangConstants;
import ctrmap.pokescript.instructions.ntr.NTRArgument;
import ctrmap.pokescript.instructions.ntr.NTRDataType;
import xstandard.formats.yaml.Yaml;
import xstandard.formats.yaml.YamlNode;
import xstandard.cli.ArgumentBuilder;
import xstandard.cli.ArgumentPattern;
import xstandard.cli.ArgumentType;
import xstandard.fs.FSFile;
import xstandard.fs.FSUtil;
import xstandard.fs.accessors.DiskFile;
import xstandard.text.FormattingUtils;
import xstandard.net.FileDownloader;
import xstandard.text.StringEx;
import xstandard.util.ArraysEx;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class BS2PKS {

	public static final String BSYML_URL_FORMAT = "https://raw.githubusercontent.com/PlatinumMaster/BeaterScript/master/%s.yml";

	private static final ArgumentPattern[] BS2PKS_ARGS = new ArgumentPattern[]{
		new ArgumentPattern("input", "An input file or the name of the source YAML in BeaterScript.", ArgumentType.STRING, "C:\\Users\\Čeněk\\eclipse-workspace\\BsYmlGen\\B2W2\\Base.yml", "-i, --input"),
		new ArgumentPattern("root", "A root directory for the output packages.", ArgumentType.STRING, "include", "-r", "--output-root")
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

		makePKSIncludes(new DiskFile(incRoot), yaml);
	}

	public static void makePKSIncludes(FSFile includesRoot, Yaml... ymls) {
		List<BSFunc> funcs = new ArrayList<>();

		Map<BSFunc, String> sourceFiles = new HashMap<>();

		/*
		Read the YAML into a function list
		 */
		for (Yaml beaterScriptYml : ymls) {
			for (YamlNode funcNode : beaterScriptYml.root.children) {
				int op = funcNode.getKeyInt();

				String name = funcNode.getChildByName("Name").getValue();
				String className = FSUtil.getFileNameWithoutExtension(beaterScriptYml.documentName);
				List<BSFunc.BSArgument> args = new ArrayList<>();

				YamlNode params = funcNode.getChildByName("Parameters");
				if (params != null) {
					int paramIdx = 1;
					for (YamlNode param : params.children) {
						BSFunc.BSArgument arg = new BSFunc.BSArgument();

						arg.name = param.getChildValue("Name");
						arg.type = parseNTRDT(param.getChildValue("Type"));
						arg.isReturn = param.getChildBoolValue("IsReturn");
						if (arg.isReturn) {
							arg.returnType = parseNTRDT(param.getChildValue("ReturnType"));
						}
						if (arg.type == null) {
							arg.type = NTRDataType.FLEX;
						}
						if (arg.name == null) {
							arg.name = "a" + paramIdx;
						}
						paramIdx++;
						args.add(arg);
					}
				}

				YamlNode pksName = funcNode.getChildByName("PSName");
				if (pksName != null) {
					name = pksName.getValue();
				}

				String brief = null;

				YamlNode briefNode = funcNode.getChildByName("Brief");
				if (briefNode != null) {
					brief = briefNode.getValue();
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
				f.args = args;
				f.brief = brief;
				funcs.add(f);
				sourceFiles.put(f, beaterScriptYml.documentName);
			}
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
			String classPath = FormattingUtils.getStrWithoutNonAlphanumeric(StringEx.deleteAllChars(e.getKey().replace('.', '/'), ' '), '/');
			String className = FSUtil.getFileName(classPath);
			FSFile target = includesRoot.getChild(classPath + LangConstants.LANG_GENERAL_HEADER_EXTENSION);

			target.getParent().mkdirs();

			PrintStream out = new PrintStream(target.getNativeOutputStream());

			List<String> localSourceFiles = new ArrayList<>();
			for (BSFunc f : e.getValue()) {
				String sf = sourceFiles.get(f);
				ArraysEx.addIfNotNullOrContains(localSourceFiles, sf);
			}

			out.println("/**");
			out.println(" *");
			out.print(" *\t");
			out.print(className);
			out.println(" native class function definition");
			out.print(" *\tRev. ");
			out.println(FormattingUtils.getCommonFormattedDate());
			if (localSourceFiles.size() > 1) {
				out.println(" *\tSource files: ");
				for (String sf : localSourceFiles) {
					out.print(" *\t\t");
					out.print(sf);
				}
			} else {
				out.print(" *\tSource file: ");
				out.println(localSourceFiles.get(0));
			}
			out.println(" *");
			out.println(" *\tThis file was auto-generated by BS2PKS.");
			out.println(" *");
			out.println(" */");

			out.print("public class ");
			out.print(className);
			out.println(" {");
			
			for (BSFunc f : e.getValue()) {
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
