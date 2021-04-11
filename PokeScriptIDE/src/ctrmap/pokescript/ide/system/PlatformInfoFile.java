package ctrmap.pokescript.ide.system;

import ctrmap.pokescript.LangPlatform;
import ctrmap.stdlib.formats.yaml.Yaml;
import ctrmap.stdlib.formats.yaml.YamlNode;
import ctrmap.stdlib.fs.FSFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class PlatformInfoFile extends Yaml {

	public List<PlatformInfo> info = new ArrayList<>();

	public PlatformInfoFile(FSFile fsf) {
		super(fsf);

		YamlNode platformInfoList = getRootNodeKeyNode("PlatformInfo");
		for (YamlNode child : platformInfoList.children) {
			info.add(new PlatformInfo(child, this));
		}
	}

	public static class PlatformInfo {

		private PlatformInfoFile f;

		public String name;
		public LangPlatform langPlatform;
		public String SDKInfoPath;
		public String HTMLPagePath;

		public PlatformInfo(YamlNode node, PlatformInfoFile f) {
			this.f = f;
			name = node.getChildByName("Name").getValue();
			langPlatform = LangPlatform.fromEnumName(node.getChildByName("LangPlatform").getValue());
			SDKInfoPath = node.getChildByName("SDKInfo").getValue();
			HTMLPagePath = node.getChildByName("HTMLPage").getValue();
		}

		@Override
		public String toString() {
			return name;
		}

		public String getHTMLDescContent() {
			if (HTMLPagePath != null) {
				FSFile src = f.document.getParent().getChild(HTMLPagePath);
				if (src != null && src.isFile()) {
					return new String(src.getBytes(), StandardCharsets.UTF_8);
				}
			}
			return null;
		}

		public SDKInfoFile getSDKInfoFile() {
			if (SDKInfoPath == null) {
				return null;
			}
			return new SDKInfoFile(f.document.getParent().getChild(SDKInfoPath));
		}
	}
}
