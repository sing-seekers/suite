package suite.net;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import suite.primitive.Bytes;

public class NetUtil {

	public static int intValue(Bytes bytes) {
		int value = 0, i = 4;
		do
			value = value << 8 | bytes.get(--i) & 0xFF;
		while (i > 0);
		return value;
	}

	public static Bytes bytesValue(int value) {
		byte bytes[] = new byte[4];
		for (int i = 0; i < 4; i++) {
			bytes[i] = (byte) (value & 0xFF);
			value >>>= 8;
		}
		return Bytes.of(bytes);
	}

	public static Bytes serialize(Object o) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			ObjectOutputStream out = new ObjectOutputStream(baos);
			out.writeObject(o);
			out.flush();
			out.close();
			return Bytes.of(baos.toByteArray());
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	public static <T> T deserialize(Bytes s) {
		byte bytes[] = s.toBytes();
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);

		try {
			ObjectInputStream in = new ObjectInputStream(bais);
			@SuppressWarnings("unchecked")
			T t = (T) in.readObject();
			return t;
		} catch (ClassNotFoundException | IOException ex) {
			throw new RuntimeException(ex);
		}
	}

}
