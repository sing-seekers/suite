package suite.lp.predicate;

import java.util.ArrayList;
import java.util.List;

import suite.Suite;
import suite.lp.doer.Binder;
import suite.lp.doer.Prover;
import suite.lp.kb.CompositeRuleSet;
import suite.lp.kb.Prototype;
import suite.lp.kb.Rule;
import suite.lp.predicate.PredicateUtil.BuiltinPredicate;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Formatter;
import suite.node.io.TermOp;
import suite.node.pp.PrettyPrinter;
import suite.util.Fail;

public class RuleSetPredicates {

	public BuiltinPredicate asserta = PredicateUtil.p1((prover, p0) -> {
		prover.ruleSet().addRuleToFront(Rule.of(p0));
		return true;
	});

	public BuiltinPredicate assertz = PredicateUtil.p1((prover, p0) -> {
		prover.ruleSet().addRule(Rule.of(p0));
		return true;
	});

	public BuiltinPredicate getAllRules = PredicateUtil.p1((prover, p0) -> {
		var ruleSet = prover.ruleSet();
		List<Rule> rules = ruleSet.getRules();
		List<Node> nodes = new ArrayList<>();

		for (var rule : rules)
			nodes.add(Tree.of(TermOp.IS____, rule.head, rule.tail));

		return prover.bind(Tree.of(TermOp.NEXT__, nodes), p0);
	});

	public BuiltinPredicate importPredicate = PredicateUtil.p1((prover, p0) -> Suite.importFrom(prover.ruleSet(), p0));

	public BuiltinPredicate importUrl = PredicateUtil.p1((prover, p0) -> {
		var url = Formatter.display(p0);
		try {
			return Suite.importUrl(prover.ruleSet(), url);
		} catch (Exception ex) {
			return Fail.t("exception when importing " + url, ex);
		}
	});

	public BuiltinPredicate list = PredicateUtil.ps((prover, ps) -> {
		Prototype proto = null;
		if (0 < ps.length)
			proto = Prototype.of(ps[0]);

		Node node = Suite.listRules(prover.ruleSet(), proto);
		var printer = new PrettyPrinter();
		System.out.println(printer.prettyPrint(node));
		return true;
	});

	public BuiltinPredicate retract = PredicateUtil.p1((prover, p0) -> {
		prover.ruleSet().removeRule(Rule.of(p0));
		return true;
	});

	public BuiltinPredicate retractAll = PredicateUtil.p1((prover, p0) -> {
		var rule0 = Rule.of(p0);

		var ruleSet = prover.ruleSet();
		var trail = prover.getTrail();
		var pit = trail.getPointInTime();
		List<Rule> targets = new ArrayList<>();

		for (var rule : ruleSet.getRules()) {
			if (Binder.bind(rule0.head, rule.head, trail) //
					&& Binder.bind(rule0.tail, rule.tail, trail))
				targets.add(rule);

			trail.unwind(pit);
		}

		for (var rule : targets)
			ruleSet.removeRule(rule);

		return true;
	});

	public BuiltinPredicate with = PredicateUtil.p2((prover, p0, p1) -> {
		var ruleSet = prover.ruleSet();
		var ruleSet1 = Suite.getRuleSet(p0);
		CompositeRuleSet ruleSet2 = new CompositeRuleSet(ruleSet1, ruleSet);
		return new Prover(ruleSet2).prove(p1);
	});

}
