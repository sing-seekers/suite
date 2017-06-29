package suite.primitive;

import java.io.IOException;

import suite.primitive.Longs.LngsBuilder;
import suite.primitive.Longs.WriteLong;
import suite.streamlet.Outlet;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;

public class Longs_ {

	private static int bufferSize = 65536;

	public static Outlet<Longs> buffer(Outlet<Longs> outlet) {
		return Outlet.of(new BufferedSource(outlet) {
			protected boolean search() {
				return bufferSize <= (p0 = p1 = buffer.size());
			}
		});
	}

	public static void copy(Outlet<Longs> outlet, WriteLong writer) {
		Longs longs;
		while ((longs = outlet.next()) != null)
			try {
				writer.write(longs.cs, longs.start, longs.end - longs.start);
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
	}

	public static Fun<Outlet<Longs>, Outlet<Longs>> split(Longs delim) {
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

	private static abstract class BufferedSource implements Source<Longs> {
		protected Outlet<Longs> outlet;
		protected Longs buffer = Longs.empty;
		protected boolean cont = true;
		protected int p0, p1;

		public BufferedSource(Outlet<Longs> outlet) {
			this.outlet = outlet;
		}

		public Longs source() {
			Longs in;
			LngsBuilder cb = new LngsBuilder();
			cb.append(buffer);

			p0 = 0;

			while (!search() && (cont &= (in = outlet.next()) != null)) {
				cb.append(in);
				buffer = cb.toLongs();
			}

			if (cont && 0 < p0) {
				Longs head = buffer.range(0, p0);
				buffer = buffer.range(p1);
				return head;
			} else
				return null;
		}

		protected abstract boolean search(); // should set p0, p1
	}

}
