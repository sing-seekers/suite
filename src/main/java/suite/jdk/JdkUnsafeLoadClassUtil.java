package suite.jdk;

import static suite.util.Friends.rethrow;

import java.nio.file.Files;
import java.nio.file.Path;

import suite.os.LogUtil;
import suite.util.Object_;

public class JdkUnsafeLoadClassUtil extends JdkUtil {

	public JdkUnsafeLoadClassUtil(Path srcDir, Path binDir) {
		super(srcDir, binDir);
	}

	public <T> T newInstance(Class<T> interfaceClazz, String canonicalName, String java) {
		return Object_.new_(load(interfaceClazz, canonicalName, compile(canonicalName, java)));
	}

	private <T> Class<? extends T> load(Class<T> interfaceClazz, String canonicalName, Path path) {
		LogUtil.info("Loading class " + canonicalName);
		var bytes = rethrow(() -> Files.readAllBytes(path));
		return new UnsafeUtil().defineClass(interfaceClazz, canonicalName, bytes);
	}

}
