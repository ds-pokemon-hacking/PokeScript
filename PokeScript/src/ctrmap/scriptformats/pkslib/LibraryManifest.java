package ctrmap.scriptformats.pkslib;

import ctrmap.pokescript.LangPlatform;
import ctrmap.stdlib.formats.yaml.Yaml;
import ctrmap.stdlib.formats.yaml.YamlNode;
import ctrmap.stdlib.fs.FSFile;
import java.util.ArrayList;
import java.util.List;

public class LibraryManifest extends Yaml {
	
	public static final String LIBRARY_MANIFEST_NAME = ".manifest";

	public LibraryManifest(FSFile fsf) {
		super(fsf);
	}

	public void setProductId(String pid) {
		getEnsureRootNodeKeyNode(LibraryAttributes.AK_PROD_ID).setValue(pid);
	}

	public String getProductId() {
		return getRootNodeKeyValue(LibraryAttributes.AK_PROD_ID);
	}

	public void setProductName(String name) {
		getEnsureRootNodeKeyNode(LibraryAttributes.AK_PROD_NAME).setValue(name);
	}

	public String getProductName() {
		return getRootNodeKeyValue(LibraryAttributes.AK_PROD_NAME);
	}

	public boolean isMultirelease() {
		return getRootNodeKeyValueBool(LibraryAttributes.AK_MR_ENABLE);
	}

	public List<PlatformSourceTarget> getMultireleaseTargets() {
		List<PlatformSourceTarget> l = new ArrayList<>();
		if (isMultirelease()) {
			YamlNode mr = getRootNodeKeyNode(LibraryAttributes.AK_MR_PATH_LIST);
			for (YamlNode child : mr.children) {
				l.add(new PlatformSourceTarget(child));
			}
		}
		return l;
	}

	public PlatformSourceTarget getMultireleaseTargetForPlatform(LangPlatform plaf) {
		for (PlatformSourceTarget tgt : getMultireleaseTargets()) {
			if (tgt.platform == plaf) {
				return tgt;
			}
		}
		return null;
	}

	public void setSupportedPlatforms(PlatformSourceTarget... targets) {
		if (targets.length > 0) {
			if (targets.length > 1) {
				getEnsureRootNodeKeyNode(LibraryAttributes.AK_MR_ENABLE).setValueBool(true);
				removeRootNodeKeyNode(LibraryAttributes.AK_SR_PLAF);
				YamlNode mrl = getEnsureRootNodeKeyNode(LibraryAttributes.AK_MR_PATH_LIST);

				mrl.children.clear();
				for (PlatformSourceTarget tgt : targets) {
					mrl.addChild(tgt.makeNode());
				}
			} else {
				removeRootNodeKeyNode(LibraryAttributes.AK_MR_PATH_LIST);
				getEnsureRootNodeKeyNode(LibraryAttributes.AK_MR_ENABLE).setValueBool(false);
				getEnsureRootNodeKeyNode(LibraryAttributes.AK_SR_PLAF).setValue(targets[0].platform.toString());
			}
		} else {
			throw new IllegalArgumentException("At least one target platform has to be specified.");
		}
	}

	public LangPlatform getSinglereleaseTargetPlatform() {
		return LangPlatform.fromEnumName(getRootNodeKeyValue(LibraryAttributes.AK_SR_PLAF));
	}

	public boolean isPlatformSupported(LangPlatform platform) {
		if (isMultirelease()) {
			for (PlatformSourceTarget mrt : getMultireleaseTargets()) {
				if (mrt.platform == platform) {
					return true;
				}
			}
		} else {
			return getSinglereleaseTargetPlatform() == platform;
		}
		return false;
	}

	public List<String> getDependencies() {
		YamlNode deps = getRootNodeKeyNode(LibraryAttributes.AK_COMPILE_DEPS);
		if (deps != null) {
			return deps.getChildValuesAsListStr();
		}
		return new ArrayList<>();
	}
}
