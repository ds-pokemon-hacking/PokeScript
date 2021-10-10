package ctrmap.pokescript.instructions.ntr;

import ctrmap.pokescript.InboundDefinition;
import ctrmap.pokescript.stage0.CompilerAnnotation;
import ctrmap.pokescript.stage0.CompilerPragma;
import ctrmap.pokescript.stage0.content.DeclarationContent;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.pokescript.types.DataType;
import ctrmap.stdlib.util.ParsingUtils;
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
			defaultArgType = getNTRDTForBytes(cg.getIntPragma(CompilerPragma.FUNCARG_BYTES_DEFAULT), null);
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
				if (arg.typeDef.baseType == DataType.FLOAT){
					type = NTRDataType.FX16; //FX16
				}
				if (argBytesOverride.containsKey(arg.name)){
					type = getNTRDTForBytes(argBytesOverride.get(arg.name), type);
					//For FX16, override to 2 bytes
				}
				
				args[i] = new NTRArgument(type);
			}
		}
		
		NTRInstructionPrototype proto = new NTRInstructionPrototype(-1, args);
		proto.debugName = header.name;
		
		if (header.extendsBase != null){
			String eb = header.extendsBase;

			int opCode = ParsingUtils.parseBasedIntOrDefault(eb, -1);
			
			proto.opCode = opCode;
		}
		
		return proto;
	}
	
	private static NTRDataType getNTRDTForBytes(int bytes, NTRDataType baseType){
		switch (bytes){
			case 2:
				return baseType == NTRDataType.FX16 ? NTRDataType.FX16 : NTRDataType.U16;
			case 1:
				return NTRDataType.U8;
			case 4:
				return baseType == NTRDataType.FX16 ? NTRDataType.FX32 : NTRDataType.S32;
		}
		return NTRDataType.VOID;
	}
}
