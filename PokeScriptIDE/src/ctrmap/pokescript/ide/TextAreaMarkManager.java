package ctrmap.pokescript.ide;

import java.util.ArrayList;
import java.util.List;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class TextAreaMarkManager implements DocumentListener {

	private List<Mark> marks = new ArrayList<>();

	public Mark createMark(int position) {
		Mark m = new Mark(position);
		marks.add(m);
		return m;
	}

	public Mark createSensitiveMark(int position, int endPosition) {
		Mark m = new Mark(position, endPosition, true);
		marks.add(m);
		return m;
	}

	@Override
	public void insertUpdate(DocumentEvent e) {
		for (Mark m : marks) {
			m.update(new SimpleTextEvent(e, false));
		}
		updateMarkList();
	}

	@Override
	public void removeUpdate(DocumentEvent e) {
		for (Mark m : marks) {
			m.update(new SimpleTextEvent(e, true));
		}
		updateMarkList();
	}

	@Override
	public void changedUpdate(DocumentEvent e) {
	}

	private void updateMarkList() {
		for (int i = 0; i < marks.size(); i++) {
			if (!marks.get(i).getIsValid()) {
				marks.remove(i);
				i--;
			}
		}
	}

	public static class Mark {

		private int position;
		private Mark last;
		private boolean valid = true;
		private boolean sensitive = false;
		private boolean focused = false;
		private boolean frozen = false;

		private Mark(int position) {
			this(position, -1, false);
		}

		private Mark(int position, int end, boolean sensitive) {
			this.position = position;
			if (end != -1) {
				this.last = new Mark(end);
			}
			this.sensitive = sensitive && this.last != null;
		}

		public Mark getLast() {
			return last;
		}

		public void focus() {
			focused = true;
		}

		public void loseFocus() {
			focused = false;
		}

		public void freeze() {
			setFrozenState(true);
		}

		public void defrost() {
			setFrozenState(false);
		}

		public void setFrozenState(boolean state) {
			frozen = state;
			if (last != null) {
				last.setFrozenState(state);
			}
		}

		public int getPosition() {
			return position;
		}

		public void forceTranspose(int amount) {
			position += amount;

			if (last != null) {
				last.forceTranspose(amount);
			}
		}

		public boolean getIsValid() {
			boolean v = valid;
			if (last != null) {
				v &= last.getIsValid();
			}
			return v;
		}

		public void discard() {
			valid = false;
		}
		
		public boolean isRange(){
			return last != null;
		}
		
		public boolean isSingleMark(){
			return !isRange();
		}

		public void update(SimpleTextEvent e) {
			if (isRange()) {
				last.update(e);
			}
			if (!frozen) {
				if (isSingleMark() | position != e.offs) {
					if (sensitive && focused) {
						if (e.offs < position || e.offs > last.getPosition()) {
							discard();
							return;
						}
					}
					if (e.getIsRemove() && e.containsPosition(position)) {
						discard();
					} else {
						position = e.getRelocatedPosition(position);
					}
				}
			}
		}
	}

	private static class SimpleTextEvent {

		private int offs;
		private int len;
		private final boolean isRemove;

		public SimpleTextEvent(DocumentEvent e, boolean isRemove) {
			offs = e.getOffset();
			len = e.getLength();
			this.isRemove = isRemove;
		}

		public boolean getIsRemove() {
			return isRemove;
		}

		public int getRelocatedPosition(int pos) {
			if (pos >= offs) {
				pos += isRemove ? -len : len;
			}
			return pos;
		}

		public boolean containsPosition(int pos) {
			int diff = pos - offs;
			return diff > 0 && diff < len;
		}
	}
}
