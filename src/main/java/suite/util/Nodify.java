package suite.util;

import static primal.statics.Fail.fail;
import static primal.statics.Rethrow.ex;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import primal.Verbs.Instantiate;
import primal.Verbs.New;
import primal.adt.Pair;
import primal.fp.Funs.Fun;
import suite.inspect.Inspect;
import suite.node.Atom;
import suite.node.Dict;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Str;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.primitive.Chars;
import suite.streamlet.Read;

/**
 * Convert (supposedly) any Java structures to nodes.
 *
 * @author ywsing
 */
public class Nodify {

	private Set<Type> collectionClasses = Set.of(ArrayList.class, Collection.class, HashSet.class, List.class, Set.class);
	private Set<Type> mapClasses = Set.of(HashMap.class, Map.class);

	private Map<Type, Nodifier> nodifiers = new ConcurrentHashMap<>();
	private Inspect inspect;

	private class FieldInfo {
		private Field field;
		private String name;
		private Nodifier nodifier;

		private FieldInfo(Field field, String name, Nodifier nodifier) {
			this.field = field;
			this.name = name;
			this.nodifier = nodifier;
		}
	}

	private class Nodifier {
		private Fun<Object, Node> nodify;
		private Fun<Node, Object> unnodify;

		private Nodifier(Fun<Object, Node> nodify, Fun<Node, Object> unnodify) {
			this.nodify = nodify;
			this.unnodify = unnodify;
		}
	}

	public Nodify(Inspect inspect) {
		this.inspect = inspect;
	}

	public <T> Node nodify(Class<T> clazz, T t) {
		return apply_(t, getNodifier(clazz));
	}

	public <T> T unnodify(Class<T> clazz, Node node) {
		@SuppressWarnings("unchecked")
		var t = (T) apply_(node, getNodifier(clazz));
		return t;
	}

	private Nodifier getNodifier(Type type) {
		var nodifier = nodifiers.get(type);
		if (nodifier == null) {
			nodifiers.put(type, new Nodifier(o -> apply_(o, getNodifier(type)), n -> apply_(n, getNodifier(type))));
			nodifiers.put(type, nodifier = newNodifier(type));
		}
		return nodifier;
	}

	@SuppressWarnings("unchecked")
	private Nodifier newNodifier(Type type) {
		return new Switch<Nodifier>(type //
		).applyIf(Class.class, clazz -> {
			if (clazz == boolean.class)
				return new Nodifier(o -> Atom.of(o.toString()), n -> n == Atom.TRUE);
			else if (clazz == int.class)
				return new Nodifier(o -> Int.of((Integer) o), Int::num);
			else if (clazz == Chars.class)
				return new Nodifier(o -> new Str(o.toString()), n -> To.chars(Str.str(n)));
			else if (clazz == String.class)
				return new Nodifier(o -> new Str(o.toString()), Str::str);
			else if (clazz.isEnum())
				return new Nodifier(o -> Atom.of(o.toString()),
						Read.from(clazz.getEnumConstants()).toMap(e -> Atom.of(e.toString()))::get);
			else if (clazz.isArray()) {
				var componentType = clazz.getComponentType();
				var nodifier1 = getNodifier(componentType);
				Fun<Object, Node> forward = o -> {
					Node node = Atom.NIL;
					for (var i = Array.getLength(o) - 1; 0 <= i; i--)
						node = Tree.ofOr(apply_(Array.get(o, i), nodifier1), node);
					return node;
				};
				return new Nodifier(forward, n -> {
					var list = Tree //
							.read(n, TermOp.OR____) //
							.map(n_ -> apply_(n_, nodifier1)) //
							.toList();
					return To.array_(list.size(), componentType, list::get);
				});
			} else if (clazz.isInterface()) // polymorphism
				return new Nodifier(o -> {
					var clazz1 = o.getClass();
					var n = apply_(o, getNodifier(clazz1));
					return Tree.of(TermOp.COLON_, Atom.of(clazz1.getName()), n);
				}, n -> {
					var tree = Tree.decompose(n, TermOp.COLON_);
					if (tree != null) {
						var clazz1 = ex(() -> Class.forName(Atom.name(tree.getLeft())));
						return apply_(tree.getRight(), getNodifier(clazz1));
					} else
						// happens when an enum implements an interface
						return fail("cannot instantiate enum from interfaces");
				});
			else {
				var pairs = inspect //
						.fields(clazz) //
						.map(field -> new FieldInfo(field, field.getName(), getNodifier(field.getGenericType()))) //
						.map(f -> Pair.of(Atom.of(f.name), f)) //
						.toList();

				return new Nodifier(o -> ex(() -> {
					var map = new HashMap<Node, Reference>();
					for (var pair : pairs) {
						var fieldInfo = pair.v;
						var value = apply_(fieldInfo.field.get(o), fieldInfo.nodifier);
						map.put(pair.k, Reference.of(value));
					}
					return Dict.of(map);
				}), n -> ex(() -> {
					var map = Dict.m(n);
					var o1 = New.clazz(clazz);
					for (var pair : pairs) {
						var fieldInfo = pair.v;
						var value = map.get(pair.k).finalNode();
						fieldInfo.field.set(o1, apply_(value, fieldInfo.nodifier));
					}
					return o1;
				}));
			}
		}).applyIf(ParameterizedType.class, pt -> {
			var rawType = pt.getRawType();
			var typeArgs = pt.getActualTypeArguments();
			var clazz = rawType instanceof Class ? (Class<?>) rawType : null;

			if (collectionClasses.contains(clazz)) {
				var nodifier1 = getNodifier(typeArgs[0]);
				return new Nodifier(o -> {
					Tree start = Tree.of(null, null, null), tree = start;
					for (var o_ : (Collection<?>) o) {
						var tree0 = tree;
						Tree.forceSetRight(tree0, tree = Tree.ofOr(apply_(o_, nodifier1), null));
					}
					Tree.forceSetRight(tree, Atom.NIL);
					return start.getRight();
				}, n -> {
					var list = Tree.read(n, TermOp.OR____).map(n_ -> apply_(n_, nodifier1)).toList();
					var o1 = (Collection<Object>) Instantiate.clazz(clazz);
					o1.addAll(list);
					return o1;
				});
			} else if (mapClasses.contains(clazz)) {
				var kn = getNodifier(typeArgs[0]);
				var vn = getNodifier(typeArgs[1]);
				return new Nodifier(o -> {
					var map = new HashMap<Node, Reference>();
					for (var e : ((Map<?, ?>) o).entrySet())
						map.put(apply_(e.getKey(), kn), Reference.of(apply_(e.getValue(), vn)));
					return Dict.of(map);
				}, n -> {
					var map = Dict.m(n);
					var object1 = (Map<Object, Object>) Instantiate.clazz(clazz);
					for (var e : map.entrySet())
						object1.put(apply_(e.getKey(), kn), apply_(e.getValue().finalNode(), vn));
					return object1;
				});
			} else
				return getNodifier(rawType);
		}).nonNullResult();
	}

	private Node apply_(Object object, Nodifier nodifier) {
		return object != null ? nodifier.nodify.apply(object) : Atom.NULL;
	}

	private Object apply_(Node node, Nodifier nodifier) {
		return node != Atom.NULL ? nodifier.unnodify.apply(node) : null;
	}

}
