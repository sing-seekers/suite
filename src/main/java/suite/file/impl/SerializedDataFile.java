package suite.file.impl;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;

import suite.file.DataFile;
import suite.primitive.Bytes;
import suite.util.SerializeUtil.Serializer;

/**
 * Persists B-tree pages to file on disk.
 *
 * The following must holds when using this class:
 *
 * pageSize >= sizeof(char) + 2 * sizeof(int) + branchFactor * branchPointerSize
 *
 * where branchPointerSize = max(sizeof(int), sizeof(Value))
 */
public class SerializedDataFile<T, V> implements Closeable {

	private DataFile<T> dataFile;
	private Serializer<V> serializer;

	private static class SerializedPagingException extends RuntimeException {
		private static final long serialVersionUID = 1l;

		public SerializedPagingException(IOException ex) {
			super(ex);
		}
	}

	public SerializedDataFile(DataFile<T> dataFile, Serializer<V> serializer) {
		this.dataFile = dataFile;
		this.serializer = serializer;
	}

	@Override
	public void close() {
		try {
			dataFile.close();
		} catch (IOException ex) {
			throw new SerializedPagingException(ex);
		}
	}

	public void sync() throws IOException {
		dataFile.sync();
	}

	public V load(T pointer) {
		try {
			return serializer.read(new DataInputStream(dataFile.load(pointer).asInputStream()));
		} catch (IOException ex) {
			throw new SerializedPagingException(ex);
		}
	}

	public void save(T pointer, V page) {
		try {
			dataFile.save(pointer, Bytes.of(dataOutput -> serializer.write(dataOutput, page)));
		} catch (IOException ex) {
			throw new SerializedPagingException(ex);
		}
	}

}
