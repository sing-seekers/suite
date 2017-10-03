package suite;

import java.util.ArrayList;
import java.util.List;

import suite.lp.Trail;
import suite.lp.doer.Generalizer;
import suite.lp.sewing.SewingBinder.BindEnv;
import suite.lp.sewing.SewingBinder.BindPredicate;
import suite.lp.sewing.VariableMapper.Env;
import suite.lp.sewing.impl.SewingBinderImpl0;
import suite.lp.sewing.impl.SewingGeneralizerImpl;
import suite.lp.sewing.impl.VariableMapperImpl.Generalization;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Reference;
import suite.primitive.Ints_;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.Memoize;
import suite.util.To;

public class BindArrayUtil {

	public interface Match {
		public Node[] apply(Node node);

		public Node substitute(Node... nodes);
	}

	public Match match(String pattern) {
		return matches.apply(pattern);
	}

	private Fun<String, Match> matches = Memoize.fun(pattern_ -> {
		Generalizer generalizer = new Generalizer();
		Node fs = Suite.parse(pattern_);
		Node toMatch = generalizer.generalize(fs);

		SewingBinderImpl0 sb = new SewingBinderImpl0(false);
		BindPredicate pred = sb.compileBind(toMatch);
		List<Integer> indexList = new ArrayList<>();
		Integer index;
		int n = 0;

		while ((index = sb.getVariableIndex(generalizer.getVariable(Atom.of("." + n++)))) != null)
			indexList.add(index);

		int size = indexList.size();
		int[] indices = Ints_.toArray(size, indexList::get);

		Source<Generalization> source = SewingGeneralizerImpl.process(fs);

		return new Match() {
			public Node[] apply(Node node) {
				Env env = sb.env();
				Trail trail = new Trail();
				BindEnv be = new BindEnv() {
					public Env getEnv() {
						return env;
					}

					public Trail getTrail() {
						return trail;
					}
				};
				if (pred.test(be, node))
					return To.array(size, Node.class, i -> env.get(indices[i]));
				else
					return null;

			}

			public Node substitute(Node... nodes) {
				Generalization generalization = source.source();
				int i = 0;
				for (Node node : nodes) {
					Node variable = generalization.getVariable(Atom.of("." + i++));
					((Reference) variable).bound(node);
				}
				return generalization.node;
			}
		};
	});

}
