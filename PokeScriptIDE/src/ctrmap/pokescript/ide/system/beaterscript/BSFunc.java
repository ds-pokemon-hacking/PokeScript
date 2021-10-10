package ctrmap.pokescript.ide.system.beaterscript;

import ctrmap.pokescript.instructions.ntr.NTRAnnotations;
import ctrmap.pokescript.instructions.ntr.NTRDataType;
import ctrmap.pokescript.types.DataType;
import ctrmap.stdlib.text.StringEx;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class BSFunc {

	public String packageAndClass;
	public String[] names;
	public String brief;
	public int opCode;

	public List<BSArgument> args = new ArrayList<>();

	public static class BSArgument {

		public String name;
		public NTRDataType type;
		public NTRDataType returnType;

		public boolean isReturn;
	}

	public String getDecl() {
		return getDecl(0);
	}

	public String getDecl(int indent) {
		StringBuilder sb = new StringBuilder();
		String ind = getIndent(indent);

		List<BSArgument> returnArguments = new ArrayList<>();
		for (BSArgument a : args) {
			if (a.isReturn) {
				returnArguments.add(a);
			}
		}

		for (int r = 0; r < returnArguments.size(); r++) {
			sb.append('\n');
			appendBrief(sb, indent);

			BSArgument retnArg = returnArguments.get(r);

			NTRDataType returnType = retnArg.returnType;

			for (int i = 0; i < args.size(); i++) {
				BSArgument arg = args.get(i);

				if (arg == retnArg) {
					sb.append(ind);
					appendAnnotation(sb, NTRAnnotations.NAME_ARG_AS_RETURN, NTRAnnotations.ARG_ARG_NUM, String.valueOf(i));
				} else if (arg.isReturn) {
					sb.append(ind);
					appendAnnotation(sb, NTRAnnotations.NAME_ARG_AS_RETURN_ALT, NTRAnnotations.ARG_ARG_NUM, String.valueOf(i));
				}
				appendArgSizeAnnotation(arg, sb, ind);
			}

			sb.append(ind);
			sb.append("static native ");
			sb.append(getPKSType(returnType).getFriendlyName());
			sb.append(" ");
			if (returnArguments.size() == 1) {
				sb.append(names[0]);
			} else {
				sb.append((r + 1) < names.length ? names[r + 1] : names[0] + (r + 1));
			}
			sb.append("(");
			int firstArg = 0;
			for (; firstArg < args.size(); firstArg++) {
				if (!args.get(firstArg).isReturn) {
					break;
				}
			}
			for (int i = firstArg; i < args.size(); i++) {
				BSArgument a = args.get(i);
				if (a.isReturn) {
					continue;
				}
				if (i != firstArg) {
					sb.append(", ");
				}
				if (a.type == NTRDataType.VAR) {
					//	sb.append("var "); //do not do this, allow for numeric variables
				} else if (a.type != NTRDataType.FLEX) {
					sb.append("final ");
				}
				sb.append(getPKSType(a.type).getFriendlyName());
				sb.append(" ");
				sb.append(a.name);
			}
			appendOpCodeAndEnd(sb);
		}

		if (returnArguments.size() > 1 || returnArguments.isEmpty()) {
			sb.append('\n');

			appendBrief(sb, indent);
			for (BSArgument arg : args) {
				appendArgSizeAnnotation(arg, sb, ind);
			}

			//append combined function
			sb.append(ind);
			sb.append("static native void ");
			sb.append(names[0]);
			sb.append("(");
			for (int i = 0; i < args.size(); i++) {
				BSArgument a = args.get(i);
				if (i != 0) {
					sb.append(", ");
				}
				if (a.type == NTRDataType.VAR) {
					//sb.append("var ");
				} else if (a.type != NTRDataType.FLEX) {
					sb.append("final ");
				}
				sb.append(getPKSType(a.type).getFriendlyName());
				sb.append(" ");
				sb.append(a.name);
			}
			appendOpCodeAndEnd(sb);
		}

		return sb.toString();
	}

	private void appendArgSizeAnnotation(BSArgument arg, StringBuilder sb, String ind) {
		if (arg.type.sizeof != Short.BYTES) {
			sb.append(ind);
			appendAnnotation(sb, NTRAnnotations.NAME_ARG_BYTES_OVERRIDE, NTRAnnotations.ARG_ARG_NAME, arg.name, NTRAnnotations.ARG_BYTES, String.valueOf(arg.type.sizeof));
		}
	}

	private static void appendAnnotation(StringBuilder sb, String name, String... paramsAndValues) {
		sb.append("@");
		sb.append(name);
		sb.append("(");
		for (int i = 0; i < paramsAndValues.length >> 1; i++) {
			if (i != 0) {
				sb.append(", ");
			}
			sb.append(paramsAndValues[i * 2]);
			sb.append(" = ");
			sb.append(paramsAndValues[i * 2 + 1]);
		}
		sb.append(")\n");
	}

	private void appendOpCodeAndEnd(StringBuilder sb) {
		sb.append(") : ");
		sb.append("0x");
		sb.append(Integer.toHexString(opCode));
		sb.append(";\n");
	}

	private void appendBrief(StringBuilder sb, int indentLvl) {
		String ind = getIndent(indentLvl);
		if (brief != null) {
			sb.append(ind);
			sb.append("/**\n");
			String[] briefLines = StringEx.splitOnecharFast(brief, '.');
			for (String bl : briefLines) {
				if (!bl.isEmpty()) {
					sb.append(ind);
					sb.append(" *");
					sb.append("\t");
					sb.append(bl.trim());
					if (!bl.endsWith(".")) {
						sb.append(".");
					}
					sb.append("\n");
				}
			}
			sb.append(ind);
			sb.append(" */\n");
		}
	}

	private static String getIndent(int lvl) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < lvl; i++) {
			sb.append('\t');
		}
		return sb.toString();
	}

	private static DataType getPKSType(NTRDataType ntrdt) {
		switch (ntrdt) {
			case FLEX:
			case U16:
			case S32:
			case U8:
				return DataType.INT;
			case VAR:
				return DataType.INT;
			case VOID:
				return DataType.VOID;
			case BOOL:
				return DataType.BOOLEAN;
			case FX16:
			case FX32:
				return DataType.FLOAT;
		}
		return null;
	}
}
