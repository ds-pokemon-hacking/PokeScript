package ctrmap.pokescript.ide.autocomplete;

import ctrmap.pokescript.LangCompiler;
import ctrmap.pokescript.LangConstants;
import ctrmap.pokescript.stage0.NMember;
import ctrmap.pokescript.stage0.Preprocessor;
import xstandard.fs.FSFile;
import ctrmap.pokescript.ide.FileEditorRSTA;
import ctrmap.pokescript.ide.autocomplete.gui.ACDocWindow;
import ctrmap.pokescript.ide.autocomplete.gui.ACLayoutScout;
import ctrmap.pokescript.ide.autocomplete.gui.ACMainWindow;
import ctrmap.pokescript.ide.autocomplete.nodes.AbstractNode;
import ctrmap.pokescript.ide.autocomplete.nodes.ClassNode;
import ctrmap.pokescript.ide.autocomplete.nodes.MemberNode;
import ctrmap.pokescript.ide.autocomplete.nodes.NodeResult;
import ctrmap.pokescript.ide.autocomplete.nodes.NodeResultFactory;
import ctrmap.pokescript.ide.autocomplete.nodes.PackageNode;
import ctrmap.pokescript.ide.system.project.IDEProject;
import ctrmap.pokescript.stage0.Statement;
import xstandard.gui.components.CaretMotion;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;

public class AutoComplete {

	private ACMainWindow acMainWindow;
	private ACDocWindow acDocWindow = new ACDocWindow();

	public static final String AC_ROOT_NODE_DUMMY_NAME = "";

	private AbstractNode acRoot = new PackageNode(AC_ROOT_NODE_DUMMY_NAME);

	public List<FSFile> includes = new ArrayList<>();

	private FileEditorRSTA area;
	private IDEProject currentProject = null;

	private AutoCompleteDocumentListener documentListener;

	public AutoComplete() {
		acMainWindow = new ACMainWindow(acDocWindow);
		acMainWindow.addAcHintListListener(new SelectionListener());
		documentListener = new AutoCompleteDocumentListener();
	}

	public AutoCompleteDocumentListener getACDocListener() {
		return documentListener;
	}

	public ACMainWindow getMainWindow() {
		return acMainWindow;
	}

	public void attachTextArea(FileEditorRSTA area) {
		if (this.area != null) {
			this.area.getDocument().removeDocumentListener(documentListener);
		}
		closeAndInvalidate();
		this.area = area;
		if (area != null) {
			area.getDocument().addDocumentListener(documentListener);
			loadProject(area.getEditedFile().getProject());
		}
	}

	private Stack<NodeResult.Handler> resultHandlers = new Stack<>();

	public boolean isHandlingResult() {
		return !resultHandlers.isEmpty();
	}

	private List<FileEditorRSTA.CustomHighLight> rslHandlerHighLights = new ArrayList<>();

	public void setResultHandlerHighlightBoxesRender() {
		if (area != null) {
			area.removeAllCustomHighlights(rslHandlerHighLights);
			rslHandlerHighLights.clear();

			if (getCurrentRslHandler() != null) {
				rslHandlerHighLights.addAll(getCurrentRslHandler().getHighLights());
				area.addAllCustomHighlights(rslHandlerHighLights);
			}

			area.repaint();
		}
	}

	public NodeResult.Handler getCurrentRslHandler() {
		if (isHandlingResult()) {
			return resultHandlers.peek();
		}
		return null;
	}

	public boolean processEnterKeyAction() {
		if (isVisible()) {
			commitAutocompleteFromList();
		} else if (isHandlingResult()) {
			if (!isCaretInACLine()) {
				return false;
			}
			if (!cancelResultHandlerIfMarksInvalidated()) {
				incrementLink();
			} else {
				return false;
			}
		}
		return true;
	}

