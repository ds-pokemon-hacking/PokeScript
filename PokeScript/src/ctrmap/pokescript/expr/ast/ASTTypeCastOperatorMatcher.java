
package ctrmap.pokescript.expr.ast;

import ctrmap.pokescript.expr.OperatorType;
import ctrmap.pokescript.expr.Operator;
import ctrmap.pokescript.expr.TypeCast;
import ctrmap.pokescript.types.TypeDef;

public class ASTTypeCastOperatorMatcher extends ASTOperatorMatcher {

	public ASTTypeCastOperatorMatcher() {
		super(TypeCast.class, OperatorType.UNARY, new ASTContentType[]{null, ASTContentType.BRACKET_START, ASTContentType.SYMBOL, ASTContentType.BRACKET_END}, new String[]{null, null, null, null});
	}
	
	@Override
	public int getEvalStartIndexOffs() {
		return -1;
	}
	
	@Override
	public boolean matchType(AST tree, int contentIndex, ASTContentType type) {
		if (contentIndex != 0) {
			return super.matchType(tree, contentIndex, type);
		}
		return type != ASTContentType.SYMBOL;
	}
	
	@Override
	public boolean matchContent(AST tree, int contentIndex, String content) {
		if (contentIndex != 2) {
			return super.matchContent(tree, contentIndex, content);
		}
		return tree.isTypeName(content);
	}

	@Override
	public Operator instantiate(String[] actualContent) {
		return new TypeCast(new TypeDef(actualContent[2]));
	}

}
