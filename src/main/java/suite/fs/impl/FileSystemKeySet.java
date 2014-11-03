package suite.fs.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import suite.fs.KeyDataStoreMutator;
import suite.fs.impl.FileSystemKeyUtil.NameKey;
import suite.primitive.Bytes;
import suite.util.Streamlet;
import suite.util.Util;

/**
 * Facilitates storage of unlimited length of filenames on the immutable B-tree.
 * This cuts up a full path into chunks of 30 characters and arrange them as a
 * trie over the original B-tree, indexed by hash values. Used for implementing
 * file systems.
 *
 * @author ywsing
 */
public class FileSystemKeySet {

	private List<NameKey> emptyKeys = Collections.<NameKey> emptyList();
	private FileSystemKeyUtil keyUtil;
	private KeyDataStoreMutator<Bytes> mutator;

	public FileSystemKeySet(FileSystemKeyUtil keyUtil, KeyDataStoreMutator<Bytes> transaction) {
		this.keyUtil = keyUtil;
		this.mutator = transaction;
	}

	public Streamlet<Bytes> list(Bytes bytes0, Bytes bytes1) {
		return list(emptyKeys, keyUtil.toNameKeys(bytes0), keyUtil.toNameKeys(bytes1));
	}

	private Streamlet<Bytes> list(List<NameKey> prefix, List<NameKey> keys0, List<NameKey> keys1) {
		Bytes hash = keyUtil.hash(keyUtil.toName(prefix));
		NameKey minKey = keys0 != null && !keys0.isEmpty() ? Util.first(keys0) : boundingKey(hash, 0);
		NameKey maxKey = keys1 != null && !keys1.isEmpty() ? Util.first(keys1) : boundingKey(hash, 1);
		Streamlet<Bytes> st = mutator.keys(keyUtil.toBytes(minKey), increment(keyUtil.toBytes(maxKey)));

		return st.concatMap(bytes -> {
			NameKey key = keyUtil.toNameKey(bytes);
			List<NameKey> prefix1 = Util.add(prefix, Arrays.asList(key));

			if (key.size == 0) {
				List<NameKey> tailKeys0 = key == minKey ? !keys0.isEmpty() ? Util.right(keys0, 1) : emptyKeys : null;
				List<NameKey> tailKeys1 = key == maxKey ? !keys1.isEmpty() ? Util.right(keys1, 1) : emptyKeys : null;
				return list(prefix1, tailKeys0, tailKeys1);
			} else
				return Streamlet.of(Arrays.asList(keyUtil.toName(prefix1)));
		});
	}

	public void add(Bytes name) {
		for (NameKey key : keyUtil.toNameKeys(name))
			mutator.putTerminal(keyUtil.toBytes(key));
	}

	public void remove(Bytes name) {
		List<NameKey> keys = keyUtil.toNameKeys(name);

		for (int i = keys.size() - 1; i >= 0; i--) {
			NameKey key = keys.get(i);

			if (key.size == 0) {
				Bytes hash = keyUtil.hash(keyUtil.toName(keys.subList(0, i + 1)));
				NameKey minKey = boundingKey(hash, 0);
				NameKey maxKey = boundingKey(hash, 1);
				if (mutator.keys(keyUtil.toBytes(minKey), keyUtil.toBytes(maxKey)) == null)
					mutator.remove(keyUtil.toBytes(key));
			} else
				mutator.remove(keyUtil.toBytes(key));
		}
	}

	private NameKey boundingKey(Bytes hash, int minMax) {
		return keyUtil.toNameKey(hash, minMax, Bytes.emptyBytes, 0);
	}

	private Bytes increment(Bytes bytes) {
		if (!bytes.isEmpty()) {
			Bytes bytes1 = bytes.subbytes(0, -1);
			byte b1 = (byte) (bytes.get(-1) + 1);
			if (b1 != 0)
				return Bytes.concat(bytes1, Bytes.asList(b1));
			else
				return Bytes.concat(increment(bytes1), Bytes.asList((byte) 0));
		} else
			return bytes;
	}

}
