package ctrmap.pokescript.ide.system.beaterscript;

import ctrmap.pokescript.instructions.ntr.NTRAnnotations;
import ctrmap.pokescript.instructions.ntr.NTRArgument;
import ctrmap.pokescript.instructions.ntr.NTRDataType;
import ctrmap.pokescript.types.DataType;
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
	public List<NTRDataType> returnTypes = new ArrayList<>();
	public List<String> returnArgNames = new ArrayList<>();
	public String[] argNames;
	public NTRArgument[] args;

	public String getDecl() {
		return getDecl(0);
	}

	public String getDecl(int indent) {
		StringBuilder sb = new StringBuilder();
		String ind = getIndent(indent);

		for (int r = 0; r < returnTypes.size(); r++) {
			appendBrief(sb, indent);
			NTRDataType returnType = returnTypes.get(r);
			String returnArgName = r < returnArgNames.size() ? returnArgNames.get(r) : null;

			if (r != 0) {
				sb.append("\n");
			}

			for (int i = 0; i < args.length; i++) {
				NTRArgument arg = args[i];
				if (argNames[i].equals(returnArgName)) {
					sb.append(ind);
					sb.append("@");
					sb.append(NTRAnnotations.NAME_ARG_AS_RETURN);
					sb.append("(");
					sb.append(NTRAnnotations.ARG_ARG_NUM);
					sb.append(" = ");
					sb.append(i);
					sb.append(")\n");
				} else if (arg.isReturnCallback()) {
					sb.append(ind);
					sb.append("@");
					sb.append(NTRAnnotations.NAME_ARG_AS_RETURN_ALT);
					sb.append("(");
					sb.append(NTRAnnotations.ARG_ARG_NUM);
					sb.append(" = ");
					sb.append(i);
					sb.append(")\n");
				}
				if (arg.dataType.sizeof != Short.BYTES) {
					sb.append(ind);
					sb.append("@");
					sb.append(NTRAnnotations.NAME_ARG_BYTES_OVERRIDE);
					sb.append("(");
					sb.append(NTRAnnotations.ARG_ARG_NAME);
					sb.append(" = ");
					sb.append(argNames[i]);
					sb.append(", ");
					sb.append(NTRAnnotations.ARG_BYTES);
					sb.append(" = ");
					sb.append(arg.dataType.sizeof);
					sb.append(")\n");
				}
			}

			sb.append(ind);
			sb.append("static native ");
			sb.append(getPKSType(returnType).getFriendlyName());
			sb.append(" ");
			if (returnTypes.size() == 1) {
				sb.append(names[0]);
			} else {
				sb.append((r + 1) < names.length ? names[r + 1] : names[0] + (r + 1));
			}
			sb.append("(");
			int firstArg = 0;
			for (; firstArg < args.length; firstArg++) {
				if (!args[firstArg].isReturnCallback()) {
					break;
				}
			}
			for (int i = firstArg; i < args.length; i++) {
				NTRArgument a = args[i];
				if (a.isReturnCallback()) {
					continue;
				}
				if (i != firstArg) {
					sb.append(", ");
				}
				if (a.dataType == NTRDataType.VAR) {
				//	sb.append("var "); //do not do this, allow for numeric variables
				} else if (a.dataType != NTRDataType.FLEX) {
					sb.append("final ");
				}
				sb.append(getPKSType(a.dataType).getFriendlyName());
				sb.append(" ");
				sb.append(argNames[i]);
			}
			appendOpCodeAndEnd(sb);
		}

		if (returnTypes.size() > 1) {
			sb.append("\n");

			appendBrief(sb, indent);

			//append combined function
			sb.append(ind);
			sb.append("static native void ");
			sb.append(names[0]);
			sb.append("(");
			for (int i = 0; i < args.length; i++) {
				NTRArgument a = args[i];
				if (i != 0) {
					sb.append(", ");
				}
				if (a.isReturnCallback()) {
					sb.append("var ");
				} else if (a.dataType != NTRDataType.FLEX) {
					sb.append("final ");
				}
				sb.append(getPKSType(a.dataType).getFriendlyName());
				sb.append(" ");
				sb.append(argNames[i]);
			}
			appendOpCodeAndEnd(sb);
		}

		boolean hasVarArg = false;
		for (int i = 0; i < args.length; i++) {
			if (args[i].dataType == NTRDataType.VAR && !returnArgNames.contains(argNames[i])) {
				hasVarArg = true;
				break;
			}
		}

		return sb.toString();
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
			String[] briefLines = brief.split("\\. ");
			for (String bl : briefLines) {
				if (!bl.isEmpty()) {
					sb.append(ind);
					sb.append(" * ");
					sb.append(bl);
					if (!bl.endsWith(".")) {
						sb.append(".");
					}
					sb.append("\n");
				}
			}
			sb.append(ind);
			sb.append("*/\n");
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
