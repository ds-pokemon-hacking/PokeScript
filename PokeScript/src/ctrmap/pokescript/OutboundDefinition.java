package ctrmap.pokescript;

import ctrmap.pokescript.expr.Throughput;

public class OutboundDefinition {

	public String name;
	public Throughput[] args;

	public OutboundDefinition(String name, Throughput[] args) {
		this.name = name;
		this.args = args;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(name);
		sb.append("(");
		if (args.length > 0) {
			for (Throughput t : args) {
				if (t == null) {
					sb.append("[ERROR]");
				} else {
					sb.append(t.type);
				}
				sb.append(", ");
			}
			sb.delete(sb.length() - 2, sb.length());
		}
		sb.append(")");
		return sb.toString();
	}
}
