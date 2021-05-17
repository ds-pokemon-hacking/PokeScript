/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ctrmap.pokescript.ide.system;

import ctrmap.pokescript.ide.system.project.IDEContext;
import ctrmap.stdlib.formats.yaml.YamlNode;
import ctrmap.stdlib.fs.FSFile;
import ctrmap.stdlib.fs.accessors.DiskFile;
import ctrmap.stdlib.gui.DialogUtils;
import ctrmap.stdlib.text.FormattingUtils;
import ctrmap.stdlib.net.FileDownloader;
import ctrmap.stdlib.res.ResourceAccess;

/**
 *
 */
public class IDEResourceReference {

	public ResourcePathType pathType;
	public String remoteExtType;
	public String path;

	public IDEResourceReference(ResourcePathType pathType, String path) {
		this(pathType, path, null);
	}

	public IDEResourceReference(ResourcePathType pathType, String path, String remoteExtType) {
		this.path = path;
		this.pathType = pathType;
		this.remoteExtType = remoteExtType;
	}

	public IDEResourceReference(YamlNode mainNode) {
		this(mainNode.getChildByName("ResourceType"), mainNode.getChildByName("ResourcePath"), mainNode.getChildByName("RemoteExtType"));
	}

	public IDEResourceReference(YamlNode ptNode, YamlNode pnode, YamlNode retNode) {
		pathType = ResourcePathType.fromName(ptNode.getValue());
		path = pnode.getValue();
		if (pathType == ResourcePathType.REMOTE_EXT) {
			remoteExtType = retNode.getValue();
		}
	}

	public void addToNode(YamlNode node) {
		node.addChild(new YamlNode("ResourceType", pathType.toString()));
		node.addChild(new YamlNode("ResourcePath", path));
		if (pathType == ResourcePathType.REMOTE_EXT) {
			node.addChild(new YamlNode("RemoteExtType", remoteExtType));
		}
	}

	public FSFile resolve(FSFile parent, IDEContext ctx) {
		switch (pathType) {
			case ON_DISK: {
				FSFile rootChild = parent.getChild(path);
				if (!rootChild.exists()) {
					DiskFile df = new DiskFile(path);
					if (df.exists()) {
						return df;
					}
				} else {
					return rootChild;
				}
				break;
			}
			case INTERNAL:
				return ResourceAccess.getResourceFile(path);
			case REMOTE: {
				try {
					return FileDownloader.downloadToMemory(path);
				} catch (Exception ex) {
					DialogUtils.showErrorMessage("Could not retrieve remote dependency", "The remote dependency referenced by URL " + path + " is not accessible. (" + ex.getClass().getSimpleName() + ")");
				}
				break;
			}
			case REMOTE_EXT:
				return ctx.resolveRemoteExt(this);
		}
		return null;
	}

	@Override
	public String toString() {
		String prefix = "";
		switch (pathType) {
			case INTERNAL:
				prefix = "res:/";
				break;
			case ON_DISK:
			case REMOTE:
				prefix = "";
				break;
			case REMOTE_EXT:
				prefix = remoteExtType + ":/";
				break;
		}
		return prefix + path;
	}

	public String typeToString() {
		if (pathType == ResourcePathType.REMOTE_EXT) {
			return remoteExtType + "(External)";
		}
		return FormattingUtils.getFriendlyEnum(pathType);
	}
}
