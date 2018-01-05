package suite.lp.compile.impl;

import java.util.HashMap;

import suite.jdk.gen.FunCreator;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.jdk.gen.FunFactory;
import suite.lp.doer.Binder;
import suite.lp.doer.BinderFactory;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Str;
import suite.node.Tree;
import suite.node.Tuple;
import suite.node.io.SwitchNode;
import suite.util.FunUtil2.BinOp;

public class CompileBinderImpl1 extends CompileClonerImpl implements BinderFactory {

	private static FunFactory f = new FunFactory();
	private static FunExpr fail = f._false();
	private static FunExpr ok = f._true();

	private boolean isBindTrees;

	public CompileBinderImpl1() {
		this(true);
	}

	public CompileBinderImpl1(boolean isBindTrees) {
		this.isBindTrees = isBindTrees;
	}

	public Bind_ binder(Node node) {
		FunCreator<Bind_> fc = FunCreator.of(Bind_.class);

		return fc.create(new BinOp<>() {
			private FunExpr env, trail, b;

			public FunExpr apply(FunExpr bindEnv, FunExpr target) {
				this.env = bindEnv.field("env");
				this.trail = bindEnv.field("trail");
				return f.declare(ok, b -> {
					this.b = b;
					return compile_(node, target);
				});
			}

			private FunExpr compile_(Node node, FunExpr target) {
				FunExpr br = bind(f.object_(node, Node.class), target);
				FunExpr brc = bindClone(node, target);

				return new SwitchNode<FunExpr>(node //
				).applyIf(Atom.class, n -> {
					return f.ifEquals(target, f.object(node), ok, br);
				}).applyIf(Int.class, n -> {
					return f.ifInstance(Int.class, target, i -> f.ifEquals(i.field("number"), f.int_(n.number), ok, fail), br);
				}).applyIf(Reference.class, n -> {
					return f.invokeStatic(Binder.class, "bind", target, env.field("refs").index(f.int_(computeIndex(node))), trail);
				}).applyIf(Str.class, n -> {
					return f.ifInstance(Str.class, target, s -> f.object(n.value).invoke("equals", s.field("value")), br);
				}).applyIf(Tree.class, tree -> {
					return f.declare(f.invokeStatic(Tree.class, "decompose", target, f.object(tree.getOperator())), t -> {
						Node lt = tree.getLeft();
						Node rt = tree.getRight();
						return f.ifNonNull(t, compile_(lt, t.invoke("getLeft"), compile_(rt, t.invoke("getRight"), ok)), brc);
					});
				}).applyIf(Tuple.class, n -> {
					return f.ifInstance(Tuple.class, target, tuple -> f.declare(tuple.field("nodes"), targets -> {
						Node[] nodes = n.nodes;
						FunExpr fe = ok;
						for (int i = 0; i < nodes.length; i++)
							fe = compile_(nodes[i], targets.index(f.int_(i)), fe);
						return f.if_(targets.length(), fe, brc);
					}), brc);
				}).nonNullResult();
			}

			private FunExpr compile_(Node node, FunExpr target, FunExpr cps) {
				return f.assign(b, compile_(node, target), f.if_(b, cps, fail));
			}

			private FunExpr bindClone(Node node, FunExpr target) {
				if (isBindTrees)
					return bind(f.object(cloner(node)).apply(env), target);
				else
					return fail;
			}

			private FunExpr bind(FunExpr node, FunExpr target) {
				return f.ifInstanceAnd(Reference.class, target, ref -> f.seq(trail.invoke("addBind", ref, node), ok));
			}
		}).apply(new HashMap<>());
	}

}
