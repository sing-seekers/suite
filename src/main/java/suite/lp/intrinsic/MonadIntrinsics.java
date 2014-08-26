package suite.lp.intrinsic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import suite.instructionexecutor.ExpandUtil;
import suite.instructionexecutor.IndexedReaderPointer;
import suite.lp.intrinsic.Intrinsics.Intrinsic;
import suite.lp.intrinsic.Intrinsics.IntrinsicBridge;
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

	public Intrinsic popen = (bridge, inputs) -> {
		Fun<Node, Node> unwrapper = bridge::unwrap;
		List<String> list = new ArrayList<>();

		Source<Node> source = ExpandUtil.expandList(unwrapper, inputs.get(0));
		Node node;

		while ((node = source.source()) != null)
			list.add(ExpandUtil.expandString(unwrapper, node));

		Node in = inputs.get(1);

		try {
			Process process = Runtime.getRuntime().exec(list.toArray(new String[list.size()]));

			Node n0 = Intrinsics.wrap(bridge, new Suspend(() -> {
				try {
					return Int.of(process.waitFor());
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			}));

			Node n1 = createReader(bridge, process.getInputStream());
			Node n2 = createReader(bridge, process.getErrorStream());

			// Use a separate thread to write to the process, so that read
			// and write occur at the same time and would not block up.
			// The input stream is also closed by this thread.
			// Have to make sure the executors are thread-safe!
			new Thread(() -> {
				try {
					try (OutputStream pos = process.getOutputStream(); Writer writer = new OutputStreamWriter(pos)) {
						ExpandUtil.expandToWriter(unwrapper, in, writer);
					}

					process.waitFor();
				} catch (Exception ex) {
					LogUtil.error(ex);
				}
			}).start();

			return Tree.of(TermOp.AND___, n0 //
					, Intrinsics.wrap(bridge, Tree.of(TermOp.AND___, n1, n2)));
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	};

	public Intrinsic seq = (bridge, inputs) -> {
		ExpandUtil.expandFully(bridge::unwrap, inputs.get(0));
		return inputs.get(1);
	};

	public Intrinsic source = new Intrinsic() {
		public Node invoke(IntrinsicBridge bridge, List<Node> inputs) {
			IndexedReaderPointer intern = Data.get(inputs.get(0));
			int ch = intern.head();

			// Suspend the right node to avoid stack overflow when input
			// data is very long under eager mode
			if (ch != -1) {
				Node left = Intrinsics.wrap(bridge, Int.of(ch));
				Node right = new Suspend(() -> bridge.wrap(this, new Data<>(intern.tail())));
				return Tree.of(TermOp.OR____, left, right);
			} else
				return Atom.NIL;
		}
	};

	private Node createReader(IntrinsicBridge bridge, InputStream is) {
		InputStreamReader isr = new InputStreamReader(is, FileUtil.charset);
		BufferedReader br = new BufferedReader(isr);
		IndexedReaderPointer irp = new IndexedReaderPointer(br);
		return bridge.wrap(source, new Data<>(irp));
	}

}
