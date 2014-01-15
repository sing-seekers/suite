package suite.btree;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import suite.util.FileUtil;

/**
 * Defines interface for reading/writing byte buffer. The operation within the
 * same serializer should always put in the same number of bytes.
 */
public interface Serializer<V> {

	public V read(ByteBuffer buffer);

	public void write(ByteBuffer buffer, V value);

	public static class B_TreeSuperBlockSerializer<Key, Value> implements Serializer<B_Tree<Key, Value>.SuperBlock> {
		private B_Tree<Key, Value> b_tree;
		private IntSerializer intSerializer = new IntSerializer();

		public B_TreeSuperBlockSerializer(B_Tree<Key, Value> b_tree) {
			this.b_tree = b_tree;
		}

		public B_Tree<Key, Value>.SuperBlock read(ByteBuffer buffer) {
			B_Tree<Key, Value>.SuperBlock superBlock = b_tree.new SuperBlock();
			superBlock.root = intSerializer.read(buffer);
			return superBlock;
		}

		public void write(ByteBuffer buffer, B_Tree<Key, Value>.SuperBlock value) {
			intSerializer.write(buffer, value.root);
		}
	}

	public static class B_TreePageSerializer<Key, Value> implements Serializer<B_Tree<Key, Value>.Page> {
		private B_Tree<Key, Value> b_tree;
		private Serializer<Key> keySerializer;
		private Serializer<Value> valueSerializer;

		private static final char LEAF = 'L';
		private static final char BRANCH = 'I';

		public B_TreePageSerializer(B_Tree<Key, Value> b_tree, Serializer<Key> keySerializer, Serializer<Value> valueSerializer) {
			this.b_tree = b_tree;
			this.keySerializer = keySerializer;
			this.valueSerializer = valueSerializer;
		}

		public B_Tree<Key, Value>.Page read(ByteBuffer buffer) {
			int pageNo = buffer.getInt();
			int size = buffer.getInt();

			B_Tree<Key, Value>.Page page = b_tree.new Page(pageNo);

			for (int i = 0; i < size; i++) {
				char nodeType = buffer.getChar();
				Key key = keySerializer.read(buffer);

				if (nodeType == BRANCH) {
					int branch = buffer.getInt();
					addBranch(page, key, branch);
				} else if (nodeType == LEAF) {
					Value value = valueSerializer.read(buffer);
					addLeaf(page, key, value);
				}
			}

			return page;
		}

		public void write(ByteBuffer buffer, B_Tree<Key, Value>.Page page) {
			buffer.putInt(page.pageNo);
			buffer.putInt(page.size());

			for (B_Tree<Key, Value>.KeyPointer kp : page)
				if (kp.pointer instanceof B_Tree.Branch) {
					buffer.putChar(BRANCH);
					keySerializer.write(buffer, kp.key);
					buffer.putInt(kp.getBranchPageNo());
				} else if (kp.pointer instanceof B_Tree.Leaf) {
					buffer.putChar(LEAF);
					keySerializer.write(buffer, kp.key);
					valueSerializer.write(buffer, kp.getLeafValue());
				}
		}

		private void addLeaf(List<B_Tree<Key, Value>.KeyPointer> kps, Key k, Value v) {
			kps.add(b_tree.new KeyPointer(k, b_tree.new Leaf(v)));
		}

		private void addBranch(List<B_Tree<Key, Value>.KeyPointer> kps, Key k, int branch) {
			kps.add(b_tree.new KeyPointer(k, b_tree.new Branch(branch)));
		}

	}

	public static class ByteArraySerializer implements Serializer<byte[]> {
		private int length;

		public ByteArraySerializer(int length) {
			this.length = length;
		}

		public byte[] read(ByteBuffer buffer) {
			byte bs[] = new byte[length];
			buffer.get(bs);
			return bs;
		}

		public void write(ByteBuffer buffer, byte value[]) {
			buffer.put(value);
		}
	}

	public static class StringSerializer implements Serializer<String> {
		private int length;

		public StringSerializer(int length) {
			this.length = length;
		}

		public String read(ByteBuffer buffer) {
			byte bs[] = new byte[length];
			int l = buffer.getInt();
			buffer.get(bs);
			return new String(bs, FileUtil.charset).substring(0, l);
		}

		public void write(ByteBuffer buffer, String value) {
			byte bs[] = Arrays.copyOf(value.getBytes(FileUtil.charset), length);
			buffer.putInt(value.length());
			buffer.put(bs);
		}
	}

	public static class IntSerializer implements Serializer<Integer> {
		public Integer read(ByteBuffer buffer) {
			return buffer.getInt();
		}

		public void write(ByteBuffer buffer, Integer value) {
			buffer.putInt(value);
		}
	}

}
