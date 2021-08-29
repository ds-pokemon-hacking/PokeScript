package ctrmap.pokescript.stage0;

import ctrmap.pokescript.LangConstants;
import ctrmap.pokescript.MemberDocumentation;
import ctrmap.pokescript.stage0.content.DeclarationContent;
import ctrmap.pokescript.types.DataType;
import ctrmap.pokescript.types.TypeDef;
import java.util.ArrayList;
import java.util.List;

public class NMember implements IModifiable {

	public MemberDocumentation doc;
	public String name;
	public TypeDef type;
	public DeclarationContent.Argument[] args = new DeclarationContent.Argument[0];
	public List<Modifier> modifiers = new ArrayList<>();

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append(getSimpleName());
		if (!hasModifier(Modifier.VARIABLE)) {
			sb.append("(");
			for (DeclarationContent.Argument a : args) {
				sb.append(a.typeDef.toFriendliestString());
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
	
	public String getSimpleName() {
		return LangConstants.getLastPathElem(name);
	}
	
	public boolean isRecommendedUserAccessible() {
		return !hasModifier(Modifier.INTERNAL);
	}

	@Override
	public List<Modifier> getModifiers() {
		return modifiers;
	}
}
