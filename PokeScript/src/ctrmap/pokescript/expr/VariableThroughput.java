package ctrmap.pokescript.expr;

import ctrmap.pokescript.types.DataType;
import ctrmap.pokescript.data.Variable;
import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.pokescript.types.TypeDef;
import java.util.ArrayList;

public class VariableThroughput extends Throughput {

	public Variable var;

	public VariableThroughput(Variable var, NCompileGraph cg) {
		super();
		this.var = var;
		code = new ArrayList<>();
		code.add(var.getReadIns(cg));

		type = new TypeDef(var.typeDef);
		type.baseType = type.baseType.getVarType();
	}

	public DataType getTypeDowncast() {
		switch (type.baseType){
			case VAR_INT:
				return DataType.INT;
			case VAR_CLASS:
				return DataType.CLASS;
			case VAR_FLOAT:
				return DataType.FLOAT;
			case VAR_BOOLEAN:
				return DataType.BOOLEAN;
			case VAR_ENUM:
				return DataType.ENUM;
		}
		return type.baseType;
	}

	public AInstruction getWriteIns(NCompileGraph cg) {
		return var.getWriteIns(cg);
	}
}
