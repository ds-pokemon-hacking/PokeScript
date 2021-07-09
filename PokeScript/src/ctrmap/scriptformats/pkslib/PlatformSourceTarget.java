package ctrmap.scriptformats.pkslib;

import ctrmap.pokescript.LangPlatform;
import ctrmap.stdlib.formats.yaml.YamlListElement;
import ctrmap.stdlib.formats.yaml.YamlNode;

public class PlatformSourceTarget {

	public LangPlatform platform;
	public String path;

	public PlatformSourceTarget(LangPlatform platform, String pathname) {
		this.platform = platform;
		this.path = pathname;
	}

	public PlatformSourceTarget(YamlNode node) {
		platform = LangPlatform.fromEnumName(node.getKey());
		path = node.getValue();
	}

	public YamlNode makeNode() {
		YamlNode n = new YamlNode();
		n.content = new YamlListElement(n);
		n.addChild(new YamlNode(platform.toString(), path));
		return n;
	}
}
