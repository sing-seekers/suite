package suite.streamlet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import suite.util.FileUtil;
import suite.util.FunUtil.Source;
import suite.util.Pair;
import suite.util.To;

public class Read {

	public static <T> Streamlet<T> empty() {
		return new Streamlet<>(() -> null);
	}

	public static Streamlet<String> lines(Path path) throws IOException {
		return lines(path.toFile());
	}

	public static Streamlet<String> lines(File file) throws FileNotFoundException {
		InputStream is = new FileInputStream(file);
		return lines(is).closeAtEnd(is);
	}

	public static Streamlet<String> lines(InputStream is) {
		Reader reader = new InputStreamReader(is, FileUtil.charset);
		return lines(reader).closeAtEnd(reader);
	}

	public static Streamlet<String> lines(Reader reader) {
		BufferedReader br = new BufferedReader(reader);
		return from(() -> {
			try {
				return br.readLine();
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}).closeAtEnd(br);
	}

	@SafeVarargs
	public static <T> Streamlet<T> from(T... col) {
		return from(Arrays.asList(col));
	}

	public static <K, V> Streamlet<Pair<K, V>> from(Map<K, V> map) {
		return Read.from(map.entrySet()).map(e -> Pair.of(e.getKey(), e.getValue()));
	}

	public static <T> Streamlet<T> from(Collection<T> col) {
		return from(To.source(col));
	}

	public static <T> Streamlet<T> from(Source<T> source) {
		return new Streamlet<>(source);
	}

}
