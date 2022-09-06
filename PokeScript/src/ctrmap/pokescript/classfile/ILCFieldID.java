package ctrmap.pokescript.classfile;

public interface ILCFieldID {
	
	public int ordinal();
	
	public static enum MethodField implements ILCFieldID {
		HEADER,
		BODY,
		METAHANDLER
	}
	
	public static enum MethodHeaderField implements ILCFieldID {
		MODIFIERS,
		RETURN_TYPE,
		NAME,
		ARGUMENTS,
		BASE
	}
	
	public static enum MethodBodyField implements ILCFieldID {
		INSTRUCTIONS
	}
	
	public static enum InstructionField implements ILCFieldID {
		TYPE,
		LABELS,
		CONTENT
	}
	
	public static enum InstructionContentPlainField implements ILCFieldID {
		OPCODE,
		ARGUMENTS
	}
	
	public static enum InstructionContentJumpField implements ILCFieldID {
		OPCODE,
		LABEL
	}
	
	public static enum InstructionContentCasetblField implements ILCFieldID {
		DEFAULT_LABEL,
		TARGETS
	}
	
	public static enum InstructionContentCasetblTargetField implements ILCFieldID {
		REF_VALUE,
		LABEL
	}
	
	public static enum InstructionContentVarAccessField implements ILCFieldID {
		VARNAME
	}
	
	public static enum InstructionContentCallLocalField implements ILCFieldID {
		LOCALS_SIZE,
		CALL
	}
	
	public static enum InstructionContentCallNativeField implements ILCFieldID {
		CALL
	}
	
	public static enum CodeOutboundCallField implements ILCFieldID {
		NAME,
		ARGS
	}
	
	public static enum CodeThroughputField implements ILCFieldID {
		TYPE,
		INSTRUCTIONS
	}
	
	public static enum TypeRefField implements ILCFieldID {
		BASE_TYPE,
		CLASS_NAME
	}
	
	public static enum ArgDefField implements ILCFieldID {
		MODIFIERS,
		TYPE,
		NAME
	}
}
