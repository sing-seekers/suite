package suite.lp.kb;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import suite.node.Node;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

/**
 * Index rules by two layers of prototype, which is the leftest element and the
 * leftest of the right element in the rule head or query.
 * 
 * @author ywsing
 */
public class DoubleIndexedRuleSet extends IndexedRuleSet {

	// Index rules by prototypes.
	// Have to use a multi-map implementation that allow null keys.
	private Map<Prototype, ListMultimap<Prototype, Rule>> index0 = new HashMap<>();

	@Override
	public void addRule(Rule rule) {
		super.addRule(rule);
		List<Rule> rules1 = ruleList(rule);
		if (rules1 != null)
			rules1.add(rule);
	}

	@Override
	public void addRuleToFront(Rule rule) {
		super.addRuleToFront(rule);
		List<Rule> rules1 = ruleList(rule);
		if (rules1 != null)
			rules1.add(0, rule);
	}

	@Override
	public void removeRule(Rule rule) {
		super.removeRule(rule);

		Prototype p0 = Prototype.get(rule);
		Prototype p1 = Prototype.get(rule, 1);
		ListMultimap<Prototype, Rule> index1 = index0.get(p0);

		if (index1 != null) {
			index1.remove(p1, rule);

			if (index1.isEmpty())
				index0.remove(p0);
		}
	}

	@Override
	public List<Rule> searchRule(Node node) {
		Prototype p0 = Prototype.get(node);
		Prototype p1 = Prototype.get(node, 1);

		if (p0 != null && p1 != null && !index0.containsKey(null)) {
			ListMultimap<Prototype, Rule> index1 = index0.get(p0);

			if (index1 != null && !index1.containsKey(null))
				return index1.get(p1);
		}

		return super.searchRule(node);
	}

	private List<Rule> ruleList(Rule rule) {
		Prototype p0 = Prototype.get(rule);
		Prototype p1 = Prototype.get(rule, 1);
		ListMultimap<Prototype, Rule> index1 = index0.get(p0);

		if (index1 == null)
			index0.put(p0, index1 = ArrayListMultimap.create());

		return index1.get(p1);
	}

}
