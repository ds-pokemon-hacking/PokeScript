package ctrmap.pokescript.ide.system;

import ctrmap.pokescript.ide.system.project.include.Dependency;
import ctrmap.pokescript.ide.system.project.include.DependencyType;
import ctrmap.stdlib.formats.yaml.Yaml;
import ctrmap.stdlib.formats.yaml.YamlNode;
import ctrmap.stdlib.fs.FSFile;
import ctrmap.stdlib.fs.accessors.DiskFile;
import java.util.ArrayList;
import java.util.List;

public class SDKInfoFile extends Yaml {

	public List<SDKInfo> info = new ArrayList<>();

	public SDKInfoFile(FSFile fsf) {
		super(fsf);
		YamlNode infoList = getRootNodeKeyNode("SDKBundles");
		for (YamlNode ch : infoList.children) {
			info.add(new SDKInfo(ch, this));
		}
	}

	public static class SDKInfo {

		private SDKInfoFile f;

		public String name;
		public IDEResourceReference ref;
		public List<String> compilerDefinitions = new ArrayList<>();

		public SDKInfo(String name, IDEResourceReference ref, SDKInfoFile f) {
			this.name = name;
			this.ref = ref;
			this.f = f;
		}

		public SDKInfo(YamlNode node, SDKInfoFile f) {
			name = node.getChildByName("Name").getValue();
			ref = new IDEResourceReference(node);
			YamlNode defList = node.getChildByName("Definitions");
			if (defList != null) {
				for (YamlNode def : defList.children) {
					compilerDefinitions.add(def.getValue());
				}
			}
			this.f = f;
		}

		public Dependency resolveToDependency() {
			Dependency dep = new Dependency(DependencyType.LIBRARY);

			String path = ref.path;
			if (ref.pathType == ResourcePathType.INTERNAL) {
				path = f.document.getParent().getPath() + "/" + path;
			} else if (ref.pathType == ResourcePathType.ON_DISK) {
				DiskFile rel = new DiskFile(f.document.getParent().getPath() + "/" + path);
				if (!rel.exists()) {
					rel = new DiskFile(path);
				}
				path = rel.getPath();
			}
			dep.ref = new IDEResourceReference(ref.pathType, path, ref.remoteExtType);
			return dep;
		}

		@Override
		public String toString() {
			return name;
		}
	}
}
