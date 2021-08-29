package ctrmap.pokescript.ide.autocomplete.nodes;

import ctrmap.pokescript.stage0.Modifier;
import ctrmap.pokescript.stage0.NMember;
import ctrmap.pokescript.ide.TextAreaMarkManager;

public class NodeResultFactory {
	public static NodeResult createNodeResult(AbstractNode node, TextAreaMarkManager marks){
		if (node instanceof MemberNode){
			NMember m = ((MemberNode) node).member;
			if (!m.hasModifier(Modifier.VARIABLE)){
				//This is a method
				return createLinkedMethodNode(m, marks);
			}
		}
		return createSimpleNameNodeResult(node);
	}
	
	private static NodeResult createSimpleNameNodeResult(AbstractNode node){
		NodeResult nr = new NodeResult();
		nr.setText(node.name);//not full
		return nr;
	}
	
	private static NodeResult createLinkedMethodNode(NMember method, TextAreaMarkManager marks){
		NodeResult nr = new NodeResult();
		StringBuilder sb = new StringBuilder();
		
		sb.append(method.getSimpleName());
		sb.append("(");
		int[] argOffsets = new int[method.args.length];
		for (int i = 0; i < method.args.length; i++){
			argOffsets[i] = sb.length();
			sb.append(method.args[i].name);
			if (i != method.args.length - 1){
				sb.append(", ");
			}
		}
		sb.append(")");
		
		//Now generate Links
		//We could do it in the same loop, but this looks cleaner
		for (int i = 0; i < method.args.length; i++){
			NodeResult.Link l = new NodeResult.Link(
					method.args[i], 
					method.args[i].name, 
					argOffsets[i],
					marks
			);
			nr.putLink(l);
		}
		
		nr.setText(sb.toString());
		return nr;
	}
}
