
package ctrmap.pokescript.ide.autocomplete.nodes;

import ctrmap.pokescript.LangCompiler;
import ctrmap.pokescript.LangConstants;
import ctrmap.stdlib.fs.FSFile;
import java.util.List;

public class PackageNode extends AbstractNode{
		
	public PackageNode(FSFile f, LangCompiler.CompilerArguments args){
		this(f.getName());
		List<FSFile> subs = f.listFiles();
		for (FSFile sub : subs){
			if (sub.isDirectory()){
				addChild(new PackageNode(sub, args));
			}
			else {
				if (LangConstants.isLangFile(sub.getName())){
					addChild(new ClassNode(sub, args));
				}
			}
		}
	}
	
	public PackageNode(String name){
		super(name);
	}
}
