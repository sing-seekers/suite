package suite.editor;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import suite.Suite;
import suite.node.Node;
import suite.node.io.Formatter;
import suite.node.io.PrettyPrinter;
import suite.util.FileUtil;
import suite.util.FunUtil;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.To;

public class EditorController {

	private Thread runThread;

	public void bottom(EditorView view) {
		toggleVisible(view, view.getBottomToolbar());
		view.repaint();
	}

	public void downToSearchList(EditorView view) {
		JList<String> leftList = view.getLeftList();
		DefaultListModel<String> listModel = view.getListModel();

		leftList.requestFocusInWindow();

		if (!listModel.isEmpty())
			leftList.setSelectedValue(listModel.get(0), true);
	}

	public void evaluate(EditorView view) {
		run(view, new Fun<String, String>() {
			public String apply(String text) {
				String result;

				try {
					Node node = Suite.evaluateFun(text, true);
					result = Formatter.dump(node);
				} catch (Exception ex) {
					result = To.string(ex);
				}

				return result;
			}
		});
	}

	public void evaluateType(EditorView view) {
		run(view, new Fun<String, String>() {
			public String apply(String text) {
				String result;

				try {
					Node node = Suite.evaluateFunType(text);
					result = Formatter.dump(node);
				} catch (Exception ex) {
					result = To.string(ex);
				}

				return result;
			}
		});
	}

	public void format(EditorView view) {
		JEditorPane editor = view.getEditor();
		Node node = Suite.parse(editor.getText());
		editor.setText(new PrettyPrinter().prettyPrint(node));
	}

	public void left(EditorView view) {
		JComponent left = view.getLeftToolbar();
		if (toggleVisible(view, left))
			view.getSearchTextField().requestFocusInWindow();
		view.repaint();
	}

	public void newFile(EditorView view) {
		JEditorPane editor = view.getEditor();
		editor.setText("");
		editor.requestFocusInWindow();

		view.getFilenameTextField().setText("pad");
	}

	public void quit(EditorView view) {
		System.exit(0);
	}

	public void right(EditorView view) {
		JComponent right = view.getRightToolbar();
		toggleVisible(view, right);
		view.repaint();
	}

	public void searchFor(EditorView view) {
		JTextField searchTextField = view.getSearchTextField();
		searchTextField.setCaretPosition(0);
		searchTextField.requestFocusInWindow();
	}

	public void searchFiles(EditorView view) {
		DefaultListModel<String> listModel = view.getListModel();
		listModel.clear();

		final String text = view.getSearchTextField().getText();

		if (!text.isEmpty()) {
			Source<File> files0 = FileUtil.findFiles(new File("."));

			Source<String> files1 = FunUtil.map(new Fun<File, String>() {
				public String apply(File file) {
					return file.getPath();
				}
			}, files0);

			Source<String> files2 = FunUtil.filter(new Fun<String, Boolean>() {
				public Boolean apply(String filename) {
					return filename.contains(text);
				}
			}, files1);

			for (String filename : FunUtil.iter(files2))
				listModel.addElement(filename);
		}
	}

	public void selectList(EditorView view) {
		try {
			String filename = view.getLeftList().getSelectedValue();
			String text = To.string(new File(filename));

			view.getFilenameTextField().setText(filename);

			JEditorPane editor = view.getEditor();
			editor.setText(text);
			editor.setCaretPosition(0);
			editor.requestFocusInWindow();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	public void top(EditorView view) {
		JTextField filenameTextField = view.getFilenameTextField();
		if (toggleVisible(view, filenameTextField))
			filenameTextField.setCaretPosition(filenameTextField.getText().length());
		view.repaint();
	}

	public void unixFilter(EditorView view) {
		JFrame frame = view.getFrame();
		JEditorPane editor = view.getEditor();

		String command = JOptionPane.showInputDialog(frame //
				, "Enter command:", "Unix Filter", JOptionPane.PLAIN_MESSAGE);

		try {
			Process process = Runtime.getRuntime().exec(command);

			try (OutputStream pos = process.getOutputStream(); Writer writer = new OutputStreamWriter(pos, FileUtil.charset)) {
				writer.write(editor.getText());
			}

			process.waitFor();

			editor.setText(To.string(process.getInputStream()));
		} catch (IOException | InterruptedException ex) {
			throw new RuntimeException(ex);
		}
	}

	private boolean toggleVisible(EditorView view, JComponent component) {
		boolean visible = !component.isVisible();
		component.setVisible(visible);
		if (visible)
			component.requestFocusInWindow();
		else if (isOwningFocus(component))
			view.getEditor().requestFocusInWindow();
		return visible;
	}

	private boolean isOwningFocus(Component component) {
		boolean isFocusOwner = component.isFocusOwner();
		if (component instanceof JComponent)
			for (Component c : ((JComponent) component).getComponents())
				isFocusOwner |= isOwningFocus(c);
		return isFocusOwner;
	}

	private void run(final EditorView view, final Fun<String, String> fun) {
		JEditorPane editor = view.getEditor();
		String selectedText = editor.getSelectedText();
		final String text = selectedText != null ? selectedText : editor.getText();

		if (runThread == null || !runThread.isAlive()) {
			runThread = new Thread() {
				public void run() {
					JTextArea bottomTextArea = view.getMessageTextArea();
					bottomTextArea.setEnabled(false);
					bottomTextArea.setText("RUNNING...");

					String result = fun.apply(text);

					bottomTextArea.setText(result);
					bottomTextArea.setEnabled(true);
					bottomTextArea.setVisible(true);

					view.repaint();
					view.getEditor().requestFocusInWindow();
				}
			};

			runThread.start();
		} else
			JOptionPane.showMessageDialog(view.getFrame(), "Previous evaluation in progress");
	}

}
