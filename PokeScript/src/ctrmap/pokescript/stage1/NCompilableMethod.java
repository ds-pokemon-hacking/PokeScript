package ctrmap.pokescript.stage1;

import ctrmap.pokescript.InboundDefinition;
import ctrmap.pokescript.data.LocalDataGraph;
import ctrmap.pokescript.data.Variable;
import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.instructions.abstractcommands.APlainOpCode;
import ctrmap.pokescript.instructions.providers.MetaFunctionHandler;
import ctrmap.pokescript.stage0.EffectiveLine;
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

	public MetaFunctionHandler metaHandler = null;

	public NCompilableMethod(String name, List<Modifier> mods, DeclarationContent.Argument[] args, TypeDef retnType) {
		def = new InboundDefinition(name, args, retnType, mods);
	}

	public NCompilableMethod(InboundDefinition def) {
		this.def = def;
	}

	public boolean isBlankBody() {
		return body.size() <= 2; //method begin instruction + return
	}
	
	public boolean isBodiless() {
		return body.isEmpty();
	}

	public void initWithCompiler(EffectiveLine line, NCompileGraph graph) {
		if (line.hasType(EffectiveLine.LineType.METHOD_BODY_START)) {
			addInstruction(graph.getPlain(APlainOpCode.BEGIN_METHOD));
		}

		locals = new LocalDataGraph(graph);
		//System.out.println("METHOD " + def.name);
		for (int i = 0; i < def.args.length; i++) {
			//System.out.println("ADDING ARGLOCAL " + def.args[i].typeDef);
			locals.addVariableUnderStackFrame(new Variable.LocalArgument(def.args[i].name, new ArrayList<>(), def.args[i].typeDef, graph, locals), graph);
		}

		if (hasModifier(Modifier.META)) {
			if (def.extendsBase != null) {
				metaHandler = graph.provider.getMetaFuncHandler(def.extendsBase);
			}

			if (metaHandler == null) {
				line.throwException("Metafunction handler not found: " + def.extendsBase + ". Invalid metafunction.");
			}
		}
	}

	public boolean hasModifier(Modifier m) {
		return def.hasModifier(m);
	}

	public void addInstruction(AInstruction ins) {
		body.add(ins);
	}

	public void addInstructions(List<AInstruction> l) {
		body.addAll(l);
	}

	public void insertInstruction(AInstruction ins) {
		body.add(1, ins);
	}

	public void insertInstructions(Collection<AInstruction> l) {
		body.addAll(1, l);
	}
}
