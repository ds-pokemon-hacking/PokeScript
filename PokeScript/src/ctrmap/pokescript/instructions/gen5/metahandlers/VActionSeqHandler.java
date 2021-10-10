package ctrmap.pokescript.instructions.gen5.metahandlers;

import ctrmap.pokescript.expr.Throughput;
import ctrmap.pokescript.instructions.abstractcommands.ACompiledInstruction;
import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.instructions.abstractcommands.ANativeCall;
import ctrmap.pokescript.instructions.abstractcommands.APlainOpCode;
import ctrmap.pokescript.instructions.abstractcommands.MetaCall;
import ctrmap.pokescript.instructions.gen5.VConstants;
import ctrmap.pokescript.instructions.ntr.NTRArgument;
import ctrmap.pokescript.instructions.ntr.NTRDataType;
import ctrmap.pokescript.instructions.ntr.NTRInstructionCall;
import ctrmap.pokescript.instructions.ntr.NTRInstructionPrototype;
import ctrmap.pokescript.instructions.ntr.instructions.PlainNTRInstruction;
import ctrmap.pokescript.instructions.providers.MetaFunctionHandler;
import ctrmap.pokescript.stage0.content.DeclarationContent;
import ctrmap.pokescript.stage1.NCompilableMethod;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.pokescript.types.DataType;
import java.util.ArrayList;
import java.util.List;

public class VActionSeqHandler implements MetaFunctionHandler {

	public static final NTRInstructionPrototype ACTIONSEQ_END_INS = new NTRInstructionPrototype(0xFE, new NTRArgument(NTRDataType.U16));
	public NTRInstructionPrototype ACTIONSEQ_APPLY_DEF = new NTRInstructionPrototype(0x64, new NTRArgument(NTRDataType.FLEX), new NTRArgument(NTRDataType.S32));

	@Override
	public void onDeclare(DeclarationContent cnt) {
		if (cnt.arguments.size() != 1 || cnt.arguments.get(0).typeDef.baseType != DataType.INT) {
			cnt.line.throwException("A VActionSeq can only have one argument specifying the target actor.");
		}
	}

	@Override
	public void onCompileBegin(NCompileGraph graph, NCompilableMethod method) {
	}

	@Override
	public void onCompileEnd(NCompileGraph graph, NCompilableMethod method) {
		for (int i = 0; i < method.body.size(); i++) {
			AInstruction ai = method.body.get(i);

			if (ai instanceof PlainNTRInstruction && ((PlainNTRInstruction) ai).opCode == APlainOpCode.RETURN) {
				PlainNTRInstruction ins = (PlainNTRInstruction) ai;

				ins.instructions.clear();
				ins.instructions.add(new NTRInstructionCall(ACTIONSEQ_END_INS, 0));
				//replace returns with action sequence terminators
			} else if (!(ai instanceof ANativeCall)) {
				method.body.remove(i);
				i--;
			}
		}
	}

	@Override
	public List<ACompiledInstruction> compileMetaCall(MetaCall call, NCompileGraph g) {
		List<ACompiledInstruction> l = new ArrayList<>();

		int imVal = -1;
		if (call.call.args.length > 0) {
			Throughput arg = call.call.args[0];
			if (arg.isImmediate()) {
				imVal = arg.getImmediateValue();
			} else {
				l.addAll(NTRInstructionCall.compileIL(arg.getCode(DataType.INT.typeDef()), g));
			}
		}

		int ptr = call.pointer;
		for (ACompiledInstruction i : l) {
			ptr += i.getSize();
		}
		ptr += ACTIONSEQ_APPLY_DEF.getSize();

		//only one variable argument - we can reuse the primary reg straight away
		l.add(new NTRInstructionCall(ACTIONSEQ_APPLY_DEF, imVal == -1 ? VConstants.GP_REG_PRI : imVal, g.getMethodByDef(call.call).getPointer() - ptr));

		return l;
	}
}
