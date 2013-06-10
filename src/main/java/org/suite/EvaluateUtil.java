package org.suite;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.instructionexecutor.FunInstructionExecutor;
import org.suite.doer.Cloner;
import org.suite.doer.ProverConfig;
import org.suite.kb.RuleSet;
import org.suite.node.Atom;
import org.suite.node.Node;
import org.suite.search.CompiledProverBuilder.CompiledProverBuilderLevel1;
import org.suite.search.InterpretedProverBuilder;
import org.suite.search.ProverBuilder.Builder;
import org.suite.search.ProverBuilder.Finder;
import org.util.FunUtil;
import org.util.FunUtil.Sink;
import org.util.FunUtil.Source;
import org.util.Util;

public class EvaluateUtil {

	public boolean proveThis(RuleSet rs, String gs) {
		return prove(new InterpretedProverBuilder(), rs, Suite.parse(gs));
	}

	public boolean evaluateLogical(Node lp) {
		ProverConfig pc = new ProverConfig();
		return prove(new CompiledProverBuilderLevel1(pc, false), pc.ruleSet(), lp);
	}

	private boolean prove(Builder builder, RuleSet rs, Node lp) {
		Node goal = Suite.substitute(".0, sink ()", lp);
		return !evaluateLogical(builder, rs, goal).isEmpty();
	}

	public List<Node> evaluateLogical(Builder builder, RuleSet rs, Node lp) {
		return collect(builder.build(rs, lp), Atom.NIL);
	}

	public Node evaluateFun(FunCompilerConfig fcc) {
		try (FunInstructionExecutor executor = configureFunExecutor(fcc)) {
			Node result = executor.execute();
			return fcc.isLazy() ? executor.unwrap(result) : result;
		}
	}

	public void evaluateFunIo(FunCompilerConfig fcc, Reader reader, Writer writer) throws IOException {
		try (FunInstructionExecutor executor = configureFunExecutor(fcc)) {
			executor.executeIo(reader, writer);
		}
	}

	private FunInstructionExecutor configureFunExecutor(FunCompilerConfig fcc) {
		RuleSet rs = fcc.isLazy() ? Suite.lazyFunCompilerRuleSet() : Suite.eagerFunCompilerRuleSet();
		Atom mode = Atom.create(fcc.isLazy() ? "LAZY" : "EAGER");
		ProverConfig pc = fcc.getProverConfig();

		String eval = "source .in" //
				+ ", compile-function .0 .in .out" //
				+ (fcc.isDumpCode() ? ", pretty.print .out" : "") //
				+ ", sink .out";
		Node node = Suite.substitute(eval, mode);

		Finder finder = new InterpretedProverBuilder(pc).build(rs, node);
		Node code = singleResult(finder, appendLibraries(fcc));

		if (code != null) {
			FunInstructionExecutor e = new FunInstructionExecutor(code);
			e.setProverConfig(new ProverConfig(rs, pc));
			return e;
		} else
			throw new RuntimeException("Function compilation error");
	}

	public Node evaluateFunType(FunCompilerConfig fcc) {
		RuleSet rs = Suite.funCompilerRuleSet();
		ProverConfig pc = fcc.getProverConfig();

		Node node = Suite.parse("source .in" //
				+ ", fc-parse .in .p" //
				+ ", infer-type-rule .p ()/()/() .tr/() .t" //
				+ ", resolve-types .tr" //
				+ ", fc-parse-type .out .t" //
				+ ", sink .out");

		Finder finder = new InterpretedProverBuilder(pc).build(rs, node);
		Node type = singleResult(finder, appendLibraries(fcc));

		if (type != null)
			return type.finalNode();
		else
			throw new RuntimeException("Type inference error");
	}

	private Node appendLibraries(FunCompilerConfig fcc) {
		Node node = fcc.getNode();
		for (String library : fcc.getLibraries())
			if (!Util.isBlank(library))
				node = Suite.substitute("using .0 >> .1", Atom.create(library), node);
		return node;
	}

	private Node singleResult(Finder finder, Node in) {
		List<Node> nodes = collect(finder, in);
		return nodes.size() == 1 ? nodes.get(0) : null;
	}

	private List<Node> collect(Finder finder, Node in) {
		final List<Node> nodes = new ArrayList<>();

		Source<Node> source = FunUtil.source(in);
		Sink<Node> sink = new Sink<Node>() {
			public void sink(Node node) {
				nodes.add(new Cloner().clone(node));
			}
		};

		finder.find(source, sink);
		return nodes;
	}

}
