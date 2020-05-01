package suite.concurrent;

import primal.Verbs.Wait;
import suite.concurrent.stm.Concurrent.DeadlockException;

import java.io.Closeable;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static primal.statics.Fail.fail;

/**
 * Mutual exclusion lock with deadlock detection.
 *
 * @author ywsing
 */
public class Mutex {

	private static Object bigLock = new Object();
	private static Map<Thread, Mutex> waitees = new WeakHashMap<>();

	private AtomicReference<Thread> owner = new AtomicReference<>();
	private int depth;

	public static class MutexLock implements Closeable {
		private Mutex mutex;

		public MutexLock(Mutex mutex) {
			this.mutex = mutex;
			mutex.lock();
		}

		@Override
		public void close() {
			mutex.unlock();
		}
	}

	public void lock() {
		synchronized (bigLock) {
			var currentThread = Thread.currentThread();
			waitees.put(currentThread, this);
			try {
				while (!owner.compareAndSet(null, currentThread)) {
					var mutex = this;

					while (mutex != null) {
						var ownerThread = mutex.owner.get();
						mutex = ownerThread != null ? waitees.get(ownerThread) : null;
						if (mutex == this)
							throw new DeadlockException();
					}

					Wait.object(bigLock);
				}

				depth++;
			} finally {
				waitees.remove(currentThread);
			}
		}
	}

	public void unlock() {
		synchronized (bigLock) {
			if (--depth == 0)
				if (owner.compareAndSet(Thread.currentThread(), null))
					bigLock.notifyAll();
				else
					fail("lock unlocked by wrong thread");
		}
	}

}
