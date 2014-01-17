package suite.btree;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;

import suite.btree.Serializer.B_TreePageSerializer;
import suite.btree.Serializer.B_TreeSuperblockSerializer;

public class B_TreeHolder<Key, Value> implements Closeable {

	private Allocator al;
	private Persister<B_Tree<Key, Value>.Superblock> sbp;
	private Persister<B_Tree<Key, Value>.Page> pp;

	private B_Tree<Key, Value> b_tree;

	public B_TreeHolder(String pathName //
			, boolean isNew //
			, Comparator<Key> comparator //
			, Serializer<Key> ks //
			, Serializer<Value> vs) throws IOException {
		new File(pathName).getParentFile().mkdirs();

		String sbf = pathName + ".superblock";
		String amf = pathName + ".alloc";
		String pf = pathName + ".pages";

		if (isNew)
			for (String filename : new String[] { sbf, amf, pf })
				new File(filename).delete();

		b_tree = new B_Tree<>(comparator);

		B_TreeSuperblockSerializer<Key, Value> sbs = new B_TreeSuperblockSerializer<>(b_tree);
		B_TreePageSerializer<Key, Value> ps = new B_TreePageSerializer<>(b_tree, ks, vs);

		al = new Allocator(amf);
		sbp = new Persister<>(sbf, sbs);
		pp = new Persister<>(pf, ps);

		b_tree.setAllocator(al);
		b_tree.setSuperblockPersister(sbp);
		b_tree.setPagePersister(pp);
		b_tree.setBranchFactor(16);

		if (isNew)
			b_tree.create();
	}

	@Override
	public void close() throws IOException {
		pp.close();
		sbp.close();
		al.close();
	}

	public B_Tree<Key, Value> get() {
		return b_tree;
	}

}
