package ctrmap.pokescript.instructions.ntr;

import ctrmap.pokescript.InboundDefinition;
import ctrmap.pokescript.stage0.CompilerAnnotation;
import ctrmap.pokescript.stage0.CompilerPragma;
import ctrmap.pokescript.stage0.content.DeclarationContent;
import ctrmap.pokescript.stage1.NCompileGraph;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class NTRInstructionConstructor {

	public static NTRInstructionPrototype constructFromMethodHeader(NCompileGraph cg, InboundDefinition header) {
		int argAsReturnIdx = -1;
		List<Integer> argAsReturnIdxExtra = new ArrayList<>();
		if (header.hasAnnotation(NTRAnnotations.NAME_ARG_AS_RETURN)) {
			argAsReturnIdx = header.getAnnotation(NTRAnnotations.NAME_ARG_AS_RETURN).getIntArg(NTRAnnotations.ARG_ARG_NUM);
		}
		for (CompilerAnnotation a : header.getAnnotations(NTRAnnotations.NAME_ARG_AS_RETURN_ALT)){
			argAsReturnIdxExtra.add(a.getIntArg(NTRAnnotations.ARG_ARG_NUM));
		}
		Map<String, Integer> argBytesOverride = new HashMap<>();
		if (header.hasAnnotation(NTRAnnotations.NAME_ARG_BYTES_OVERRIDE)){
			for (CompilerAnnotation a : header.getAnnotations(NTRAnnotations.NAME_ARG_BYTES_OVERRIDE)){
				if (a.checkArgType(NTRAnnotations.ARG_BYTES, CompilerAnnotation.AnnotationType.INT)){
					argBytesOverride.put(a.getStrArg(NTRAnnotations.ARG_ARG_NAME), a.getIntArg(NTRAnnotations.ARG_BYTES));
				}
			}
		}

		NTRArgument[] args = new NTRArgument[header.args.length + (argAsReturnIdx == -1 ? 0 : 1) + argAsReturnIdxExtra.size()];
		
		NTRDataType defaultArgType = NTRDataType.U16;
		if (cg.hasPragma(CompilerPragma.FUNCARG_BYTES_DEFAULT)){
			defaultArgType = getNTRDTForBytes(cg.getIntPragma(CompilerPragma.FUNCARG_BYTES_DEFAULT));
		}

		int argSrcReloc = 0;
		for (int i = 0; i < args.length; i++) {
			if (i == argAsReturnIdx) {
				args[i] = new NTRArgument(NTRDataType.VAR, 0);

				argSrcReloc--;
			} else if (argAsReturnIdxExtra.contains(i)){
				args[i] = new NTRArgument(NTRDataType.VAR, argAsReturnIdxExtra.indexOf(i) + 1);
				
				argSrcReloc--;
			}
			else {
				DeclarationContent.Argument arg = header.args[i + argSrcReloc];
				
				NTRDataType type = defaultArgType;
				if (argBytesOverride.containsKey(arg.name)){
					type = getNTRDTForBytes(argBytesOverride.get(arg.name));
				}
				
				args[i] = new NTRArgument(type);
			}
		}
		
		NTRInstructionPrototype proto = new NTRInstructionPrototype(-1, args);
		proto.debugName = header.name;
		
		if (header.extendsBase != null){
			String eb = header.extendsBase;

			int opCode = -1;
			if (eb.startsWith("0x")){
				try {
					opCode = Integer.parseInt(eb.substring(2), 16);
				}
				catch (NumberFormatException ex){
					
				}
			}
			else {
				try {
					opCode = Integer.parseInt(eb);
				}
				catch (NumberFormatException ex){
					
				}
			}
			proto.opCode = opCode;
		}
		
		return proto;
	}
	
	private static NTRDataType getNTRDTForBytes(int bytes){
		switch (bytes){
			case 2:
				return NTRDataType.U16;
			case 1:
				return NTRDataType.U8;
			case 4:
				return NTRDataType.S32;
		}
		return NTRDataType.VOID;
	}
}
