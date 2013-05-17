package org.suite;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.suite.SuiteUtil.FunCompilerConfig;
import org.suite.doer.Formatter;
import org.suite.doer.Generalizer;
import org.suite.doer.PrettyPrinter;
import org.suite.doer.Prover;
import org.suite.doer.ProverConfiguration;
import org.suite.doer.Station;
import org.suite.doer.TermParser;
import org.suite.doer.TermParser.TermOp;
import org.suite.kb.Rule;
import org.suite.kb.RuleSet;
import org.suite.kb.RuleSet.RuleSetUtil;
import org.suite.node.Atom;
import org.suite.node.Node;
import org.suite.node.Tree;
import org.util.IoUtil;
import org.util.LogUtil;
import org.util.Util;

/**
 * Logic interpreter and functional interpreter. Likes Prolog and Haskell.
 * 
 * @author ywsing
 */
public class Main {

	private FunCompilerConfig fcc = new FunCompilerConfig();
	private ProverConfiguration pc = new ProverConfiguration();

	private boolean isFilter = false;
	private boolean isFunctional = false;
	private boolean isLogical = false;

	public Main() {
		fcc.setProverConfiguration(pc);
		fcc.setIn(new StringReader(""));
	}

	public static void main(String args[]) {
		LogUtil.initLog4j();

		try {
			new Main().run(args);
		} catch (Throwable ex) {
			log.error(Main.class, ex);
		}
	}

	private void run(String args[]) throws IOException {
		int code = 0;
		List<String> inputs = new ArrayList<>();
		Iterator<String> iter = Arrays.asList(args).iterator();

		while (iter.hasNext()) {
			String arg = iter.next();

			if (arg.startsWith("-"))
				processOption(arg, iter);
			else
				inputs.add(arg);
		}

		if (isFilter)
			code = runFilter(inputs) ? 0 : 1;
		else if (isFunctional)
			code = runFunctional(inputs) ? 0 : 1;
		else if (isLogical)
			code = runLogical(inputs) ? 0 : 1;
		else
			run(inputs);

		System.exit(code);
	}

	private void processOption(String arg, Iterator<String> iter) {
		processOption(arg, iter, true);
	}

	private void processOption(String arg, Iterator<String> iter, boolean on) {
		if (arg.equals("-dump-code"))
			fcc.setDumpCode(on);
		else if (arg.equals("-eager"))
			fcc.setLazy(!on);
		else if (arg.equals("-filter"))
			isFilter = on;
		else if (arg.equals("-functional"))
			isFunctional = on;
		else if (arg.equals("-lazy"))
			fcc.setLazy(on);
		else if (arg.equals("-libraries") && iter.hasNext())
			fcc.setLibraries(Arrays.asList(iter.next().split(",")));
		else if (arg.equals("-logical"))
			isLogical = on;
		else if (arg.startsWith("-no-"))
			processOption("-" + arg.substring(4), iter, false);
		else if (arg.equals("-precompile") && iter.hasNext())
			for (String lib : iter.next().split(","))
				runPrecompile(lib);
		else if (arg.equals("-trace"))
			pc.setTrace(on);
		else
			throw new RuntimeException("Unknown option " + arg);
	}

	public enum InputType {
		EVALUATE("\\"), //
		EVALUATESTR("\\s"), //
		EVALUATETYPE("\\t"), //
		FACT(""), //
		OPTION("-"), //
		PRETTYPRINT("\\p"), //
		QUERY("?"), //
		QUERYCOMPILED("\\l"), //
		QUERYELABORATE("/"), //
		;

		String prefix;

		private InputType(String prefix) {
			this.prefix = prefix;
		}
	};

