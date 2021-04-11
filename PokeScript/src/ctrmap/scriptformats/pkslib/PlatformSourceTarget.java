package ctrmap.scriptformats.pkslib;

import ctrmap.pokescript.LangPlatform;
import ctrmap.stdlib.formats.yaml.KeyValuePair;
import ctrmap.stdlib.formats.yaml.YamlListElement;
import ctrmap.stdlib.formats.yaml.YamlNode;

public class PlatformSourceTarget {
	public LangPlatform platform;
	public String path;
	
	public PlatformSourceTarget(LangPlatform platform, String pathname){
		this.platform = platform;
		this.path = pathname;
	}
	
	public PlatformSourceTarget(YamlNode node){
		if (node.content != null && node.content instanceof KeyValuePair){
			platform = LangPlatform.fromEnumName(node.getKey());
			path = node.getValue();
		}
		else {
			System.err.println("Invalid multirelease target - must be a key/value pair.");
		}
	}
	
	public YamlNode makeNode(){
		YamlNode n = new YamlNode();
		n.content = new YamlListElement(n);
		n.addChild(new YamlNode(platform.toString(), path));
		return n;
	}
}
