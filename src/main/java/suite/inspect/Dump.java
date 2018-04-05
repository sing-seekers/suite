package suite.inspect;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import suite.jdk.gen.Type_;
import suite.node.util.Singleton;
import suite.os.LogUtil;
import suite.util.FunUtil.Sink;
import suite.util.Thread_;

public class Dump {

	private static Inspect inspect = Singleton.me.inspect;

	public static <T> T t(T t) {
		out(t);
		return t;
	}

	/**
	 * Dumps object content (public data and getters) through Reflection to a log4j.
	 */
	public static void out(Object object) {
		var trace = Thread_.getStackTrace(3);
		out(trace.getClassName() + "." + trace.getMethodName(), object);
	}

	/**
	 * Dumps object content (public data and getters) through Reflection to a log4j,
	 * with a descriptive name which you gave.
	 */
	public static void out(String name, Object object) {
		var sb = new StringBuilder();
		sb.append("Dumping ");
		sb.append(name);
		Dump.object("", object, sb);
		LogUtil.info(sb.toString());
	}

	public static String object(Object object) {
		return object("", object);
	}

	/**
	 * Dumps object content (public data and getters) through Reflection to a
	 * string, line-by-line.
	 *
	 * Private fields are not dumped.
	 *
	 * @param prefix
	 *            To be appended before each line.
	 * @param object
	 *            The monster.
	 */
	public static String object(String prefix, Object object) {
		var sb = new StringBuilder();
		object(prefix, object, sb);
		return sb.toString();
	}

	public static void object(String prefix, Object object, StringBuilder sb) {
		object(prefix, object, sb::append);
	}

	private static void object(String prefix, Object object, Sink<String> sink) {
		var dumpedIds = new HashSet<>();

		new Object() {
			private void d(String prefix, Object object) {
				d(prefix, object != null ? object.getClass() : void.class, object);
			}

			private void d(String prefix, Class<?> clazz, Object object) {
				var id = System.identityHashCode(object);
				sink.sink(prefix);
				sink.sink(" =");

				if (object == null)
					sink.sink(" null\n");
				else if (dumpedIds.add(id))
					try {
						if (clazz == String.class)
							sink.sink(" \"" + object + "\"");

						if (!Collection.class.isAssignableFrom(clazz))
							sink.sink(" " + object);

						sink.sink(" [" + clazz.getSimpleName() + "]\n");

						var count = 0;

						// simple listings for simple classes
						if (Type_.isSimple(clazz))
							;
						else if (clazz.isArray())
							for (var i = 0; i < Array.getLength(object); i++)
								d(prefix + "[" + count++ + "]", Array.get(object, i));
						else if (Collection.class.isAssignableFrom(clazz))
							for (var o1 : (Collection<?>) object)
								d(prefix + "[" + count++ + "]", o1);
						else if (Map.class.isAssignableFrom(clazz))
							for (var e : ((Map<?, ?>) object).entrySet()) {
								Object key = e.getKey(), value = e.getValue();
								d(prefix + "[" + count + "].getKey()", key);
								d(prefix + "[" + count + "].getValue()", value);
								count++;
							}
						else {
							for (var field : inspect.fields(clazz))
								try {
									var name = field.getName();
									var o = field.get(object);
									Class<?> type = field.getType();
									if (Type_.isSimple(type))
										d(prefix + "." + name, type, o);
									else
										d(prefix + "." + name, o);
								} catch (Throwable ex) {
									sink.sink(prefix + "." + field.getName());
									sink.sink(" caught " + ex + "\n");
								}

							for (var method : inspect.getters(clazz)) {
								var name = method.getName();
								try {
									var o = method.invoke(object);
									if (!(o instanceof Class<?>))
										d(prefix + "." + name + "()", o);
								} catch (Throwable ex) {
									sink.sink(prefix + "." + name + "()");
									sink.sink(" caught " + ex + "\n");
								}
							}
						}
					} finally {
						dumpedIds.remove(id);
					}
				else
					sink.sink(" <<recursed>>");
			}
		}.d(prefix, object);
	}

}
