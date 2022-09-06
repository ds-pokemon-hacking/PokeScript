package ctrmap.pokescript.stage1;

import ctrmap.pokescript.data.Variable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

public class BlockStack<T extends CompileBlock> extends Stack<T> {

	public BlockStack() {
		super();
	}
	
	public CompileBlock getLatestBlock(){
		if (empty()){
			return null;
		}
		return peek();
	}

	@Override
	public Iterator<T> iterator() {
		return new Iterator() {
			int idx = size();

			@Override
			public boolean hasNext() {
				return idx > 0;
			}

			@Override
			public Object next() {
				idx--;
				return get(idx);
			}
		};
	}
	
	public CompileBlock getFirstBlockForAttr(CompileBlock.BlockAttribute a, String label) {
		for (CompileBlock b : this) {
			if (b.hasAttribute(a)) {
				if (label == null || b.getShortBlockName().equals(label)) {
					return b;
				}
			}
		}
		return null;
	}

	public BlockResult getBlocksToAttribute(CompileBlock.BlockAttribute a, String label) {
		BlockResult rsl = new BlockResult();

		Iterator<? extends CompileBlock> it = iterator();
		while (it.hasNext()) {
			CompileBlock blk = it.next();
			rsl.blocks.push(blk);
			if (blk.hasAttribute(a)) {
				if (label == null || blk.getShortBlockName().equals(label)) {
					break;
				}
			}
		}
		return rsl;
	}

	public static class BlockResult {

		public BlockStack<CompileBlock> blocks = new BlockStack<>();

		public List<Variable> collectLocalsNoBottom(CompileBlock bottomBlock) {
			List<Variable> v = new ArrayList<>();
			int index = blocks.indexOf(bottomBlock);
			for (int i = index + 1; i < blocks.size(); i++) {
				v.addAll(blocks.get(i).localsOfThisBlock);
			}
			return v;
		}
		
		public CompileBlock getBlockByAttr(CompileBlock.BlockAttribute a, String name) {
			for (CompileBlock b : blocks) {
				if (b.hasAttribute(a)) {
					if (name == null || b.getShortBlockName().equals(name)) {
						return b;
					}
				}
			}
			return null;
		}

		public CompileBlock getBottomBlock() {
			return blocks.firstElement();
		}
		
		public CompileBlock getBlockByName(String name) {
			for (CompileBlock b : blocks) {
				if (b.getShortBlockName().equals(name)) {
					return b;
				}
			}
			return null;
		}
	}
}
