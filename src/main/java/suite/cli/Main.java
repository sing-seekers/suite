package suite.cli;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import suite.util.FileUtil;
import suite.util.FunUtil.Source;
import suite.util.LogUtil;
import suite.util.ParseUtil;
import suite.util.To;
import suite.util.Util;
import suite.util.Util.ExecutableProgram;

/**
 * Logic interpreter and functional interpreter. Likes Prolog and Haskell.
 *
 * @author ywsing
 */
public class Main extends ExecutableProgram {

	private CommandOptions opt;
	private CommandDispatcher dispatcher;

	private Reader reader = new BufferedReader(new InputStreamReader(System.in, FileUtil.charset));
	private Writer writer = new BufferedWriter(new OutputStreamWriter(System.out, FileUtil.charset));

	public static void main(String args[]) {
		Util.run(Main.class, args);
	}

	protected boolean run(String args[]) throws IOException {
		opt = new CommandOptions();
		dispatcher = new CommandDispatcher(opt);

		boolean result = true;
		List<String> inputs = new ArrayList<>();
		Source<String> source = To.source(args);
		String verb = null;
		String arg;

		while ((arg = source.source()) != null)
			if (arg.startsWith("-"))
				if (Util.stringEquals(arg, "--file"))
					inputs.add(readScript(source.source()));
				else
					result &= opt.processOption(arg, source);
			else if (verb == null)
				verb = arg;
			else
				inputs.add(arg);

		if (result)
			if (Util.stringEquals(verb, "evaluate"))
				result &= dispatcher.dispatchEvaluate(inputs);
			else if (Util.stringEquals(verb, "filter"))
				result &= dispatcher.dispatchFilter(inputs, reader, writer);
			else if (Util.stringEquals(verb, "precompile"))
				result &= dispatcher.dispatchPrecompile(inputs);
			else if (Util.stringEquals(verb, "precompile-all"))
				result &= dispatcher.dispatchPrecompileAll(inputs);
			else if (Util.stringEquals(verb, "prove"))
				result &= dispatcher.dispatchProve(inputs);
			else if (Util.stringEquals(verb, "query"))
				result &= runInteractive(inputs);
			else if (Util.stringEquals(verb, "type"))
				result &= dispatcher.dispatchType(inputs);
			else if (verb == null)
				result &= runInteractive(inputs);
			else
				throw new RuntimeException("Unknown action " + verb);

		return result;
	}

	private String readScript(String filename) throws IOException {
		String contents = FileUtil.read(filename);
		if (contents.startsWith("#")) // Skips first line comment
			contents = contents.substring(contents.indexOf('\n') + 1);
		return contents;
	}

	private boolean runInteractive(List<String> filenames) throws IOException {
		BufferedReader br = new BufferedReader(reader);
		boolean code = true;

		code &= dispatcher.importFiles(filenames);
		opt.prompt().println("READY");

		while (true)
			try {
				StringBuilder sb = new StringBuilder();
				String line;

				do {
					opt.prompt().print(sb.length() == 0 ? "=> " : "   ");

					if ((line = br.readLine()) != null)
						sb.append(line + "\n");
					else
						return code;
				} while (!opt.isQuiet() //
						&& (!ParseUtil.isParseable(sb.toString(), true) || !line.isEmpty() && !line.endsWith("#")));

				code &= dispatcher.dispatchCommand(sb.toString(), writer);
			} catch (Throwable ex) {
				LogUtil.error(ex);
			}
	}

	@Override
	public void close() {
		Util.closeQuietly(reader);
		Util.closeQuietly(writer);
	}

}
