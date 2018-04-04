package suite.algo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import suite.util.FunUtil;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.List_;

/**
 * Lempel-Ziv-Welch compression.
 *
 * @author ywsing
 */
public class LempelZivWelch<Unit> {

	private List<Unit> units;

	private class Trie {
		private Integer index;
		private Map<Unit, Trie> branches = new HashMap<>();

		private Trie(Integer index) {
			this.index = index;
		}
	}

	public LempelZivWelch(List<Unit> units) {
		this.units = units;
	}

	public Source<Integer> encode(Source<Unit> in) {
		return FunUtil.suck(sink -> encode(in, sink));
	}

	public Source<Unit> decode(Source<Integer> in) {
		return FunUtil.suck(sink -> decode(in, sink));
	}

	private void encode(Source<Unit> source, Sink<Integer> sink) {
		Trie root = new Trie(null);
		var index = 0;

		for (Unit unit : units)
			root.branches.put(unit, new Trie(index++));

		Trie trie = root;
		Unit unit;

		while ((unit = source.source()) != null) {
			if (!trie.branches.containsKey(unit)) {
				sink.sink(trie.index);
				trie.branches.put(unit, new Trie(index++));
				trie = root;
			}

			trie = trie.branches.get(unit);
		}

		if (trie != root)
			sink.sink(trie.index);
	}

	private void decode(Source<Integer> source, Sink<Unit> sink) {
		List<List<Unit>> dict = new ArrayList<>();

		for (Unit unit : units)
			dict.add(List.of(unit));

		Integer index;

		if ((index = source.source()) != null) {
			List<Unit> word;

			for (Unit unit : word = dict.get(index))
				sink.sink(unit);

			while ((index = source.source()) != null) {
				List<Unit> word0 = word;
				List<Unit> newWord;

				if (index < dict.size())
					newWord = List_.concat(word0, List_.left(word = dict.get(index), 1));
				else
					newWord = word = List_.concat(word0, List_.left(word0, 1));

				dict.add(newWord);

				for (Unit unit : word)
					sink.sink(unit);
			}
		}
	}

}
