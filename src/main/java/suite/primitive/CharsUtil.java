package suite.primitive;

import java.io.IOException;
import java.io.Writer;

import suite.primitive.Chars.CharsBuilder;
import suite.streamlet.Outlet;
import suite.util.FunUtil.Source;

public class CharsUtil {

	private static final int bufferSize = 65536;

	public static Outlet<Chars> buffer(Outlet<Chars> o) {
		return new Outlet<>(new Source<Chars>() {
			protected Chars buffer = Chars.empty;
			protected boolean isEof = false;

			public Chars source() {
				CharsBuilder cb = new CharsBuilder();
				cb.append(buffer);

				Chars in;
				while (!isEof && cb.size() < bufferSize)
					if ((in = o.next()) != null)
						cb.append(in);
					else
						isEof = true;

				Chars chars = cb.toChars();
				int n = Math.min(chars.size(), bufferSize);
				Chars head = chars.subchars(0, n);
				buffer = chars.subchars(n);

				return head;
			}
		});
	}

	public static Outlet<Chars> concatSplit(Outlet<Chars> o, Chars delim) {
		int ds = delim.size();

		return new Outlet<>(new Source<Chars>() {
			private Chars buffer = Chars.empty;
			private boolean isArriving;
			private int p;

			public Chars source() {
				Chars chars;
				CharsBuilder cb = new CharsBuilder();
				cb.append(buffer);

				p = 0;

				while (isArriving && !search(delim) && (isArriving = (chars = o.next()) != null)) {
					cb.append(chars);
					buffer = cb.toChars();
				}

				if (isArriving) {
					Chars head = buffer.subchars(0, p);
					buffer = buffer.subchars(p + ds);
					return head;
				} else
					return !buffer.isEmpty() ? buffer : null;
			}

			private boolean search(Chars delim) {
				boolean isMatched = false;

				while (!isMatched && p + ds <= buffer.size()) {
					boolean isMatched_ = true;
					for (int i = 0; isMatched_ && i < ds; i++)
						isMatched_ = buffer.get(p + i) == delim.get(i);
					if (isMatched_)
						isMatched = true;
					else
						p++;
				}

				return isMatched;
			}
		});
	}

	public static void copy(Outlet<Chars> o, Writer writer) throws IOException {
		Chars chars;
		while ((chars = o.next()) != null)
			chars.write(writer);
	}

}
