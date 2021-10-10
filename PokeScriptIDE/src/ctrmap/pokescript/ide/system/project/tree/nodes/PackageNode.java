package ctrmap.pokescript.ide.system.project.tree.nodes;

import ctrmap.pokescript.LangConstants;
import ctrmap.pokescript.ide.PSIDE;
import ctrmap.pokescript.ide.PSIDETemplateVar;
import ctrmap.pokescript.ide.forms.NewItemDialog;
import ctrmap.pokescript.ide.forms.settings.project.ProjectSettings;
import ctrmap.pokescript.ide.system.project.IDEFile;
import ctrmap.stdlib.fs.FSFile;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.tree.TreeNode;

public class PackageNode extends IDENodeBase {

	public static int RESID = 2;

	protected IDEFile dir;

	private List<PackageNode> packages = new ArrayList<>();
	private List<ClassNode> classes = new ArrayList<>();

	public PackageNode(PSIDE ide, IDEFile dir) {
		this(ide);
		this.dir = dir;

		addChildrenFromDir(dir);
	}

	protected PackageNode(PSIDE ide) {
		super(ide);
	}

	protected void addChildrenFromDir(IDEFile directory) {
		for (IDEFile child : directory.listFiles()) {
			child.setReadOnly(!directory.canWrite());
			if (child.isDirectory()) {
				packages.add(new PackageNode(ide, child));
			} else {
				if (LangConstants.isLangFile(child.getName())) {
					classes.add(new ClassNode(ide, child));
				}
			}
		}
		packages.sort(new Comparator<PackageNode>() {
			@Override
			public int compare(PackageNode o1, PackageNode o2) {
				return o1.dir.getName().compareTo(o2.dir.getName());
			}
		});
		classes.sort(new Comparator<ClassNode>() {
			@Override
			public int compare(ClassNode o1, ClassNode o2) {
				return o1.getClassFile().getName().compareTo(o2.getClassFile().getName());
			}
		});
		for (PackageNode n : packages) {
			add(n);
		}
		for (ClassNode n : classes) {
			add(n);
		}
	}

	public int getNewPackageIndex(String newPackageName) {
		for (int i = 0; i < packages.size(); i++) {
			if (packages.get(i).dir.getName().compareTo(newPackageName) > 0) {
				return i;
			}
		}
		return packages.size();
	}

	public int getNewClassIndex(String newClassName) {
		for (int i = 0; i < classes.size(); i++) {
			if (classes.get(i).getClassFile().getName().compareTo(newClassName) > 0) {
				return i;
			}
		}
		return classes.size();
	}

	protected boolean isDeletable() {
		return true;
	}
	
	@Override
	public void remove(int childIndex){
		if (childIndex != -1){
			TreeNode child = getChildAt(childIndex);
			if (child instanceof PackageNode){
				packages.remove((PackageNode)child);
			}
			else if (child instanceof ClassNode){
				classes.remove((ClassNode)child);
			}
		}
		super.remove(childIndex);
	}

	@Override
	public void onNodePopupInvoke(MouseEvent evt) {
		if (dir.canWrite()) {
			JMenuItem delete = null;
			if (isDeletable()) {
				delete = createDeleteMenuItem(dir.getName(), (() -> {
					ide.deleteFile(dir);
					ide.getProjectTree().removeFileByNode(this);
				}));
			}
			JMenuItem addNew = createNewMenuItem(dir);

			showPopupMenu(evt, addNew, delete);
		}
	}

	protected JMenuItem createNewMenuItem(IDEFile parentFile) {
		JMenu btnNew = new JMenu("New");
		JMenuItem newClass = new JMenuItem("Class");
		JMenuItem newHeader = new JMenuItem("Header");
		JMenuItem newEnum = new JMenuItem("Enum");
		JMenuItem newPackage = new JMenuItem("Package");
		btnNew.add(newClass);
		btnNew.add(newHeader);
		btnNew.add(newEnum);
		btnNew.addSeparator();
		btnNew.add(newPackage);

		class NewItemActionListener implements ActionListener {

			private final NewItemDialog.IDEFileItemType type;

			public NewItemActionListener(NewItemDialog.IDEFileItemType type) {
				this.type = type;
			}

			@Override
			public void actionPerformed(ActionEvent e) {
				NewItemDialog dlg = new NewItemDialog(ide, true, parentFile, type);
				dlg.setVisible(true);
				IDEFile rsl = dlg.getResult();
				if (rsl != null) {
					switch (type) {
						case HEADER:
						case ENUM:
						case CLASS: {
							String classType = type == NewItemDialog.IDEFileItemType.ENUM ? "enum" : "class";
							
							rsl.setBytes(PSIDE.getTemplateData("Class.pks", 
								new PSIDETemplateVar("CLASSNAME", rsl.getNameWithoutExtension()),
								new PSIDETemplateVar("CLASSTYPE", classType),
								new PSIDETemplateVar("PKGNAME", dir.getClasspathInProject())
							));
							ide.openFile(rsl);
							ClassNode cls = new ClassNode(ide, rsl);
							int idx = getNewClassIndex(rsl.getName());
							ide.getProjectTree().addFileByNode(cls, PackageNode.this, idx + packages.size());
							classes.add(idx, cls);
							break;
						}
						case PACKAGE: {
							rsl.mkdirs();
							int idx = getNewPackageIndex(rsl.getName());
							PackageNode pkg = new PackageNode(ide, rsl);
							ide.getProjectTree().addFileByNode(pkg, PackageNode.this, idx);
							packages.add(idx, pkg);
							break;
						}
					}
				}
			}
		}

		newClass.addActionListener(new NewItemActionListener(NewItemDialog.IDEFileItemType.CLASS));
		newHeader.addActionListener(new NewItemActionListener(NewItemDialog.IDEFileItemType.HEADER));
		newEnum.addActionListener(new NewItemActionListener(NewItemDialog.IDEFileItemType.ENUM));
		newPackage.addActionListener(new NewItemActionListener(NewItemDialog.IDEFileItemType.PACKAGE));

		return btnNew;
	}

	@Override
	public int getIconResourceID() {
		return RESID;
	}

	@Override
	public String getNodeName() {
		return dir.getName();
	}

	@Override
	public String getUniqueName() {
		return getNodeName();
	}
}
