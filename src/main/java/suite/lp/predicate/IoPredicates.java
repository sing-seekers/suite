package suite.lp.predicate;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import suite.lp.predicate.PredicateUtil.BuiltinPredicate;
import suite.node.Int;
import suite.node.Node;
import suite.node.Str;
import suite.node.Tree;
import suite.node.io.Formatter;
import suite.node.util.SuiteException;
import suite.primitive.Bytes.BytesBuilder;
import suite.util.os.FileUtil;
import suite.util.os.LogUtil;

public class IoPredicates {

	public BuiltinPredicate dump = PredicateUtil.run(n -> System.out.print(Formatter.dump(n)));

	public BuiltinPredicate dumpStack = (prover, ps) -> {
		String date = LocalDateTime.now().toString();
		String trace = prover.getTracer().getStackTrace();
		LogUtil.info("-- Stack trace at " + date + " --\n" + trace);
		return true;
	};

	public BuiltinPredicate exec = (prover, ps) -> {
		if (ps instanceof Str)
			try {
				String cmd = ((Str) ps).value;
				return Runtime.getRuntime().exec(cmd).waitFor() == 0;
			} catch (Exception ex) { // IOException or InterruptedException
				LogUtil.error(ex);
			}
		return false;
	};

	public BuiltinPredicate exit = PredicateUtil.run(n -> System.exit(n instanceof Int ? ((Int) n).number : 0));

	public BuiltinPredicate fileExists = PredicateUtil.bool(n -> Files.exists(Paths.get(Formatter.display(n))));

	public BuiltinPredicate fileRead = PredicateUtil.fun(n -> {
		String filename = Formatter.display(n);
		try {
			return new Str(FileUtil.read(filename));
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	});

	public BuiltinPredicate fileWrite = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 2);
		String filename = Formatter.display(params[0]);
		String content = Formatter.display(params[1]);

		try (OutputStream fos = FileUtil.out(filename)) {
			fos.write(content.getBytes(FileUtil.charset));
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}

		return true;
	};

	public BuiltinPredicate homeDir = (prover, ps) -> {
		return prover.bind(new Str(FileUtil.homeDir()), ps);
	};

	public BuiltinPredicate nl = PredicateUtil.run(n -> System.out.println());

	public BuiltinPredicate readLine = (prover, ps) -> {
		try {
			BytesBuilder bb = new BytesBuilder();
			byte b;
			while ((b = (byte) System.in.read()) >= 0 && b != 10)
				bb.append(b);
			String s = new String(bb.toBytes().toBytes(), FileUtil.charset);
			return prover.bind(new Str(s), ps);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	};

	public BuiltinPredicate log = PredicateUtil.run(n -> LogUtil.info(Formatter.dump(n)));

	public BuiltinPredicate sink = (prover, ps) -> {
		prover.config().getSink().sink(ps);
		return false;
	};

	public BuiltinPredicate source = (prover, ps) -> {
		Node source = prover.config().getSource().source();
		return prover.bind(ps, source);
	};

	public BuiltinPredicate throwPredicate = PredicateUtil.run(n -> {
		throw new SuiteException(n.finalNode());
	});

	public BuiltinPredicate write(PrintStream printStream) {
		return PredicateUtil.run(n -> printStream.print(Formatter.display(n)));
	}

}
