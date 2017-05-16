package suite.primitive;

import java.io.Closeable;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class DataOutput_ implements Closeable, DataOutput {

	private DataOutputStream out;

	public static DataOutput_ of(OutputStream os) {
		return new DataOutput_(new DataOutputStream(os));
	}

	private DataOutput_(DataOutputStream out) {
		this.out = out;
	}

	@Override
	public void close() throws IOException {
		out.close();
	}

	@Override
	public void write(int b) throws IOException {
		out.write(b);
	}

	@Override
	public void write(byte[] b) throws IOException {
		out.write(b);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		out.write(b, off, len);
	}

	@Override
	public void writeBoolean(boolean v) throws IOException {
		out.writeBoolean(v);
	}

	@Override
	public void writeByte(int v) throws IOException {
		out.writeByte(v);
	}

	public void writeBytes(Bytes bytes) throws IOException {
		int start = bytes.start, end = bytes.end;
		out.write(bytes.bs, start, end - start);
	}

	@Override
	public void writeShort(int v) throws IOException {
		out.writeShort(v);
	}

	@Override
	public void writeChar(int v) throws IOException {
		out.writeChar(v);
	}

	@Override
	public void writeInt(int v) throws IOException {
		out.writeInt(v);
	}

	@Override
	public void writeLong(long v) throws IOException {
		out.writeLong(v);
	}

	@Override
	public void writeFloat(float v) throws IOException {
		out.writeFloat(v);
	}

	@Override
	public void writeDouble(double v) throws IOException {
		out.writeDouble(v);
	}

	@Override
	public void writeBytes(String s) throws IOException {
		out.writeBytes(s);
	}

	@Override
	public void writeChars(String s) throws IOException {
		out.writeChars(s);
	}

	@Override
	public void writeUTF(String s) throws IOException {
		out.writeUTF(s);
	}

}
