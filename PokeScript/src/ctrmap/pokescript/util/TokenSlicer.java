package ctrmap.pokescript.util;

import java.util.ArrayList;
import java.util.List;

public class TokenSlicer<T, B> {

	private final BlockPattern<T, B>[] patterns;

	public TokenSlicer(BlockPattern<T, B>... patterns) {
		this.patterns = patterns;
	}

	public List<Block<T, B>> slice(List<Token<T>> tokens) {
		List<Block<T, B>> blocks = new ArrayList<>();

		BlockPattern<T, B> curPattern = null;
		int blockStartPos = 0;
		for (int pos = 0; pos < tokens.size(); pos++) {
			BlockPattern<T, B> newPtn = curPattern;
			if (curPattern == null || !curPattern.hasDefinedEndTokens()) {
				for (BlockPattern<T, B> pattern : patterns) {
					if (pattern != curPattern && pattern.checkStart(tokens, pos)) {
						newPtn = pattern;
						break;
					}
				}
			} else {
				if (curPattern.checkEnd(tokens, pos)) {
					newPtn = null;
				}
			}
			if (newPtn != curPattern) {
				if (curPattern != null) {
					blocks.add(new Block<>(curPattern, tokens, blockStartPos, newPtn == null ? pos : newPtn.calcBlockEndPos(pos)));
				}
				blockStartPos = newPtn == null ? pos : newPtn.calcBlockStartPos(pos);
				curPattern = newPtn;
			}
		}
		if (curPattern != null) {
			blocks.add(new Block<>(curPattern, tokens, blockStartPos, tokens.size() - 1));
		}

		return blocks;
	}

	public static class BlockPattern<T, B> {

		private final B blockType;

		private final T[] startTokens;
		private final T[] endTokens;
		private final T delimiter;

		public BlockPattern(B blockType, T[] startTokens) {
			this(blockType, startTokens, null, null);
		}

		public BlockPattern(B blockType, T[] startTokens, T[] endTokens, T delimiter) {
			this.startTokens = startTokens;
			this.endTokens = endTokens;
			this.delimiter = delimiter;
			this.blockType = blockType;
		}

		public boolean hasDefinedEndTokens() {
			return endTokens != null;
		}

		public boolean checkStart(List<Token<T>> tokens, int offset) {
			if (startTokens == null) {
				return true;
			}
			return checkImpl(startTokens, tokens, offset);
		}

		public boolean checkEnd(List<Token<T>> tokens, int offset) {
			if (offset == tokens.size() - 1) {
				return true;
			}
			int delimOffs = offset - endTokens.length;
			if (delimOffs >= 0) {
				if (tokens.get(delimOffs).type == delimiter) {
					return false;
				}
			}
			return checkImpl(endTokens, tokens, offset);
		}

		public boolean checkImpl(T[] comp, List<Token<T>> tokens, int offset) {
			int pos = offset - (comp.length - 1);
			if (pos >= 0) {
				for (int compIdx = 0; pos <= offset; pos++, compIdx++) {
					if (comp[compIdx] != tokens.get(pos).type) {
						return false;
					}
				}
				return true;
			}
			return false;
		}
		
		private int calcBlockEndPos(int pos) {
			if (endTokens == null) {
				return pos;
			}
			return pos - endTokens.length - 1;
		}

		private int calcBlockStartPos(int pos) {
			if (startTokens == null) {
				return pos;
			}
			return pos - startTokens.length + 1;
		}
	}

	public static class Block<T, B> {

		private final BlockPattern<T, B> pattern;
		
		public final B type;

		public final List<Token<T>> tokens;

		private Block(BlockPattern<T, B> pattern, List<Token<T>> srcTokens, int srcOffset, int srcEndOffset) {
			this.pattern = pattern;
			this.type = pattern.blockType;
			tokens = srcTokens.subList(srcOffset, srcEndOffset + 1);
		}
		
		public List<Token<T>> tokensTrimmed() {
			if (pattern == null) {
				return tokens;
			}
			return tokens.subList(
					pattern.startTokens == null ? 0 : pattern.startTokens.length, 
					tokens.size() - (pattern.endTokens == null ? 0 : pattern.endTokens.length)
			);
		}

		public String tokenContentTrimmed() {
			return Token.join(tokensTrimmed());
		}
		
		@Override
		public String toString() {
			/*StringBuilder sb = new StringBuilder();
			for (Token t : tokens) {
				sb.append(t.toString());
				sb.append('\n');
			}
			return sb.toString();*/
			return "--[" + type + "]--\n" + Token.join(tokens);
		}
	}
}
