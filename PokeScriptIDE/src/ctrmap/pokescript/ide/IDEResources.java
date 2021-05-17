package ctrmap.pokescript.ide;

import ctrmap.stdlib.res.ResourceAccess;
import java.io.File;


public class IDEResources {
	public static void load(){
		ResourceAccess.loadResourceTable("res_ide.tbl");
	}
	
	public static void main(String[] args){
		ResourceAccess.buildResourceTable(new File("src/ctrmap/resources"), "res_ide.tbl");
	}
}
