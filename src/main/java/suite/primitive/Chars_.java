package suite.primitive;

import java.io.IOException;

import suite.primitive.Chars.CharsBuilder;
import suite.primitive.Chars.WriteChar;
import suite.streamlet.Outlet;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;

public class Chars_ {

	private static int bufferSize = 65536;

	public static Outlet<Chars> buffer(Outlet<Chars> outlet) {
		return Outlet.of(new BufferedSource(outlet) {
			protected boolean search() {
				return bufferSize <= (p0 = p1 = buffer.size());
			}
		});
	}

	public static void copy(char[] from, int fromIndex, char[] to, int toIndex, int size) {
		if (0 < size)
			System.arraycopy(from, fromIndex, to, toIndex, size);
		else if (size < 0)
			throw new IndexOutOfBoundsException();
	}

	public static void copy(Outlet<Chars> outlet, WriteChar writer) {
		Chars chars;
		while ((chars = outlet.next()) != null)
			try {
				writer.write(chars.cs, chars.start, chars.end - chars.start);
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
	}

	public static Fun<Outlet<Chars>, Outlet<Chars>> split(Chars delim) {
		int ds = delim.size();

		return outlet -> Outlet.of(new BufferedSource(outlet) {
			protected boolean search() {
				int size = buffer.size();
				while ((p1 = p0 + ds) <= size)
					if (!delim.equals(buffer.range(p0, p1)))
						p0++;
					else
						return true;
				if (!cont) {
					p0 = p1 = buffer.size();
					return true;
				} else
					return false;
			}
		});
	}

	private static abstract class BufferedSource implements Source<Chars> {
		protected Outlet<Chars> outlet;
		protected Chars buffer = Chars.empty;
		protected boolean cont = true;
		protected int p0, p1;

		public BufferedSource(Outlet<Chars> outlet) {
			this.outlet = outlet;
		}

		public Chars source() {
			Chars in;
			CharsBuilder cb = new CharsBuilder();
			cb.append(buffer);

			p0 = 0;

			while (!search() && (cont &= (in = outlet.next()) != null)) {
				cb.append(in);
				buffer = cb.toChars();
			}

			if (cont && 0 < p0) {
				Chars head = buffer.range(0, p0);
				buffer = buffer.range(p1);
				return head;
			} else
				return null;
		}

		protected abstract boolean search(); // should set p0, p1
	}

}
