package ctrmap.pokescript.ide;

import ctrmap.pokescript.ide.autocomplete.AutoComplete;
import ctrmap.pokescript.ide.autocomplete.AutoCompleteKeyListener;
import ctrmap.pokescript.ide.system.project.IDEFile;
import ctrmap.pokescript.stage0.Preprocessor;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.stdlib.gui.DialogUtils;
import ctrmap.stdlib.gui.components.CaretMotion;
import ctrmap.stdlib.io.base.impl.InputStreamReadable;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
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
					forceReparsing();
					ac.updateByArea();
					ac.attachWindowLayoutToNameAndOpen(CaretMotion.NONE);
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

		getInputMap().put(KeyStroke.getKeyStroke("F5"), "ReloadACHotKey");
		getActionMap().put("ReloadACHotKey", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ac.closeAndInvalidate();
				ac.reloadProject();
			}
		});

		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK), "CloseFileHotKey");
		getActionMap().put("CloseFileHotKey", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ide.closeFileTab(FileEditorRSTA.this);
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

	public void forceReparsing() {
		forceReparsing(parser);
	}

	public SaveResult saveTextToFile(boolean dialog) {
		waitFinishCompilers();
		parser.disable();
		String text = getText();
		if (!text.equals(lastSavedContent)) {
			int rsl = JOptionPane.YES_OPTION;
			if (dialog) {
				rsl = DialogUtils.showSaveConfirmationDialog(ide, file.getName());
			}

			switch (rsl) {
				case JOptionPane.NO_OPTION:
					file.saveNotify(SaveResult.NO_CHANGES);
					parser.enable();
					return SaveResult.NO_CHANGES;
				case JOptionPane.CANCEL_OPTION:
					file.saveNotify(SaveResult.CANCELLED);
					parser.enable();
					return SaveResult.CANCELLED;
			}

			file.setBytes(text.getBytes());

			lastSavedContent = text;
			ide.setFileTabModified(this, false);

			file.saveNotify(SaveResult.SAVED);
			parser.enable();
			return SaveResult.SAVED;
		}
		file.saveNotify(SaveResult.NO_CHANGES);
		parser.enable();
		return SaveResult.NO_CHANGES;
	}

	public void waitFinishCompilers() {
		try {
			parser.currentCompilerThread.join();
		} catch (InterruptedException ex) {
			Logger.getLogger(FileEditorRSTA.class.getName()).log(Level.SEVERE, null, ex);
		}
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
		
		private boolean enabled = true;

		public class CompilerThread extends Thread {

			@Override
			public void run() {
				pp = file.getCompiler();
				pp.read(new InputStreamReadable(new ByteArrayInputStream(getText().getBytes())));
				NCompileGraph cg = pp.getCompileGraph();

				if (ac.isBoundToArea(FileEditorRSTA.this)) {
					ac.rebuildNodeTree(pp);
					SwingUtilities.invokeLater((() -> {
						ide.buildErrorTable(pp);
					}));
				}
			}
		};
		
		public void enable() {
			enabled = true;
		}
		
		public void disable() {
			enabled = false;
		}

		CompilerThread currentCompilerThread = null;

		@Override
		public ParseResult parse(RSyntaxDocument doc, String style) {
			if (!enabled) {
				return new DefaultParseResult(this);
			}
			if (/*file.canWrite()*/true) {
				String text = getText();

				boolean modified = !text.equals(lastSavedContent);
				ide.setFileTabModified(FileEditorRSTA.this, modified);

				if (currentCompilerThread == null || (!currentCompilerThread.isAlive())) {
					currentCompilerThread = new CompilerThread();
					currentCompilerThread.start();
				}
			} else {
				ide.buildErrorTable(null);
			}

			return new DefaultParseResult(this);
		}
	}
}
