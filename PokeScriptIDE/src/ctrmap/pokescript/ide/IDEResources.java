package ctrmap.pokescript.ide;

import xstandard.res.ResourceAccess;
import java.io.File;
import xstandard.res.ResourceAccessor;


public class IDEResources {
	
	public static final ResourceAccessor ACCESSOR = new ResourceAccessor("ctrmap/resources");
	
	public static void load(){
		ResourceAccess.loadResourceTable(IDEResources.class.getClassLoader(), "ctrmap/resources/res_ide.tbl");
	}
	
	public static void main(String[] args){
		ResourceAccess.buildResourceTable(new File("src/ctrmap/resources"), "ctrmap/resources", "res_ide.tbl");
	}
}
