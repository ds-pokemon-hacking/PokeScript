package ctrmap.pokescript.classfile;

import ctrmap.pokescript.InboundDefinition;
import ctrmap.pokescript.LangCompiler;
import ctrmap.pokescript.LangPlatform;
import ctrmap.pokescript.OutboundDefinition;
import ctrmap.pokescript.expr.Throughput;
import ctrmap.pokescript.instructions.abstractcommands.AAccessVariable;
import ctrmap.pokescript.instructions.abstractcommands.ACaseTable;
import ctrmap.pokescript.instructions.abstractcommands.AConditionJump;
import ctrmap.pokescript.instructions.abstractcommands.AFloatInstruction;
import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.instructions.abstractcommands.ALocalCall;
import ctrmap.pokescript.instructions.abstractcommands.ANativeCall;
import ctrmap.pokescript.instructions.abstractcommands.APlainInstruction;
import ctrmap.pokescript.stage0.Modifier;
import ctrmap.pokescript.stage0.Preprocessor;
import ctrmap.pokescript.stage0.content.DeclarationContent;
import ctrmap.pokescript.stage1.NCompilableMethod;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.pokescript.types.DataType;
import ctrmap.pokescript.types.TypeDef;
import ctrmap.scriptformats.pkslib.LibraryFile;
import xstandard.fs.FSFile;
import xstandard.fs.accessors.DiskFile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ILClassFile {

	private final List<NCompilableMethod> methods = new ArrayList<>();

	public ILClassFile(NCompileGraph cg) {
		methods.addAll(cg.methods);
	}

	public ILClassFile(ILCStream in) throws IOException {
		read(in);
	}

	public ILClassFile(FSFile fsf) {
		try {
			ILCStream in = new ILCStream(fsf.getIO());
			read(in);
			in.close();
		} catch (IOException ex) {
			Logger.getLogger(ILClassFile.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void read(ILCStream in) throws IOException {
		ILCObjTag header;

		while ((header = in.beginReadILCObj()) != ILCObjTag.ILCF_END) {
			switch (header) {
				case ILCF_START:
					break; //dummy
				case METHOD:
					methods.add(readMethod(in));
					break;
				default:
					throw new UnsupportedOperationException("ILCObjTag " + header + " not expected here!");
			}
		}
	}

	public static void main(String[] argss) {
		FSFile scrFile = new DiskFile("D:\\_REWorkspace\\CTRMapProjects\\White2\\user\\scripting\\workspace\\ZD0427-Events\\src\\MainEvents.pks");
		LangCompiler.CompilerArguments args = new LangCompiler.CompilerArguments();
		args.setPlatform(LangPlatform.EV_SWAN);
		args.includeRoots.add(new LibraryFile(new DiskFile("D:\\_REWorkspace\\PokeScriptWorkspaces\\TestWorkspace\\BW2-test\\lib\\PokeScriptSDK5-master")).getSourceDirForPlatform(LangPlatform.EV_SWAN));
		Preprocessor p = new Preprocessor(scrFile, args);
		NCompileGraph cg = p.getCompileGraph();
		ILClassFile ilc = new ILClassFile(cg);
		ilc.writeToFile(new DiskFile("D:\\_REWorkspace\\scr\\ilc\\debug.ilc"));

		ILClassFile reread = new ILClassFile(new DiskFile("D:\\_REWorkspace\\scr\\ilc\\debug.ilc"));
		reread.writeToFile(new DiskFile("D:\\_REWorkspace\\scr\\ilc\\debug2.ilc"));
	}

	public void writeToFile(FSFile fsf) {
		try {
			ILCStream out = new ILCStream(fsf.getIO());
			out.setLength(0);
			write(out);
			out.close();
		} catch (IOException ex) {
			Logger.getLogger(ILClassFile.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void write(ILCStream out) throws IOException {
		out.writeILCObjHeader(ILCObjTag.ILCF_START);

		for (NCompilableMethod m : methods) {
			out.writeILCObjHeader(ILCObjTag.METHOD);
			writeMethod(out, m);
		}

		out.writeILCObjHeader(ILCObjTag.ILCF_END);
	}

	private NCompilableMethod readMethod(ILCStream in) throws IOException {
		return in.readObject(new NCompilableMethod(null), ILCFieldID.MethodField.class, (io, m, fieldId) -> {
			switch (fieldId) {
				case HEADER:
					m.def = readMethodHeader(io);
					break;
			}
		});
	}

	private InboundDefinition readMethodHeader(ILCStream in) throws IOException {
		return in.readObject(new InboundDefinition(), ILCFieldID.MethodHeaderField.class, (io, def, fieldId) -> {
			switch (fieldId) {
				case NAME:
					def.name = in.readStringWithAddress();
					break;
				case BASE:
					def.extendsBase = in.readStringWithAddress();
					break;
				case RETURN_TYPE:
					def.retnType = readTypeRef(in);
					break;
				case MODIFIERS:
					decodeModifiers(in.readInt(), def.modifiers);
					break;
				case ARGUMENTS:
					def.args = in.readArray(DeclarationContent.Argument.class, new ILCStream.ILCListReaderCallback<DeclarationContent.Argument>() {
						@Override
						public DeclarationContent.Argument read(ILCStream in) throws IOException {
							return readArgDef(in);
						}
					});
					break;
			}
		});
	}

	private void writeMethod(ILCStream out, NCompilableMethod method) throws IOException {
		writeMethodHeaderAsField(out, ILCFieldID.MethodField.HEADER, method.def);
		writeMethodBodyAsField(out, ILCFieldID.MethodField.BODY, method.body);
		out.endObject();
	}

	private void writeInstructionList(ILCStream out, ILCFieldID fieldId, List<AInstruction> instructions) throws IOException {
		out.writeList(fieldId, instructions, new ILCStream.ILCListWriterCallback<AInstruction>() {
			@Override
			public void write(ILCStream out, AInstruction elem) throws IOException {
				out.writeEnum32ILC(ILCFieldID.InstructionField.TYPE, elem.getType());

				if (!elem.labels().isEmpty()) {
					out.writeStringList(ILCFieldID.InstructionField.LABELS, elem.labels());
				}

				out.beginWriteField(ILCFieldID.InstructionField.CONTENT);
				writeInstructionContent(out, elem);
				out.endWriteField();
				out.endObject();
			}
		});
	}

	private void writeMethodBodyAsField(ILCStream out, ILCFieldID fieldId, List<AInstruction> body) throws IOException {
		out.beginWriteField(fieldId);
		writeMethodBody(out, body);
		out.endWriteField();
	}

	private void writeMethodBody(ILCStream out, List<AInstruction> body) throws IOException {
		out.writeILCObjHeader(ILCObjTag.CODE);
		writeInstructionList(out, ILCFieldID.MethodBodyField.INSTRUCTIONS, body);
		out.endObject();
	}

	private void writeInstructionContent(ILCStream out, AInstruction elem) throws IOException {
		switch (elem.getType()) {
			case PLAIN:
				APlainInstruction pi = (APlainInstruction) elem;

				out.writeEnum32ILC(ILCFieldID.InstructionContentPlainField.OPCODE, pi.opCode);
				out.writeIntArray(ILCFieldID.InstructionContentPlainField.ARGUMENTS, pi.args);

				break;
			case PLAIN_FLOAT:
				AFloatInstruction fi = (AFloatInstruction) elem;

				out.writeEnum32ILC(ILCFieldID.InstructionContentPlainField.OPCODE, fi.opCode);
				out.writeFloatArray(ILCFieldID.InstructionContentPlainField.ARGUMENTS, fi.args);

				break;
			case JUMP:
				AConditionJump jmp = (AConditionJump) elem;

				out.writeEnum32ILC(ILCFieldID.InstructionContentJumpField.OPCODE, jmp.getOpCode());
				out.writeStringILC(ILCFieldID.InstructionContentJumpField.LABEL, jmp.targetLabel);

				break;
			case GET_VARIABLE:
			case SET_VARIABLE:
				AAccessVariable acv = (AAccessVariable) elem;

				out.writeStringILC(ILCFieldID.InstructionContentVarAccessField.VARNAME, acv.var.name);

				break;
			case CALL_LOCAL:
				ALocalCall c = (ALocalCall) elem;

				out.writeIntILC(ILCFieldID.InstructionContentCallLocalField.LOCALS_SIZE, c.localsSize);
				writeOutboundCall(out, ILCFieldID.InstructionContentCallLocalField.CALL, c.call);

				break;
			case CALL_NATIVE:
				ANativeCall nc = (ANativeCall) elem;

				writeOutboundCall(out, ILCFieldID.InstructionContentCallNativeField.CALL, nc.call);
				
				break;
			case CASE_TABLE:
				ACaseTable ct = (ACaseTable) elem;

				out.writeStringILC(ILCFieldID.InstructionContentCasetblField.DEFAULT_LABEL, ct.defaultCase);

				out.writeList(ILCFieldID.InstructionContentCasetblField.TARGETS, new ArrayList<>(ct.targets.entrySet()), new ILCStream.ILCListWriterCallback<Map.Entry<Integer, String>>() {
					@Override
					public void write(ILCStream out, Map.Entry<Integer, String> elem) throws IOException {
						out.writeIntILC(ILCFieldID.InstructionContentCasetblTargetField.REF_VALUE, elem.getKey());
						out.writeStringILC(ILCFieldID.InstructionContentCasetblTargetField.LABEL, elem.getValue());
					}
				});

				break;
		}
		out.endObject();
	}

	private void writeOutboundCall(ILCStream out, ILCFieldID id, OutboundDefinition call) throws IOException {
		out.beginWriteField(id);
		out.writeStringILC(ILCFieldID.CodeOutboundCallField.NAME, call.name);
		out.writeArray(ILCFieldID.CodeOutboundCallField.ARGS, call.args, new ILCStream.ILCListWriterCallback<Throughput>() {
			@Override
			public void write(ILCStream out, Throughput elem) throws IOException {
				writeTypeRefAsField(out, ILCFieldID.CodeThroughputField.TYPE, elem.type);

				writeInstructionList(out, ILCFieldID.CodeThroughputField.INSTRUCTIONS, elem.getCode(DataType.ANY.typeDef()));

				out.endObject();
			}
		});
		out.endObject();
		out.endWriteField();
	}

	private void writeMethodHeaderAsField(ILCStream out, ILCFieldID id, InboundDefinition header) throws IOException {
		out.beginWriteField(id);
		writeMethodHeader(out, header);
		out.endWriteField();
	}

	private void writeMethodHeader(ILCStream out, InboundDefinition header) throws IOException {
		out.writeIntILC(ILCFieldID.MethodHeaderField.MODIFIERS, encodeModifierList(header.modifiers));
		writeTypeRefAsField(out, ILCFieldID.MethodHeaderField.RETURN_TYPE, header.retnType);
		out.writeStringILC(ILCFieldID.MethodHeaderField.NAME, header.name);
		out.writeArray(ILCFieldID.MethodHeaderField.ARGUMENTS, header.args, (io, arg) -> {
			writeArgDef(io, arg);
		});
		out.writeStringILC(ILCFieldID.MethodHeaderField.BASE, header.extendsBase);
		out.endObject();
	}

	private DeclarationContent.Argument readArgDef(ILCStream in) throws IOException {
		return in.readObject(new DeclarationContent.Argument(), ILCFieldID.ArgDefField.class, (io, obj, fieldId) -> {
			switch (fieldId) {
				case NAME:
					obj.name = io.readStringWithAddress();
					break;
				case MODIFIERS:
					decodeModifiers(io.readInt(), obj.requestedModifiers);
					break;
				case TYPE:
					obj.typeDef = readTypeRef(io);
					break;
			}
		});
	}

	private void writeArgDef(ILCStream out, DeclarationContent.Argument arg) throws IOException {
		out.writeIntILC(ILCFieldID.ArgDefField.MODIFIERS, encodeModifierList(arg.requestedModifiers));
		writeTypeRefAsField(out, ILCFieldID.ArgDefField.TYPE, arg.typeDef);
		out.writeStringILC(ILCFieldID.ArgDefField.NAME, arg.name);
		out.endObject();
	}

	private void writeTypeRefAsField(ILCStream out, ILCFieldID fieldId, TypeDef td) throws IOException {
		out.beginWriteField(fieldId);
		writeTypeRef(out, td);
		out.endWriteField();
	}

	private TypeDef readTypeRef(ILCStream in) throws IOException {
		if (in.beginReadILCObj() != ILCObjTag.TYPEDEF_REFERENCE) {
			throw new RuntimeException("Bad object.");
		}

		return in.readObject(new TypeDef((String) null), ILCFieldID.TypeRefField.class, (io, r, fieldId) -> {
			switch (fieldId) {
				case CLASS_NAME:
					r.className = in.readStringWithAddress();
					break;
				case BASE_TYPE:
					r.baseType = in.readEnum32ILC(DataType.class);
					break;
			}
		});
	}

	private void writeTypeRef(ILCStream out, TypeDef td) throws IOException {
		out.writeILCObjHeader(ILCObjTag.TYPEDEF_REFERENCE);
		out.writeEnum32ILC(ILCFieldID.TypeRefField.BASE_TYPE, td.baseType);
		out.writeStringILC(ILCFieldID.TypeRefField.CLASS_NAME, td.className);
		out.endObject();
	}

	private void decodeModifiers(int mods, List<Modifier> dest) {
		for (int i = 0, pow = 1; i < Modifier.MODIFIERS_ALL.length; i++, pow <<= 1) {
			if ((mods & pow) != 0) {
				dest.add(Modifier.MODIFIERS_ALL[i]);
			}
		}
	}

	private int encodeModifierList(List<Modifier> l) {
		int i = 0;
		for (Modifier mod : l) {
			i |= (1 << mod.ordinal());
		}
		return i;
	}
}
