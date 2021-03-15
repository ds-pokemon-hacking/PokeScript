package ctrmap.pokescript.ide.autocomplete.nodes;

import ctrmap.pokescript.stage0.NMember;

public class MemberNode extends AbstractNode{
	public NMember member;
	
	public MemberNode(NMember m){
		super(m.name);
		member = m;
	}
	
	@Override
	public String getPrintableShortName(){
		return member.toString();
	}
}