	public void commitAutocompleteFromList() {
		if (area != null) {
			AbstractNode n = acMainWindow.getSelectedNode();

			if (n != null) {
				NodeResult.Handler oldHandler = getCurrentRslHandler();
				if (oldHandler != null) {
					if (oldHandler.getCurrentLink() != null) {
						//oldHandler.getCurrentLink().setFocused(false);
					}
				}

				NodeResult rsl = NodeResultFactory.createNodeResult(n, area.getMarkManager());
				NodeResult.Handler newHandler = rsl.createHandler();
				resultHandlers.push(newHandler);

				int cp = area.getCaretPosition();
				newHandler.transpose(area.getText(), cp);
				rsl.freezeLinks();
				String insert = /*newHandler.getFullTextMinusCommonLength()*/ rsl.getText();
				int posAfter = cp + insert.length();
				area.replaceRange(insert, cp - newHandler.tpCommonLength, cp); //WARNING:The method actually ignores the range. If it was ever to be fixed, the following line has to be uncommented.
				//area.insert(newHandler.getFullTextMinusCommonLength(), cp);
				area.setCaretPosition(cp + (insert.length() - newHandler.tpCommonLength)); //replaceRange in RSTA is broken...

				String line = getCurrentLineToPos(cp).trim();
				boolean allowSemicolon = false;
				if (line.startsWith(Statement.IMPORT.getDeclarationStr())) {
					allowSemicolon = true;
					for (AbstractNode ch : n.children) {
						if (!(ch instanceof MemberNode)) {
							allowSemicolon = false;
							break;
						}
					}
				}

				if (allowSemicolon && (posAfter >= area.getDocument().getLength() || area.getText().charAt(posAfter) == '\n')) {
					//If the node has no children (==no more autocomplete) and we are at the end of the line, insert a semicolon
					area.insert(";", posAfter);
				}
				rsl.defrostLinks();
				if (newHandler.hasLinks()) {
					incrementLink();
				} else {
					popResultHandler();
				}
			}
		}
	}

	public void incrementLink() {
		NodeResult.Handler h = getCurrentRslHandler();
		if (h != null) {
			NodeResult.Link l = h.nextLink();
			if (l != null) {
				if (area != null) {
					area.setSelectionStart(l.getOffset());
					area.setSelectionEnd(l.getOffset() + l.getLength());
				}
			} else {
				popResultHandler();
				if (getCurrentRslHandler() != null) {
					incrementLink();
				} else {
					traverseToNextWhitespace();
				}
			}
		}
		setResultHandlerHighlightBoxesRender();
	}

	public boolean cancelResultHandlerIfMarksInvalidated() {
		for (NodeResult.Handler h : resultHandlers) {
			if (h.isLinkChainBroken()) {
				resultHandlers.clear();
				setResultHandlerHighlightBoxesRender();
				return true;
			}
		}
		return false;
	}

	public void popResultHandler() {
		if (!resultHandlers.isEmpty()) {
			resultHandlers.pop();
			setResultHandlerHighlightBoxesRender();
		}
	}

	public void cancelAllResultHandlers() {
		if (!resultHandlers.isEmpty()) {
			resultHandlers.clear();

			traverseToNextWhitespace();

			setResultHandlerHighlightBoxesRender();
		}
	}

	public void traverseToNextWhitespace() {
		if (area != null) {
			int pos = area.getCaretPosition();
			String txt = area.getText();
			while (pos < txt.length() && !Character.isWhitespace(txt.charAt(pos))) {
				pos++;
			}
			area.setSelectionStart(pos);
			area.setSelectionEnd(pos);
		}
	}

	public void loadProject(IDEProject project) {
		if (currentProject != project) {
			currentProject = project;
			acRoot.children.clear();
			for (FSFile fsf : project.getAllIncludeFiles()) {
				addInclude(fsf, project);
			}
		}
	}

	public void reloadProject() {
		if (currentProject != null) {
			IDEProject p = currentProject;
			currentProject = null;
			loadProject(p);
		}
	}

	public void addInclude(FSFile f, IDEProject proj) {
		LangCompiler.CompilerArguments cfg = proj.getCompilerArguments();
		if (f.isDirectory()) {
			for (FSFile c : f.listFiles()) {
				if (c.isDirectory()) {
					acRoot.addChildUnbound(new PackageNode(c, cfg));
				} else {
					acRoot.addChildUnbound(new ClassNode(c, cfg));
				}
			}
		} else {
			if (LangConstants.isLangFile(f.getName())) {
				acRoot.addChildUnbound(new ClassNode(f, cfg));
			} else {
				throw new UnsupportedOperationException("Cannot explicitly include a non-language file. (" + f + ")");
			}
		}
	}

