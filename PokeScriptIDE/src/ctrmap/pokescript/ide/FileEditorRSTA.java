package ctrmap.pokescript.ide;

import ctrmap.pokescript.ide.autocomplete.AutoComplete;
import ctrmap.pokescript.ide.autocomplete.AutoCompleteKeyListener;
import ctrmap.pokescript.ide.system.project.IDEFile;
import ctrmap.pokescript.stage0.Preprocessor;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.stdlib.gui.DialogUtils;
import ctrmap.stdlib.io.base.impl.InputStreamReadable;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import static org.fife.ui.rsyntaxtextarea.SyntaxConstants.SYNTAX_STYLE_PP;
import org.fife.ui.rsyntaxtextarea.parser.AbstractParser;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParseResult;
import org.fife.ui.rsyntaxtextarea.parser.ParseResult;
import org.fife.ui.rtextarea.RTextScrollPane;

public class FileEditorRSTA extends RSyntaxTextArea {

	private PSIDE ide;
	private AutoComplete ac;

	private String lastSavedContent;
	private IDEFile file;

	private TextAreaMarkManager marks = new TextAreaMarkManager();
	private List<CustomHighLight> customHLs = new ArrayList<>();

	private RTextScrollPane scrollPane;

	private PPParser parser;

	public FileEditorRSTA(PSIDE ide, IDEFile file) {
		super();
		this.ide = ide;
		this.file = file;
		this.ac = ide.getAutoCompletionEngine();

		setEditable(file.canWrite());

		if (!isEditable()) {
			setBackground(new Color(220, 220, 220));
		}

		scrollPane = new RTextScrollPane(this, true);

		setSyntaxEditingStyle(SYNTAX_STYLE_PP);
		parser = new PPParser();
		addParser(parser);
		getDocument().addDocumentListener(ac.getACDocListener());
		getDocument().addDocumentListener(marks);

		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, KeyEvent.CTRL_DOWN_MASK), "AutoCompleteHotkey");
		getActionMap().put("AutoCompleteHotkey", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (ac.isBoundToArea(FileEditorRSTA.this)) {
					ac.updateByArea();
					ac.attachWindowLayoutToNameAndOpen();
				}
			}
		});

		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "EndACHotKey");
		getActionMap().put("EndACHotKey", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ac.closeAndInvalidate();
			}
		});

		addCaretListener(new CaretListener() {
			@Override
			public void caretUpdate(CaretEvent e) {
				if (ac.isBoundToArea(FileEditorRSTA.this)) {
					if (!ac.isCaretInAC()) {
						ac.close();
					}
				}
			}
		});
		addKeyListener(new AutoCompleteKeyListener(ac));

		reloadFromFile();
	}

	public SaveResult saveTextToFile(boolean dialog) {
		String text = getText();
		if (!text.equals(lastSavedContent)) {
			int rsl = JOptionPane.YES_OPTION;
			if (dialog) {
				rsl = DialogUtils.showSaveConfirmationDialog(ide, file.getName());
			}

			switch (rsl) {
				case JOptionPane.NO_OPTION:
					return SaveResult.NO_CHANGES;
				case JOptionPane.CANCEL_OPTION:
					return SaveResult.CANCELLED;
			}

			file.setBytes(text.getBytes());

			lastSavedContent = text;
			ide.setFileTabModified(this, false);

			file.saveNotify();

			return SaveResult.SAVED;
		}
		return SaveResult.NO_CHANGES;
	}

	public void reloadFromFile() {
		lastSavedContent = new String(file.getBytes());

		setText(lastSavedContent);
		
		discardAllEdits();
	}

	public IDEFile getEditedFile() {
		return file;
	}

	public RTextScrollPane getScrollPane() {
		return scrollPane;
	}

	public TextAreaMarkManager getMarkManager() {
		return marks;
	}

	public void clearCustomHighlights() {
		customHLs.clear();
	}

	public void addCustomHighlight(TextAreaMarkManager.Mark beginChar, TextAreaMarkManager.Mark endChar, Color color) {
		customHLs.add(new CustomHighLight(beginChar, endChar, color));
	}

	public void addCustomHighlight(CustomHighLight hl) {
		customHLs.add(hl);
	}

	public void addAllCustomHighlights(Collection<CustomHighLight> c) {
		customHLs.addAll(c);
	}

	public void removeAllCustomHighlights(Collection<CustomHighLight> c) {
		customHLs.removeAll(c);
	}

	public void removeCustomHighlight(CustomHighLight hl) {
		customHLs.remove(hl);
	}

	public void publishErrorTable() {
		ide.buildErrorTable(parser.pp);
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		for (CustomHighLight hl : customHLs) {
			try {
				Rectangle start = modelToView(hl.start.getPosition());
				Rectangle end = modelToView(hl.end.getPosition());
				g.setColor(hl.color);
				g.drawRect(start.x, start.y, end.x - start.x, start.height - 2);
			} catch (BadLocationException ex) {
				Logger.getLogger(FileEditorRSTA.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}

	public static enum SaveResult {
		SAVED,
		NO_CHANGES,
		CANCELLED
	}

	public static class CustomHighLight {

		public final Color color;
		public final TextAreaMarkManager.Mark start;
		public final TextAreaMarkManager.Mark end;

		public CustomHighLight(TextAreaMarkManager.Mark beginChar, TextAreaMarkManager.Mark endChar, Color color) {
			start = beginChar;
			end = endChar;
			this.color = color;
		}
	}

	public class PPParser extends AbstractParser {

		public Preprocessor pp;

		@Override
		public ParseResult parse(RSyntaxDocument doc, String style) {
			if (/*file.canWrite()*/true) {
				String text = getText();

				boolean modified = !text.equals(lastSavedContent);
				ide.setFileTabModified(FileEditorRSTA.this, modified);

				pp = file.getCompiler();
				pp.read(new InputStreamReadable(new ByteArrayInputStream(text.getBytes())));
				NCompileGraph cg = pp.getCompileGraph();

				if (cg != null) {
					ac.rebuildNodeTree(pp);
				}
				ide.buildErrorTable(pp);
			} else {
				ide.buildErrorTable(null);
			}

			return new DefaultParseResult(this);
		}
	}
}
