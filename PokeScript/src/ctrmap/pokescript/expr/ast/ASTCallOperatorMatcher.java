package ctrmap.pokescript.expr.ast;

import ctrmap.pokescript.expr.OperatorType;
import ctrmap.pokescript.expr.CallOperator;

public class ASTCallOperatorMatcher extends ASTOperatorMatcher {

	public ASTCallOperatorMatcher() {
		super(CallOperator.class, OperatorType.UNARY, new ASTContentType[]{ASTContentType.SYMBOL, ASTContentType.BRACKET_START}, new String[]{null});
	}

	@Override
	public ASTNode makeNode(String[] actualContent) {
		return new ASTCallNode(instantiate(actualContent));
	}

	@Override
	public CallOperator instantiate(String[] actualContent) {
		return new CallOperator(actualContent[0]);
	}

}
