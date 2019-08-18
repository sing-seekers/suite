package suite.concurrent;

import primal.adt.Mutable;
import primal.persistent.PerList;

public class LockFreeQueue<T> {

	private CasReference<BackFront> cas = new CasReference<>(new BackFront(PerList.end(), PerList.end()));

	private class BackFront {
		private PerList<T> back;
		private PerList<T> front;

		private BackFront(PerList<T> back, PerList<T> front) {
			this.back = back;
			this.front = front;
		}
	}

	public void enqueue(T t) {
		cas.apply(queue0 -> new BackFront(PerList.cons(t, queue0.back), queue0.front));
	}

	/**
	 * @return null if the queue is empty.
	 */
	public T dequeue() {
		var result = Mutable.<T> nil();

		cas.apply(fb0 -> {
			var back = fb0.back;
			var front = fb0.front;
			if (front.isEmpty()) { // reverse elements from back to front
				for (var t_ : fb0.back)
					front = PerList.cons(t_, front);
				back = PerList.end();
			}
			if (!front.isEmpty()) {
				result.set(front.head);
				return new BackFront(back, front.tail);
			} else {
				result.set(null);
				return fb0;
			}
		});

		return result.value();
	}

}
