
package ctrmap.pokescript.data;

import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.stage0.Modifier;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.pokescript.types.TypeDef;
import java.util.List;

/**
 *
 */
public class ClassVariable extends Variable {
	
	public ClassVariable(String name, List<Modifier> modifiers, TypeDef type, NCompileGraph cg){
		super(name, modifiers, type, cg);
	}

	@Override
	public VarLoc getLocation() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public AInstruction getReadIns(NCompileGraph cg) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public AInstruction getWriteIns(NCompileGraph cg) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

}