	private List<AbstractNode> localNodes = new ArrayList<>();
	private Map<AbstractNode, String> localAliases = new HashMap<>();

	public synchronized void rebuildNodeTree(Preprocessor script) {
		if (script == null || script.cg == null) {
			return;
		}
		acRoot.children.removeAll(localNodes);
		localNodes.clear();
		for (NMember member : script.getMembers()) {
			if (member.isRecommendedUserAccessible()) {
				localNodes.add(new MemberNode(member));
			}
		}
		for (AbstractNode n : localNodes) {
			acRoot.addChildUnbound(n);
		}
		List<AbstractNode> toRelocate = new ArrayList<>();
		for (String ns : script.cg.importedNamespaces) {
			AbstractNode n = acRoot.findNode(ns);
			if (n != null) {
				n.removeAlias(localAliases.get(n));
				toRelocate.add(n);
			}
		}
		localAliases.clear();	//we have to run the above loop practically twice to ensure stuff does not get left over when deleted
		for (AbstractNode n : toRelocate) {
			String alias = n.name;	//this is really all that's needed for our imports
			n.addAlias(alias);
			localAliases.put(n, alias);
		}
	}

	public boolean isHintRoot() {
		return acMainWindow.isHintRoot();
	}

	private int acAreaStart = 0;
	private int acAreaEnd = 0;

	public boolean isCaretInAC() {
		int cp = area.getCaretPosition();
		return cp >= acAreaStart && cp <= acAreaEnd;
	}

	private String lastQuery = null;

	public String getLastQuery() {
		return lastQuery;
	}

	public void updateByArea() {
		updateByArea(CaretMotion.NONE);
	}

	public void updateByArea(CaretMotion motion) {
		if (area != null) {
			int caretPos = area.getCaretPosition() + getCaretMotIncrement(motion);
			if (caretPos < 0) {
				return;
			}

			updateAutocomplete(caretPos);
		}
	}

	public boolean isBoundToArea(FileEditorRSTA area) {
		return this.area == area;
	}

	public void updateAutocomplete(int caretPosition) {
		if (area != null) {
			String line = getCurrentLineToPos(caretPosition);
			String query = AbstractNode.lastNameToLowerCase(doBackwardsScanUntilNonName(line, line.length()));
			lastQuery = query;

			acAreaEnd = caretPosition;
			acAreaStart = acAreaEnd - query.length();

			//We now have a query that is only the name, nothing else
			List<AbstractNode> hints = acRoot.getRecommendations(query);

			if (line.trim().startsWith(Statement.IMPORT.getDeclarationStr())) {
				//Import - remove members, which Pokescript can't import
				for (int i = 0; i < hints.size(); i++) {
					if (hints.get(i) instanceof MemberNode) {
						hints.remove(i);
						i--;
					}
				}
			}

			acMainWindow.buildList(hints);
		}
	}

	private String getCurrentLineToPos(int pos) {
		try {
			int lso = area.getLineStartOffset(area.getCaretLineNumber());
			if (pos - lso <= 0) {
				return "";
			}
			String line = area.getText(lso, pos - lso);
			return line;
		} catch (BadLocationException ex) {
			Logger.getLogger(AutoComplete.class.getName()).log(Level.SEVERE, null, ex);
		}
		return "";
	}

	public static int getCaretMotIncrement(CaretMotion motion) {
		int caretPos = 0;
		if (motion == CaretMotion.FORWARD) {
			caretPos++;
		} else if (motion == CaretMotion.BACKWARD) {
			caretPos -= 2;
		}
		return caretPos;
	}

