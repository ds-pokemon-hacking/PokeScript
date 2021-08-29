package ctrmap.pokescript;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MemberDocumentation {

	public String header;

	public Map<String, String> params = new LinkedHashMap<>();
	public Map<String, String> exceptions = new LinkedHashMap<>();
	public List<String> returns = new ArrayList<>();

	public MemberDocumentation(String src){
		readDoc(src);
	}
	
	public final void readDoc(String src) {
		if (src.endsWith("*/")){
			src = src.substring(0, src.length() - 2);
		}
		//get params first
		String[] garbo = src.split("@");
		//first is method desc
		header = streamLineComment(garbo[0]);
		for (int i = 1; i < garbo.length; i++) {
			String cmd = getFirstWord(garbo[i]);
			switch (cmd) {
				case "param":
					String prmName = getWord(garbo[i], cmd);
					String prmDesc = streamLineComment(garbo[i].substring(getIdxAfter(garbo[i], prmName)));
					params.put(prmName, prmDesc);
					break;
				case "throws":
					String errName = getWord(garbo[i], cmd);
					String errDesc = streamLineComment(garbo[i].substring(getIdxAfter(garbo[i], errName)));
					exceptions.put(errName, errDesc);
					break;
				case "return":
					returns.add(streamLineComment(garbo[i].substring(getIdxAfter(garbo[i], cmd))));
					break;
			}
		}
	}
	
	public static String getFirstWord(String line) {
		return getWord(line, 0);
	}

	
	public static int getIdxAfter(String src, String afterWhat) {
		int start;
		if (afterWhat != null) {
			start = src.indexOf(afterWhat) + afterWhat.length();
		} else {
			start = 0;
		}
		return start;
	}

	public static String getWord(String line, String lastWord) {
		int start = getIdxAfter(line, lastWord);
		return getWord(line, start);
	}

	public static String getWord(String line, int start) {
		StringBuilder sb = new StringBuilder();
		boolean started = false;

		for (int i = start; i < line.length(); i++) {
			char c = line.charAt(i);
			if (Character.isLetterOrDigit(c) || c == '_' || c == '.') {
				started = true;
				sb.append(c);
			} else {
				if (started) {
					break;
				}
			}
		}
		return sb.toString().trim();
	}

	public static String streamLineComment(String com) {
		String s = com.trim(); //beginning and end white spaces are unnecessary
		StringBuilder sb = new StringBuilder();

		//find first alphabetic
		int i = 0;
		while (i < s.length()) {
			char c = s.charAt(i);
			if (!(Character.isWhitespace(c) || c == '*')) { //eliminate * at /** comments
				break;
			}
			i++;
		}

		MainLoop:
		for (; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '\n' || c == '\r') { // we need to check newline for both Linux and Windows boyz
				while (i < s.length()) {
					c = s.charAt(i);
					if (Character.isWhitespace(c) || c == '*') { //eliminate * at /** comments
						i++;
						if (i >= s.length()) {
							break MainLoop;
						}
					} else {
						break;
					}
				}
			}
			sb.append(c);
		}
		return sb.toString();
	}
}
