package ctrmap.pokescript;

public class CompilerExceptionData {
	public String fileName;
	public int lineNumberStart;
	public int lineNumberEnd;
	public String text;
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		boolean ml = lineNumberEnd != lineNumberStart;
		sb.append("In file: ");
		sb.append(fileName);
		sb.append(": ");
		sb.append("Exception at line");
		if (ml){
			sb.append("s");
		}
		sb.append(" ");
		sb.append(lineNumberStart);
		if (ml){
			sb.append(" to ");
			sb.append(lineNumberEnd);
		}
		sb.append(": ");
		sb.append(text);
		return sb.toString();
	}
}
