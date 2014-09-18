package suite.lp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import suite.adt.ListMultimap;
import suite.immutable.IList;
import suite.lp.doer.Binder;
import suite.lp.doer.Generalizer;
import suite.lp.doer.Generalizer.Env;
import suite.lp.doer.Prover;
import suite.lp.kb.Prototype;
import suite.lp.kb.Rule;
import suite.lp.kb.RuleSet;
import suite.lp.predicate.PredicateUtil.SystemPredicate;
import suite.lp.predicate.SystemPredicates;
import suite.node.Atom;
import suite.node.Data;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.Pair;
import suite.util.Util;

public class ProveInterpreter {

	private Prover prover;
	private SystemPredicates systemPredicates;

	private ListMultimap<Prototype, Rule> rules = new ListMultimap<>();
	private Map<Prototype, Trampoline> trampolinesByPrototype;

	private int nCutPoints;

	private Trampoline okay = rt -> {
		throw new RuntimeException("Impossibly okay");
	};
	private Trampoline fail = rt -> {
		throw new RuntimeException("Impossibly fail");
	};

	public interface Trampoline {
		public Trampoline prove(Runtime rt);
	}

	private class CompileTime {
		private Generalizer generalizer;
		private int cutIndex;

		public CompileTime(Generalizer generalizer, int cutIndex) {
			this.generalizer = generalizer;
			this.cutIndex = cutIndex;
		}
	}

	private class Runtime {
		private IList<Trampoline> rems; // Continuations
		private IList<Trampoline> alts; // Alternative
		private Env ge;
		private Journal journal;
		private IList<Trampoline> cutPoints[];

		private Runtime(Runtime rt, Env ge1) {
			this(ge1, rt.journal, rt.rems, rt.alts, rt.cutPoints);
		}

		private Runtime(Env ge, Trampoline tr) {
			this(ge, new Journal(), IList.end(), IList.cons(tr, IList.end()), null);
			@SuppressWarnings("unchecked")
			IList<Trampoline>[] trampolineStacks = (IList<Trampoline>[]) new IList<?>[nCutPoints];
			cutPoints = trampolineStacks;
		}

		private Runtime(Env ge, Journal journal, IList<Trampoline> rems, IList<Trampoline> alts, IList<Trampoline> cutPoints[]) {
			this.ge = ge;
			this.journal = journal;
			this.rems = rems;
			this.alts = alts;
			this.cutPoints = cutPoints;
		}

		private void pushRem(Trampoline tr) {
			rems = IList.cons(tr, rems);
		}

		private void pushAlt(Trampoline tr) {
			alts = IList.cons(tr, alts);
		}
	}

	public ProveInterpreter(RuleSet rs) {
		prover = new Prover(rs);
		systemPredicates = new SystemPredicates(prover);

		for (Rule rule : rs.getRules())
			rules.put(Prototype.of(rule), rule);

		if (rules.containsKey(null))
			throw new RuntimeException("Must not contain wild rules");
	}

	public Source<Boolean> compile(Node node) {
		return () -> {
			boolean result[] = new boolean[] { false };
			run(node, env -> result[0] = true);
			return result[0];
		};
	}

	private void run(Node node, Sink<Env> sink) {
		trampolinesByPrototype = new HashMap<>();

		for (Pair<Prototype, Collection<Rule>> entry : rules.listEntries()) {
			List<Rule> rs = new ArrayList<>(entry.t1);
			int cutIndex = nCutPoints++;
			Node query = new Reference();
			Trampoline tr = fail;

			for (int i = rs.size() - 1; i >= 0; i--) {
				Rule rule = rs.get(i);

				Node rn = Tree.of(TermOp.AND___ //
						, Tree.of(TermOp.EQUAL_ //
								, query //
								, rule.getHead()) //
						, rule.getTail());

				Generalizer g = new Generalizer();
				CompileTime ct = new CompileTime(g, cutIndex);
				tr = or(newEnv(g, compile0(ct, rn)), tr);
			}

			trampolinesByPrototype.put(entry.t0, cutBegin(cutIndex, tr));
		}

		Generalizer g1 = new Generalizer();
		CompileTime ct = new CompileTime(g1, nCutPoints++);

		Env env = g1.env();
		Trampoline tr0 = cutBegin(ct.cutIndex, newEnv(g1, compile0(ct, node)));
		Runtime rt = new Runtime(env, and(tr0, (rt_ -> {
			sink.sink(env);
			return fail;
		})));

		while (!rt.alts.isEmpty()) {
			rt.pushRem(rt.alts.getHead());
			rt.alts = rt.alts.getTail();

			Trampoline rem;
			while ((rem = rt.rems.getHead()) != fail) {
				rt.rems = rt.rems.getTail();
				if (rem != okay)
					rt.pushRem(rem.prove(rt));
			}
		}

		rt.journal.undoAllBinds();
	}

