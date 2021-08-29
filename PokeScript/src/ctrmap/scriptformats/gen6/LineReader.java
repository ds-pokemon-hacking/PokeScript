package ctrmap.scriptformats.gen6;

import java.util.ArrayDeque;
import java.util.Queue;

public class LineReader {

	public Queue<String> lines = new ArrayDeque<>();

	public LineReader(String str) {
		int index;
		int last = 0;
		while ((index = str.indexOf('\n', last)) != -1) {
			lines.add(str.substring(last, index));
			last = index + 1;
		}
		if (last != str.length()) {
			lines.add(str.substring(last));
		}
	}

	public boolean hasNextLine() {
		return !lines.isEmpty();
	}

	public String nextLine() {
		return lines.poll();
	}

}
