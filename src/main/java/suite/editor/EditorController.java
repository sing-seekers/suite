package suite.editor;

import java.io.File;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;

import suite.Suite;
import suite.node.Node;
import suite.node.io.Formatter;
import suite.util.FileUtil;
import suite.util.FunUtil;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.To;

public class EditorController {

	private Thread runThread;

	public void bottom(EditorView view) {
		JComponent bottom = view.getBottomToolbar();
		bottom.setVisible(!bottom.isVisible());
		view.repaint();
	}

	public void left(EditorView view) {
		JComponent left = view.getLeftToolbar();
		left.setVisible(!left.isVisible());
		view.getLeftTextField().requestFocus();
		view.repaint();
	}

	public void quit(EditorView view) {
		System.exit(0);
	}

	public void right(EditorView view) {
		JComponent right = view.getRightToolbar();
		right.setVisible(!right.isVisible());
		view.repaint();
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

	public void searchFiles(EditorView view) {
		DefaultListModel<String> listModel = view.getListModel();
		listModel.clear();

		final String text = view.getLeftTextField().getText();

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

	public void top(EditorView view) {
		JComponent top = view.getTopToolbar();
		top.setVisible(!top.isVisible());
		view.repaint();
	}

	private void run(final EditorView view, final Fun<String, String> fun) {
		JEditorPane editor = view.getEditor();
		String selectedText = editor.getSelectedText();
		final String text = selectedText != null ? selectedText : editor.getText();

		if (runThread == null || !runThread.isAlive()) {
			runThread = new Thread() {
				public void run() {
					JTextArea bottomTextArea = view.getBottomTextArea();
					bottomTextArea.setEnabled(false);
					bottomTextArea.setText("RUNNING...");

					String result = fun.apply(text);

					bottomTextArea.setText(result);
					bottomTextArea.setEnabled(true);
					bottomTextArea.setVisible(true);

					view.repaint();
					view.getEditor().requestFocus();
				}
			};

			runThread.start();
		} else
			JOptionPane.showMessageDialog(view.getFrame(), "Previous run in progress");
	}

}
