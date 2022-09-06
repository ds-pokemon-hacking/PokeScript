package ctrmap.pokescript.instructions.gen5.metahandlers;

import ctrmap.pokescript.expr.Throughput;
import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.instructions.abstractcommands.AInstructionType;
import ctrmap.pokescript.instructions.abstractcommands.APlainInstruction;
import ctrmap.pokescript.instructions.abstractcommands.APlainOpCode;
import ctrmap.pokescript.instructions.abstractcommands.MetaCall;
import ctrmap.pokescript.instructions.gen5.VConstants;
import ctrmap.pokescript.instructions.ntr.NTRArgument;
import ctrmap.pokescript.instructions.ntr.NTRDataType;
import ctrmap.pokescript.instructions.ntr.NTRInstructionCall;
import ctrmap.pokescript.instructions.ntr.NTRInstructionPrototype;
import ctrmap.pokescript.stage0.content.DeclarationContent;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.pokescript.stage2.AbstractCodeFactory;
import ctrmap.pokescript.types.DataType;

public class VActionSeqHandler implements VMetaFuncHandler {

	public static final NTRInstructionPrototype ACTIONSEQ_END_INS = new NTRInstructionPrototype(0xFE, new NTRArgument(NTRDataType.U16));
	public NTRInstructionPrototype ACTIONSEQ_APPLY_DEF = new NTRInstructionPrototype(0x64, new NTRArgument(NTRDataType.FLEX), new NTRArgument(NTRDataType.S32));

	@Override
	public void onDeclare(DeclarationContent cnt) {
		if (cnt.arguments.size() != 1 || cnt.arguments.get(0).typeDef.baseType != DataType.INT) {
			cnt.line.throwException("A VActionSeq can only have one argument specifying the target actor.");
		}
	}

	@Override
	public NTRInstructionCall compileMetaCall(AbstractCodeFactory<NTRInstructionCall> factory, MetaCall call, NCompileGraph g) {
		int tgtActor = -1;
		if (call.call.args.length > 0) {
			Throughput arg = call.call.args[0];
			if (arg.isImmediate()) {
				tgtActor = arg.getImmediateValue();
			} else {
				factory.assemble(arg.getCode(DataType.INT.typeDef()));
			}
		}

		//only one variable argument - we can reuse the primary reg straight away
		NTRInstructionCall callIns = ACTIONSEQ_APPLY_DEF.createCall(tgtActor == -1 ? VConstants.GP_REG_PRI : tgtActor, 0);
		factory.addInstruction(callIns);
		return callIns;
	}

	@Override
	public boolean assembleMetaInstruction(AbstractCodeFactory<NTRInstructionCall> assembler, AInstruction instruction, NCompileGraph graph) {
		if (instruction.getType() == AInstructionType.PLAIN) {
			APlainInstruction plain = (APlainInstruction) instruction;
			if (plain.opCode == APlainOpCode.RETURN) {
				assembler.addInstruction(ACTIONSEQ_END_INS.createCall(0));
			}
		}
		else if (instruction.getType() == AInstructionType.CALL_NATIVE) {
			return false; //assemble normally
		}
		return true; //handle here or ignore
	}

	@Override
	public void linkCall(NTRInstructionCall source, NTRInstructionCall target) {
		source.args[1] = target.pointer - (source.pointer + source.getSize());
	}
}
