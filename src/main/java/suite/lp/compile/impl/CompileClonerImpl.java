package suite.lp.compile.impl;

import java.util.HashMap;

import suite.adt.pair.Pair;
import suite.jdk.gen.FunCreator;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.jdk.gen.FunFactory;
import suite.lp.doer.ClonerFactory;
import suite.lp.sewing.VariableMapper;
import suite.node.Atom;
import suite.node.Dict;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Str;
import suite.node.Tree;
import suite.node.Tuple;
import suite.node.io.SwitchNode;
import suite.streamlet.Read;
import suite.util.FunUtil.Iterate;

public class CompileClonerImpl extends VariableMapper implements ClonerFactory {

	private static FunFactory f = new FunFactory();

	@Override
	public Clone_ cloner(Node node) {
		FunCreator<Clone_> fc = FunCreator.of(Clone_.class, false);

		return fc.create(new Iterate<>() {
			private FunExpr env;

			public FunExpr apply(FunExpr env) {
				this.env = env;
				return compile_(node);
			}

			private FunExpr compile_(Node node_) {
				return new SwitchNode<FunExpr>(node_ //
				).applyIf(Atom.class, n -> {
					return f.object(node_);
				}).applyIf(Dict.class, n -> {
					FunExpr[] exprs = Read //
							.from2(n.map) //
							.map((key, value) -> f.invokeStatic(Pair.class, "of", compile_(key), compile_(value))) //
							.toArray(FunExpr.class);
					return f.invokeStatic(Dict.class, "of", f.array(Pair.class, exprs));
				}).applyIf(Int.class, n -> {
					return f.object(node_);
				}).applyIf(Reference.class, n -> {
					return env.field("refs").index(f.int_(computeIndex(node_)));
				}).applyIf(Str.class, n -> {
					return f.object(node_);
				}).applyIf(Tree.class, tree -> {
					FunExpr fe0 = compile_(tree.getLeft()).cast(Node.class);
					FunExpr fe1 = compile_(tree.getRight()).cast(Node.class);
					return f.invokeStatic(Tree.class, "of", f.object(tree.getOperator()), fe0, fe1);
				}).applyIf(Tuple.class, n -> {
					FunExpr[] exprs = Read.from(n.nodes).map(this::compile_).toArray(FunExpr.class);
					return f.invokeStatic(Tuple.class, "of", f.array(Node.class, exprs));
				}).nonNullResult();
			}
		}).apply(new HashMap<>());
	}

}
