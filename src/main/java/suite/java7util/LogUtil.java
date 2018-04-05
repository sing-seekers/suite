package suite.java7util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

import org.apache.commons.logging.LogFactory;

import suite.inspect.Dump;
import suite.util.Array_;

@Deprecated
public class LogUtil {

	private static int maxStackTraceLength = 99;

	public static <I> I proxy(Class<I> interface_, I object) {
		@SuppressWarnings("unchecked")
		Class<I> clazz = (Class<I>) object.getClass();
		var log = LogFactory.getLog(clazz);

		InvocationHandler handler = (proxy, method, ps) -> {
			var methodName = method.getName();
			var prefix = methodName + "()\n";
			var sb = new StringBuilder();

			sb.append(prefix);

			if (ps != null)
				for (var i = 0; i < ps.length; i++)
					Dump.object("p" + i, ps[i], sb);

			log.info(sb.toString());

			try {
				Object value = method.invoke(object, ps);
				String rd = Dump.object("return", value);
				log.info(prefix + rd);
				return value;
			} catch (InvocationTargetException ite) {
				var th = ite.getTargetException();
				var isTrimmed = trimStackTrace(th);
				log.error(prefix + (isTrimmed ? "(Trimmed)" : ""), th);
				throw th instanceof Exception ? (Exception) th : ite;
			}
		};

		var classLoader = clazz.getClassLoader();
		Class<?>[] classes = { interface_ };

		@SuppressWarnings("unchecked")
		I proxied = (I) Proxy.newProxyInstance(classLoader, classes, handler);
		return proxied;
	}

	private static boolean trimStackTrace(Throwable th) {
		var isTrimmed = false;

		// trims stack trace to appropriate length
		while (th != null) {
			StackTraceElement[] st0 = th.getStackTrace();

			if (maxStackTraceLength < st0.length) {
				StackTraceElement[] st1 = new StackTraceElement[maxStackTraceLength];
				Array_.copy(st0, 0, st1, 0, maxStackTraceLength);
				th.setStackTrace(st1);

				isTrimmed = true;
			}

			th = th.getCause();
		}

		return isTrimmed;
	}

}
