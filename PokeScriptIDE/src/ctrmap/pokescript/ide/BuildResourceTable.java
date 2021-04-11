package ctrmap.pokescript.ide;

import ctrmap.stdlib.res.ResourceAccess;
import java.io.File;


public class BuildResourceTable {
	public static void main(String[] args){
		ResourceAccess.buildResourceTable(new File("src/ctrmap/resources"));
	}
}
