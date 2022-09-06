package ctrmap.pokescript.stage0.content;

import ctrmap.pokescript.expr.Throughput;
import ctrmap.pokescript.expr.ast.AST;
import ctrmap.pokescript.stage0.Preprocessor;
import ctrmap.pokescript.stage0.EffectiveLine;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.pokescript.types.DataType;

public class ExpressionContent extends AbstractContent {

	public String trimmedData;

	public ExpressionContent(EffectiveLine source) {
		super(source);
		trimmedData = Preprocessor.getStrWithoutTerminator(source.data);
		//we can safely remove ALL whitespaces in expressions, which makes analysis much more straightforward. - EDIT: the NExpression class will do this instead
	}

	@Override
	public CompilerContentType getContentType() {
		return CompilerContentType.EXPRESSION;
	}

	@Override
	public void addToGraph(NCompileGraph graph) {
		//create an NExpression that compiles into the method
		Throughput tp = new AST(line, graph, trimmedData).toThroughput();
		if (tp != null) {
			graph.addInstructions(tp.getCode(DataType.ANY.typeDef()));
		}
	}
}