	public void run(List<String> importFilenames) throws IOException {
		RuleSet rs = pc.ruleSet();
		SuiteUtil.importResource(pc.ruleSet(), "auto.sl");

		for (String importFilename : importFilenames)
			SuiteUtil.importFile(rs, importFilename);

		InputStreamReader is = new InputStreamReader(System.in, IoUtil.charset);
		BufferedReader br = new BufferedReader(is);

		while (true)
			try {
				StringBuilder sb = new StringBuilder();
				String line;

				do {
					System.out.print(sb.length() == 0 ? "=> " : "   ");

					if ((line = br.readLine()) != null)
						sb.append(line + "\n");
					else
						return;
				} while (!line.isEmpty() && !line.endsWith("#"));

				String input = sb.toString();

				if (Util.isBlank(input))
					continue;

				InputType type = null;

				commandFound: for (int i = Math.min(2, input.length()); i >= 0; i--) {
					String starts = input.substring(0, i);

					for (InputType inputType : InputType.values())
						if (Util.equals(starts, inputType.prefix)) {
							type = inputType;
							input = input.substring(i);
							break commandFound;
						}
				}

				input = input.trim();
				if (input.endsWith("#"))
					input = Util.substr(input, 0, -1);

				final int count[] = { 0 };
				Node node = new TermParser().parse(input.trim());

				Prover prover = new Prover(pc);

				switch (type) {
				case EVALUATE:
					node = evaluateFunctional(node);
					System.out.println(Formatter.dump(node));
					break;
				case EVALUATESTR:
					node = evaluateFunctional(node);
					System.out.println(SuiteUtil.stringize(node).toString());
					break;
				case EVALUATETYPE:
					fcc.setNode(node);
					node = SuiteUtil.evaluateFunType(fcc);
					System.out.println(Formatter.dump(node));
					break;
				case FACT:
					rs.addRule(Rule.formRule(node));
					break;
				case OPTION:
					List<String> args = Arrays.asList(("-" + input).split(" "));
					Iterator<String> iter = args.iterator();
					while (iter.hasNext())
						processOption(iter.next(), iter);
					break;
				case PRETTYPRINT:
					System.out.println(new PrettyPrinter().prettyPrint(node));
					break;
				case QUERY:
				case QUERYELABORATE:
					final Generalizer generalizer = new Generalizer();
					node = generalizer.generalize(node);

					if (type == InputType.QUERY)
						System.out.println(yesNo(prover.prove(node)));
					else if (type == InputType.QUERYELABORATE) {
						Node elab = new Station() {
							public boolean run() {
								String dump = generalizer.dumpVariables();
								if (!dump.isEmpty())
									System.out.println(dump);

								count[0]++;
								return false;
							}
						};

						prover.prove(Tree.create(TermOp.AND___, node, elab));

						if (count[0] == 1)
							System.out.println(count[0] + " solution\n");
						else
							System.out.println(count[0] + " solutions\n");
					}
					break;
				case QUERYCOMPILED:
					List<Node> nodes = SuiteUtil.evaluateLogical(node //
							, Atom.NIL //
							, pc //
							, false);
					System.out.println(yesNo(!nodes.isEmpty()));
				}
			} catch (Throwable ex) {
				LogUtil.error(Main.class, ex);
			}
	}

	public boolean runLogical(List<String> files) throws IOException {
		boolean result = true;

		RuleSet rs = RuleSetUtil.create();
		result &= SuiteUtil.importResource(rs, "auto.sl");

		for (String file : files)
			result &= SuiteUtil.importFile(rs, file);

		return result;
	}

	public boolean runFilter(List<String> inputs) throws IOException {
		StringBuilder sb = new StringBuilder();
		for (String input : inputs)
			sb.append(input + " ");

		Node node = SuiteUtil.parse(applyFilter(sb.toString()));
		evaluateFunctional(node);
		return true;
	}

	public boolean runFunctional(List<String> files) throws IOException {
		if (files.size() == 1) {
			FileInputStream is = new FileInputStream(files.get(0));
			Node node = SuiteUtil.parse(IoUtil.readStream(is));
			Node result = evaluateFunctional(node);
			return result == Atom.TRUE;
		} else
			throw new RuntimeException("Only one evaluation is allowed");
	}

	public void runPrecompile(String libraryName) {
		System.out.println("Pre-compiling " + libraryName + "... ");
		String imports[] = { "auto.sl", "fc-precompile.sl" };

		RuleSet rs = SuiteUtil.createRuleSet(imports);
		Prover prover = new Prover(new ProverConfiguration(rs, pc));

		String goal = "fc-setup-precompile " + libraryName;
		Node node = SuiteUtil.parse(goal);

		if (prover.prove(node))
			System.out.println("Pre-compilation success\n");
		else
			System.out.println("Pre-compilation failed");
	}

	// Public to be called by test case FilterTest.java
	public static String applyFilter(String func) {
		return "source {} | (" + func + ") | sink {}";
	}

	private Node evaluateFunctional(Node node) {
		fcc.setNode(node);
		return SuiteUtil.evaluateFun(fcc);
	}

	private String yesNo(boolean q) {
		return q ? "Yes\n" : "No\n";
	}

	private static Log log = LogFactory.getLog(Util.currentClass());

}
