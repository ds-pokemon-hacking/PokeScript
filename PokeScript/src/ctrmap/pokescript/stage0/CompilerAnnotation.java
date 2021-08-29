
package ctrmap.pokescript.stage0;

import java.util.HashMap;
import java.util.Map;

public class CompilerAnnotation {
	private EffectiveLine line;
	
	public String name;
	public Map<String, String> args = new HashMap<>();
	
	public CompilerAnnotation(EffectiveLine line, String name){
		this.name = name;
		this.line = line;
	}
	
	public boolean checkArgType(String argName, AnnotationType tp){
		String argValue = args.get(argName);
		if (argValue == null){
			return false;
		}
		switch (tp){
			case BOOLEAN:
				return argValue.equals(Boolean.FALSE.toString()) || argValue.equals(Boolean.TRUE.toString());
			case INT:
				try {
					Integer.parseInt(argValue);
					return true;
				}
				catch (NumberFormatException ex){
					return false;
				}
			case STRING:
				return true;
		}
		return false;
	}
	
	public int getIntArg(String name){
		if (checkArgType(name, AnnotationType.INT)){
			return Integer.parseInt(args.get(name));
		}
		line.throwException("Invalid annotation parameter for type int.");
		return -1;
	}
	
	public boolean getBlnArg(String name){
		if (checkArgType(name, AnnotationType.BOOLEAN)){
			return Boolean.parseBoolean(args.get(name));
		}
		line.throwException("Invalid annotation parameter for type boolean.");
		return false;
	}
	
	public String getStrArg(String name){
		if (checkArgType(name, AnnotationType.STRING)){
			return args.get(name);
		}
		line.throwException("Invalid annotation parameter for type String.");
		return "";
	}
	
	public static enum AnnotationType {
		BOOLEAN,
		INT,
		STRING
	}
}
