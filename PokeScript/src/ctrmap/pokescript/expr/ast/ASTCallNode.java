package ctrmap.pokescript.expr.ast;

import ctrmap.pokescript.expr.CallOperator;

public class ASTCallNode extends ASTOperatorNode {

	private CallOperator call;
	
	public ASTCallNode(CallOperator oper) {
		super(oper);
		this.call = oper;
	}

	/*@Override
	public void analyzeThis(AST tree) {
		String methodName = call.funcName;
		
		for (ASTNode n : this) {
			if (n.isOperatorNode()) {
				ASTOperatorNode on = (ASTOperatorNode) n;
				if (on.operator != null && on.operator.getType() == OperatorType.SEQUENTIAL) {
					//arguments
					
				}
			}
		}
	}*/
}