	public void attachWindowLayoutToNameAndOpen(CaretMotion caretMotion) {
		//Scroll back through the text until a nonalphanumeric character or non_ is found (including dots, yeah)
		String text = area.getText();
		int idx = Math.min(text.length() - 1, area.getCaretPosition() - 1 + getCaretMotIncrement(caretMotion));
		for (; idx >= 0; idx--) {
			char c = text.charAt(idx);
			if (!(Character.isAlphabetic(c) || c == '_')) {
				idx++;
				break;
				//we do want it one character << to warrant for the icons in the labels
			}
		}
		acMainWindow.wOpen();
		ACLayoutScout.setUpLayout(acMainWindow, acDocWindow, getOSLOfChar(idx), area.getFont().getSize());
	}

	public void updateACContentsAndLocation() {
		updateAutocomplete(area.getCaretPosition());
		attachWindowLayoutToNameAndOpen(CaretMotion.NONE);
	}

	private Point getOSLOfChar(int charIndex) {
		try {
			Rectangle loc = area.modelToView(Math.max(0, Math.min(area.getDocument().getLength(), charIndex)));
			Point p = translatePointToAbsolute(loc.getLocation());
			return p;
		} catch (BadLocationException ex) {
			Logger.getLogger(AutoComplete.class.getName()).log(Level.SEVERE, null, ex);
			return null;
		}
	}

	private Point getCaretLocation() {
		Point loc = area.getCaret().getMagicCaretPosition();
		if (loc == null) {
			loc = new Point(0, 0);
		}
		Point locPass = new Point(loc);
		return translatePointToAbsolute(loc);
	}

	private Point translatePointToAbsolute(Point p) {
		p.translate(area.getLocationOnScreen().x, area.getLocationOnScreen().y);
		return p;
	}

	public void close() {
		acMainWindow.wClose();
		acDocWindow.wClose();
	}

	public void closeAndInvalidate() {
		close();
		cancelAllResultHandlers();
	}

	public boolean isVisible() {
		return acMainWindow.isVisible();
	}

	public boolean isCaretInACLine() {
		if (area != null) {
			int caretLineNo = area.getCaretLineNumber();
			NodeResult.Handler hnd = getCurrentRslHandler();
			if (hnd != null) {
				int off = hnd.getFirstOffset();
				if (off != -1) {
					try {
						return area.getLineOfOffset(off) == caretLineNo;
					} catch (BadLocationException ex) {
						return false;
					}
				}
			}
		}
		return false;
	}

	public static String doBackwardsScanUntilNonName(String s, int index) {
		StringBuilder sb = new StringBuilder();
		for (int i = index - 1; i >= 0; i--) {
			char c = s.charAt(i);
			if (Character.isLetterOrDigit(c) || LangConstants.allowedNonAlphaNumericNameCharacters.contains(c)) {
				sb.insert(0, c);
			} else {
				break;
			}
		}
		return sb.toString();
	}

	private static String getNameWithoutNamespace(String name) {
		int liodot = name.lastIndexOf('.');
		return liodot != -1 ? name.substring(liodot + 1) : name;
	}

	public class SelectionListener implements ListSelectionListener {

		@Override
		public void valueChanged(ListSelectionEvent e) {
			if (!e.getValueIsAdjusting()) {
				if (isVisible()) {
					AbstractNode n = acMainWindow.getSelectedNode();
					acDocWindow.buildDoc(n);
					acDocWindow.wOpen();
				}
			}
		}
	}

	public class AutoCompleteDocumentListener implements DocumentListener {

		@Override
		public void insertUpdate(DocumentEvent e) {
			try {
				String text = e.getDocument().getText(e.getOffset(), e.getLength());
				if (text.endsWith(".")) {
					updateByArea(CaretMotion.FORWARD);
					attachWindowLayoutToNameAndOpen(CaretMotion.FORWARD);
				}
				if (isVisible()) {
					updateByArea(CaretMotion.FORWARD);
					if (isHintRoot() && getLastQuery().length() == 0) {
						close();
					}
				}
				if (getCurrentRslHandler() != null) {
					cancelResultHandlerIfMarksInvalidated();
				}
			} catch (BadLocationException ex) {
				Logger.getLogger(AutoCompleteDocumentListener.class.getName()).log(Level.SEVERE, null, ex);
			}
		}

		@Override
		public void removeUpdate(DocumentEvent e) {
			close();
		}

		@Override
		public void changedUpdate(DocumentEvent e) {
		}
	}
}
