package suite.fp.intrinsic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import suite.fp.intrinsic.Intrinsics.Intrinsic;
import suite.fp.intrinsic.Intrinsics.IntrinsicCallback;
import suite.instructionexecutor.IndexedReader;
import suite.instructionexecutor.ThunkUtil;
import suite.node.Atom;
import suite.node.Data;
import suite.node.Int;
import suite.node.Node;
import suite.node.Suspend;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.util.FileUtil;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.LogUtil;

public class MonadIntrinsics {

	private Map<Node, Map<Node, Node>> mutables = new WeakHashMap<>();

	public Intrinsic get = (callback, inputs) -> getFrame(inputs).get(inputs.get(1));

	public Intrinsic popen = (callback, inputs) -> {
		Fun<Node, Node> yawn = callback::yawn;
		List<String> list = new ArrayList<>();

		Source<Node> source = ThunkUtil.evaluateToSource(yawn, inputs.get(0));
		Node node;

		while ((node = source.source()) != null)
			list.add(ThunkUtil.evaluateToString(yawn, node));

		Node in = inputs.get(1);

		try {
			Process process = Runtime.getRuntime().exec(list.toArray(new String[list.size()]));

			Node n0 = Intrinsics.wrap(callback, new Suspend(() -> {
				try {
					return Int.of(process.waitFor());
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			}));

			Node n1 = createReader(callback, process.getInputStream());
			Node n2 = createReader(callback, process.getErrorStream());

			// Use a separate thread to write to the process, so that read
			// and write occur at the same time and would not block up.
			// The input stream is also closed by this thread.
			// Have to make sure the executors are thread-safe!
			new Thread(() -> {
				try {
					try (OutputStream pos = process.getOutputStream(); Writer writer = new OutputStreamWriter(pos)) {
						ThunkUtil.evaluateToWriter(yawn, in, writer);
					}

					process.waitFor();
				} catch (Exception ex) {
					LogUtil.error(ex);
				}
			}).start();

			return Tree.of(TermOp.AND___, n0 //
					, Intrinsics.wrap(callback, Tree.of(TermOp.AND___, n1, n2)));
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	};

	public Intrinsic put = (callback, inputs) -> {
		getFrame(inputs).put(inputs.get(1), inputs.get(2));
		return Atom.NIL;
	};

	public Intrinsic source = new Intrinsic() {
		public Node invoke(IntrinsicCallback callback, List<Node> inputs) {
			IndexedReader.Pointer intern = Data.get(inputs.get(0));
			int ch = intern.head();

			// Suspend the right node to avoid stack overflow when input
			// data is very long under eager mode
			if (ch != -1) {
				Node left = Intrinsics.wrap(callback, Int.of(ch));
				Node right = new Suspend(() -> callback.enclose(this, new Data<>(intern.tail())));
				return Tree.of(TermOp.OR____, left, right);
			} else
				return Atom.NIL;
		}
	};

	private Map<Node, Node> getFrame(List<Node> inputs) {
		Node frame = inputs.get(0);
		return mutables.computeIfAbsent(frame, f -> new HashMap<>());
	}

	private Node createReader(IntrinsicCallback callback, InputStream is) {
		InputStreamReader isr = new InputStreamReader(is, FileUtil.charset);
		BufferedReader br = new BufferedReader(isr);
		IndexedReader.Pointer irp = new IndexedReader(br).pointer();
		return callback.enclose(source, new Data<>(irp));
	}

}
