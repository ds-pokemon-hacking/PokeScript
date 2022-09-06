package ctrmap.pokescript.ide.autocomplete.nodes;

import ctrmap.pokescript.LangCompiler;
import ctrmap.pokescript.stage0.NMember;
import ctrmap.pokescript.stage0.Preprocessor;
import xstandard.fs.FSFile;
import xstandard.fs.FSUtil;

public class ClassNode extends AbstractNode {

	public ClassNode(FSFile f, LangCompiler.CompilerArguments args) {
		super(FSUtil.getFileNameWithoutExtension(f.getName()));
		Preprocessor reader = new Preprocessor(f, args);
		for (NMember m : reader.getMembers(true)) {
			if (m.isRecommendedUserAccessible()) {
				addChild(new MemberNode(m));
			}
		}
	}
}
