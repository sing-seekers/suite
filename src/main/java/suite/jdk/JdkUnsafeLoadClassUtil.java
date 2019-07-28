package suite.jdk;

import static suite.util.Rethrow.ex;

import java.nio.file.Files;
import java.nio.file.Path;

import suite.object.Object_;
import suite.os.Log_;

public class JdkUnsafeLoadClassUtil extends JdkUtil {

	public JdkUnsafeLoadClassUtil(Path srcDir, Path binDir) {
		super(srcDir, binDir);
	}

	public <T> T newInstance(Class<T> interfaceClazz, String canonicalName, String java) {
		return Object_.new_(load(interfaceClazz, canonicalName, compile(canonicalName, java)));
	}

	private <T> Class<? extends T> load(Class<T> interfaceClazz, String canonicalName, Path path) {
		Log_.info("Loading class " + canonicalName);
		var bytes = ex(() -> Files.readAllBytes(path));
		return new UnsafeUtil().defineClass(interfaceClazz, canonicalName, bytes);
	}

}
