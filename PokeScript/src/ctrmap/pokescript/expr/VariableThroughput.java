package ctrmap.pokescript.expr;

import ctrmap.pokescript.types.DataType;
import ctrmap.pokescript.instructions.ctr.instructions.CTRInstruction;
import ctrmap.pokescript.data.Variable;
import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.stage1.NCompileGraph;
import java.util.ArrayList;

public class VariableThroughput extends Throughput {

	public Variable var;

	public VariableThroughput(Variable var, NCompileGraph cg) {
		super();
		this.var = var;
		code = new ArrayList<>();
		code.add(var.getReadIns(cg));
		if (var.typeDef.isClass()) {
			type = DataType.VAR_CLASS;
		} else {
			switch (var.typeDef.baseType){
				case INT:
					type = DataType.VAR_INT;
					break;
				case FLOAT:
					type = DataType.VAR_FLOAT;
					break;
				case BOOLEAN:
					type = DataType.VAR_BOOLEAN;
					break;
			}
		}
	}

	public DataType getTypeDowncast() {
		switch (type){
			case VAR_INT:
				return DataType.INT;
			case VAR_CLASS:
				return DataType.CLASS;
			case VAR_FLOAT:
				return DataType.FLOAT;
			case VAR_BOOLEAN:
				return DataType.BOOLEAN;
		}
		return type;
	}

	public AInstruction getWriteIns(NCompileGraph cg) {
		return var.getWriteIns(cg);
	}
}
