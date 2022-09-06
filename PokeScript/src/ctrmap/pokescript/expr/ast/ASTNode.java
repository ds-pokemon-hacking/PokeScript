package ctrmap.pokescript.expr.ast;

import ctrmap.pokescript.expr.Throughput;
import ctrmap.pokescript.stage1.NCompileGraph;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class ASTNode implements Iterable<ASTNode> {

	public AST tree;
	
	public final ASTContentType charType;
	public ASTNode parent;
	protected List<ASTNode> children = new ArrayList<>();

	public ASTNode(ASTContentType type) {
		this.charType = type;
	}
	
	public abstract Throughput toThroughput(NCompileGraph cg);
	
	public int getChildCount() {
		return children.size();
	}
	
	public ASTNode getChild(int index) {
		if (index < 0 || index >= children.size()) {
			return null;
		}
		return children.get(index);
	}

	public boolean isOperatorNode() {
		return charType == ASTContentType.OP_CHAR;
	}

	public final void analyze(AST tree) {
		this.tree = tree;
		for (ASTNode child : children) {
			child.analyze(tree);
		}
		analyzeThis(tree);
	}

	protected void analyzeThis(AST tree) {

	}

	public final ASTNode simplify(AST ast) {
		for (int i = 0; i < children.size(); i++) {
			ASTNode ch = children.get(i);
			ASTNode simplified = ch.simplify(ast);
			if (simplified != ch) {
				children.set(i, simplified);
				simplified.parent = this;
			}
		}
		return simplifyThis(ast);
	}

	protected ASTNode simplifyThis(AST ast) {
		return this;
	}

	public void removeChild(ASTNode child) {
		child.parent = null;
		children.remove(child);
	}
	
	public void moveChild(int src, int dest) {
		children.set(dest, children.get(src));
	}

	public void setChildAt(int idx, ASTNode child) {
		if (child != null) {
			child.parent = this;
			children.set(idx, child);
		}
	}
	
	public void addChild(ASTNode child) {
		if (child != null) {
			child.parent = this;
			children.add(child);
		}
	}

	@Override
	public Iterator<ASTNode> iterator() {
		return children.iterator();
	}
}
