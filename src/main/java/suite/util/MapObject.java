package suite.util;

import java.util.HashMap;

import suite.adt.IdentityKey;
import suite.streamlet.Read;

public abstract class MapObject<T extends MapObject<T>> extends BaseObject<T> implements Cloneable, Comparable<T>, MapInterface<T> {

	@Override
	public MapObject<T> clone() {
		var map = new HashMap<IdentityKey<?>, MapObject<?>>();

		return Rethrow.ex(() -> {
			@SuppressWarnings("unchecked")
			T object = (T) new Object() {
				private MapObject<?> clone(MapObject<?> t0) throws IllegalAccessException {
					var key = IdentityKey.of(t0);
					var tx = map.get(key);
					if (tx == null) {
						var list0 = Read.from(MapObject_.list(t0));
						var list1 = list0.map(v -> v instanceof MapObject ? ((MapObject<?>) v).clone() : v).toList();
						map.put(key, tx = MapObject_.construct(getClass(), list1));
					}
					return tx;
				}
			}.clone(this);

			return object;
		});
	}

	@Override
	protected AutoObject_<T> autoObject() {
		return new AutoObject_<>(MapObject_::list);
	}

}
