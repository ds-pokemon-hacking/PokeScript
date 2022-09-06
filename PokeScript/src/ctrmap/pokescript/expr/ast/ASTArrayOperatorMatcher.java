package ctrmap.pokescript.expr.ast;

import ctrmap.pokescript.expr.ArrayOperator;
import ctrmap.pokescript.expr.OperatorType;
import ctrmap.pokescript.expr.CallOperator;

public class ASTArrayOperatorMatcher extends ASTOperatorMatcher {

	public ASTArrayOperatorMatcher() {
		super(CallOperator.class, OperatorType.BINARY, new ASTContentType[]{ASTContentType.ARRAY_BRACKET_START}, new String[]{});
	}

	@Override
	public ArrayOperator instantiate(String[] actualContent) {
		return new ArrayOperator();
	}
}
