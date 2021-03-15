package ctrmap.pokescript.stage0;

import ctrmap.pokescript.MemberDocumentation;
import ctrmap.pokescript.stage0.content.DeclarationContent;
import ctrmap.pokescript.types.DataType;
import ctrmap.pokescript.types.TypeDef;
import java.util.ArrayList;
import java.util.List;

public class NMember {

	public MemberDocumentation doc;
	public String name;
	public TypeDef type;
	public DeclarationContent.Argument[] args = new DeclarationContent.Argument[0];
	public List<Modifier> modifiers = new ArrayList<>();

	public boolean hasModifier(Modifier m) {
		return modifiers.contains(m);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append(name);
		if (!hasModifier(Modifier.VARIABLE)) {
			sb.append("(");
			for (DeclarationContent.Argument a : args) {
				sb.append(a.typeDef.getClassName());
				sb.append(" ");
				sb.append(a.name);
				sb.append(", ");
			}
			if (args.length > 0) {
				sb.delete(sb.length() - 2, sb.length());
			}
			sb.append(")");
		}
		return sb.toString();
	}
}
