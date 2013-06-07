package org.suite.search;

import org.instructionexecutor.LogicInstructionExecutor;
import org.suite.Suite;
import org.suite.doer.Cloner;
import org.suite.doer.Prover;
import org.suite.doer.ProverConfig;
import org.suite.kb.RuleSet;
import org.suite.node.Node;
import org.suite.search.ProverBuilder.Builder;
import org.suite.search.ProverBuilder.Finder;
import org.util.FunUtil;
import org.util.FunUtil.Sink;
import org.util.FunUtil.Source;

public class CompiledProverBuilder implements Builder {

	private ProverConfig proverConfig;
	private Finder compiler;

	/**
	 * Interpretes the logic compiler to compile the given code, then execute.
	 */
	public static class CompiledProverBuilderLevel1 extends CompiledProverBuilder {
		public CompiledProverBuilderLevel1(ProverConfig proverConfig, boolean isDumpCode) {
			super(new InterpretedProverBuilder(), proverConfig, isDumpCode);
		}
	}

	/**
	 * Compiles the logic compiler, execute it to compile the given code, then
	 * execute.
	 */
	public static class CompiledProverBuilderLevel2 extends CompiledProverBuilder {
		public CompiledProverBuilderLevel2(ProverConfig proverConfig, boolean isDumpCode) {
			super(new CompiledProverBuilderLevel1(proverConfig, false), proverConfig, isDumpCode);
		}
	}

	public CompiledProverBuilder(Builder builder, ProverConfig proverConfig, boolean isDumpCode) {
		this.compiler = createCompiler(builder, isDumpCode);
		this.proverConfig = proverConfig;
	}

	@Override
	public Finder build(final RuleSet ruleSet, Node goal) {
		Node goal1 = Suite.substitute(".0 >> .1", Suite.ruleSetToNode(ruleSet), goal);
		final Node code = compile(goal1);
		final ProverConfig proverConfig = new ProverConfig(ruleSet, this.proverConfig);

		return new Finder() {
			public void find(Source<Node> source, Sink<Node> sink) {
				proverConfig.setSource(source);
				proverConfig.setSink(sink);
				Prover prover = new Prover(proverConfig);
				new LogicInstructionExecutor(code, prover).execute();
			}
		};
	}

	private Node compile(Node program) {
		final Node holder[] = new Node[] { null };

		compiler.find(FunUtil.source(program), new Sink<Node>() {
			public void sink(Node node) {
				holder[0] = new Cloner().clone(node);
			}
		});

		Node code = holder[0];

		if (code != null)
			return code;
		else
			throw new RuntimeException("Logic compilation error");
	}

	private Finder createCompiler(Builder builder, boolean isDumpCode) {
		String compile = "source .in, compile-logic .in .out";
		compile += isDumpCode ? ", pretty.print .out" : "";
		compile += ", sink .out";
		return builder.build(Suite.logicalCompilerRuleSet(), Suite.parse(compile));
	}

}