	private Trampoline compile0(CompileTime ct, Node node) {
		Trampoline result = null;
		Tree tree = Tree.decompose(node);

		if (tree != null) {
			Operator operator = tree.getOperator();
			Node lhs = tree.getLeft();
			Node rhs = tree.getRight();

			if (operator == TermOp.AND___) { // a, b
				Trampoline tr0 = compile0(ct, lhs);
				Trampoline tr1 = compile0(ct, rhs);
				result = and(tr0, tr1);
			} else if (operator == TermOp.EQUAL_) { // a = b
				Fun<Generalizer.Env, Node> f0 = ct.generalizer.compile(lhs);
				Fun<Generalizer.Env, Node> f1 = ct.generalizer.compile(rhs);
				result = rt -> Binder.bind(f0.apply(rt.ge), f1.apply(rt.ge), rt.journal) ? okay : fail;
			} else if (operator == TermOp.OR____) { // a; b
				Trampoline tr0 = compile0(ct, lhs);
				Trampoline tr1 = compile0(ct, rhs);
				result = or(tr0, tr1);
			} else if (operator == TermOp.TUPLE_ && lhs instanceof Atom) // a b
				result = callSystemPredicate(ct, ((Atom) lhs).getName(), rhs);
			else
				result = callSystemPredicate(ct, operator.getName(), node);
		} else if (node instanceof Atom) {
			String name = ((Atom) node).getName();

			if (Util.stringEquals(name, Generalizer.cutName)) {
				int cutIndex = ct.cutIndex;
				result = rt -> {
					rt.alts = rt.cutPoints[cutIndex];
					return okay;
				};
			} else if (Util.stringEquals(name, "fail"))
				result = fail;
			else if (Util.stringEquals(name, "") || Util.stringEquals(name, "yes"))
				result = okay;
			else
				result = callSystemPredicate(ct, name, Atom.NIL);
		} else if (node instanceof Data<?>) {
			Object data = ((Data<?>) node).getData();
			if (data instanceof Source<?>)
				result = rt -> ((Source<?>) data).source() != Boolean.TRUE ? okay : fail;
		}

		if (result == null) {
			Prototype prototype = Prototype.of(node);
			if (rules.containsKey(prototype))
				result = rt -> trampolinesByPrototype.get(prototype)::prove;
		}

		if (result != null)
			return result;
		else
			throw new RuntimeException("Cannot understand " + node);
	}

	private Trampoline cutBegin(int cutIndex, Trampoline tr) {
		return rt -> {
			IList<Trampoline> alts0 = rt.cutPoints[cutIndex];
			rt.pushAlt(rt_ -> {
				rt_.cutPoints[cutIndex] = alts0;
				return fail;
			});

			rt.cutPoints[cutIndex] = rt.alts;
			return tr;
		};
	}

	private Trampoline newEnv(Generalizer g, Trampoline tr) {
		return rt -> {
			Env ge0 = rt.ge;
			rt.pushAlt(rt_ -> {
				rt_.ge = ge0;
				return fail;
			});

			rt.ge = g.env();
			return tr;
		};
	}

	private Trampoline and(Trampoline tr0, Trampoline tr1) {
		return rt -> {
			rt.pushRem(tr1);
			return tr0;
		};
	}

	private Trampoline or(Trampoline tr0, Trampoline tr1) {
		return rt -> {
			IList<Trampoline> rems0 = rt.rems;
			int pit = rt.journal.getPointInTime();
			rt.pushAlt(rt_ -> {
				rt_.journal.undoBinds(pit);
				rt.rems = rems0;
				return tr1;
			});
			return tr0;
		};
	}

	private Trampoline callSystemPredicate(CompileTime ct, String name, Node pass) {
		SystemPredicate systemPredicate = systemPredicates.get(name);
		if (systemPredicate != null) {
			Fun<Generalizer.Env, Node> f = ct.generalizer.compile(pass);
			return rt -> systemPredicate.prove(prover, f.apply(rt.ge)) ? okay : fail;
		} else
			return null;
	}

}
