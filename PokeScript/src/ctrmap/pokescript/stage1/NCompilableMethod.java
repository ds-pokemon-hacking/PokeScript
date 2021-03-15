package ctrmap.pokescript.stage1;

import ctrmap.pokescript.InboundDefinition;
import ctrmap.pokescript.data.LocalDataGraph;
import ctrmap.pokescript.data.Variable;
import ctrmap.pokescript.types.DataType;
import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.instructions.abstractcommands.APlainOpCode;
import ctrmap.pokescript.stage0.CompilerAnnotation;
import ctrmap.pokescript.stage0.Modifier;
import ctrmap.pokescript.stage0.content.DeclarationContent;
import ctrmap.pokescript.types.TypeDef;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class NCompilableMethod {

	public InboundDefinition def;
	public List<AInstruction> body = new ArrayList<>();
	public LocalDataGraph locals;

	public NCompilableMethod(String name, List<Modifier> mods, DeclarationContent.Argument[] args, TypeDef retnType) {
		def = new InboundDefinition(name, args, retnType, mods);
	}
	
	public void initWithCompiler(NCompileGraph graph){
		addInstruction(graph.getPlain(APlainOpCode.BEGIN_METHOD));
		
		locals = new LocalDataGraph(graph);
		for (int i = 0; i < def.args.length; i++){
			locals.addVariableUnderStackFrame(new Variable.Local(def.args[i].name, new ArrayList<Modifier>(), def.args[i].typeDef, graph), graph);
		}
	}
	
	public boolean hasModifier(Modifier m){
		return def.hasModifier(m);
	}

	public void addInstruction(AInstruction ins) {
		body.add(ins);
	}

	public void addInstructions(List<AInstruction> l) {
		body.addAll(l);
	}
	
	public void insertInstruction(AInstruction ins){
		body.add(1, ins);
	}
	
	public void insertInstructions(Collection<AInstruction> l){
		body.addAll(1, l);
	}

	public int getPointer() {
		return body.get(0).pointer; //first instruction is always PROC(0)
	}
}
