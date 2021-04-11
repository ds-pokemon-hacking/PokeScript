package ctrmap.pokescript.ide.system;

public enum ResourcePathType {
	INTERNAL,
	ON_DISK,
	REMOTE,
	REMOTE_EXT;
	
	public static ResourcePathType fromName(String name){
		for (ResourcePathType t : values()){
			if (t.toString().equals(name)){
				return t;
			}
		}
		return null;
	}
}
