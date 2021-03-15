
package ctrmap.pokescript.types;

import ctrmap.scriptformats.gen6.PawnInstruction;
import ctrmap.pokescript.FloatLib;
import ctrmap.pokescript.OutboundDefinition;
import ctrmap.pokescript.expr.Throughput;
import ctrmap.pokescript.instructions.ctr.instructions.PNativeCall;
import ctrmap.pokescript.instructions.abstractcommands.APlainOpCode;
import ctrmap.pokescript.stage0.EffectiveLine;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.stdlib.util.ArraysEx;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class IntegerTypeHandler implements AbstractTypeHandler{

	@Override
	public Throughput tryAssign(String source, NCompileGraph cg) {
		int intValue = 0;
		int radix = 10;
		if (source.startsWith("0x")){
			radix = 16;
			source = source.substring(2);
		}
		else if (source.startsWith("0b")){
			radix = 2;
			source = source.substring(2);
		}
		
		try {
			intValue = Integer.parseInt(source, radix);
			return new Throughput(intValue, cg);
		} catch (NumberFormatException ex) {

		}
		return null;
	}

	@Override
	public CastResult getInstructionForCast(DataType castedType, NCompileGraph cg) {
		CastResult r = new CastResult();
		r.success = true;
		switch (castedType){
			case INT:
				return r;
			case FLOAT:
				OutboundDefinition flt = FloatLib._float.createDummyOutbound();
				flt.args[0] = new Throughput(castedType, ArraysEx.asList(cg.getPlain(APlainOpCode.PUSH_PRI)));
				r.instructions.add(cg.provider.getNativeCall(flt));
				return r;
		}
		r.success = false;
		r.exception = "Integer can not be casted to " + castedType;
		return r;
	}

}
