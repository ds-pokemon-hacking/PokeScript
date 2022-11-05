package ctrmap.pokescript.expr.ast;

import ctrmap.pokescript.expr.CmnMathOp;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ASTMathOperatorMatcher extends ASTOperatorMatcher {

	private final boolean allowDouble;
	private final Class<? extends CmnMathOp> mathCls;

	public ASTMathOperatorMatcher(Class<? extends CmnMathOp> cls, String... contents) {
		this(cls, false, contents);
	}
	
	public ASTMathOperatorMatcher(Class<? extends CmnMathOp> cls, boolean allowDouble, String... contents) {
		super(cls, contents);
		this.allowDouble = allowDouble;
		this.mathCls = cls;
	}

	@Override
	public CmnMathOp instantiate(String[] actualContent) {
		try {
			Constructor<? extends CmnMathOp> c = mathCls.getConstructor(Boolean.TYPE, Boolean.TYPE);
			boolean isMoreChar = contents.length > 1;
			return c.newInstance(isMoreChar && contents[1].equals(contents[0]) && allowDouble, isMoreChar && contents[contents.length - 1].equals("="));
		} catch (IllegalAccessException | NoSuchMethodException | SecurityException | InstantiationException | IllegalArgumentException | InvocationTargetException ex) {
			Logger.getLogger(AST.class.getName()).log(Level.SEVERE, "Could not instantiate operator " + mathCls, ex);
		}
		return null;
	}

}
