package suite.ansi;

import java.util.ArrayList;
import java.util.List;

import suite.adt.Trie;
import suite.adt.pair.Pair;
import suite.streamlet.Read;
import suite.streamlet.Signal;
import suite.streamlet.Signal.Redirector;
import suite.util.FunUtil.Sink;

public class Keyboard {

	private LibcJna libc;
	private Trie<Integer, VK> trie = new Trie<>();

	public enum VK {
		ALT_J____, //
		ALT_DOWN_, //
		ALT_LEFT_, //
		ALT_UP___, //
		ALT_RIGHT, //
		BKSP_, //
		CTRL_C____, //
		CTRL_DOWN_, //
		CTRL_LEFT_, //
		CTRL_Q____, //
		CTRL_RIGHT, //
		CTRL_UP___, //
		CTRL_V____, //
		CTRL_W____, //
		CTRL_X____, //
		CTRL_Y____, //
		CTRL_Z____, //
		DEL__, //
		DOWN_, //
		END__, //
		HOME_, //
		INS__, //
		LEFT_, //
		PGUP_, //
		PGDN_, //
		RIGHT, //
		SHIFT_DOWN_, //
		SHIFT_LEFT_, //
		SHIFT_UP___, //
		SHIFT_RIGHT, //
		UP___, //
	}

	public Keyboard(LibcJna libc) {
		this.libc = libc;

		List<Pair<List<Integer>, VK>> pairs = new ArrayList<>();
		pairs.add(Pair.of(List.of(3), VK.CTRL_C____));
		pairs.add(Pair.of(List.of(17), VK.CTRL_Q____));
		pairs.add(Pair.of(List.of(22), VK.CTRL_V____));
		pairs.add(Pair.of(List.of(23), VK.CTRL_W____));
		pairs.add(Pair.of(List.of(24), VK.CTRL_X____));
		pairs.add(Pair.of(List.of(25), VK.CTRL_Y____));
		pairs.add(Pair.of(List.of(26), VK.CTRL_Z____));
		pairs.add(Pair.of(List.of(27, 27, 91, 65), VK.ALT_UP___));
		pairs.add(Pair.of(List.of(27, 27, 91, 66), VK.ALT_DOWN_));
		pairs.add(Pair.of(List.of(27, 27, 91, 67), VK.ALT_RIGHT));
		pairs.add(Pair.of(List.of(27, 27, 91, 68), VK.ALT_LEFT_));
		pairs.add(Pair.of(List.of(27, 74), VK.ALT_J____)); // alt-J
		pairs.add(Pair.of(List.of(27, 79, 97), VK.CTRL_UP___));// urxvt
		pairs.add(Pair.of(List.of(27, 79, 98), VK.CTRL_DOWN_));// urxvt
		pairs.add(Pair.of(List.of(27, 79, 99), VK.CTRL_RIGHT));// urxvt
		pairs.add(Pair.of(List.of(27, 79, 100), VK.CTRL_LEFT_));// urxvt
		pairs.add(Pair.of(List.of(27, 91, 49, 59, 50, 65), VK.SHIFT_UP___)); // terminator
		pairs.add(Pair.of(List.of(27, 91, 49, 59, 50, 66), VK.SHIFT_DOWN_)); // terminator
		pairs.add(Pair.of(List.of(27, 91, 49, 59, 50, 67), VK.SHIFT_RIGHT)); // terminator
		pairs.add(Pair.of(List.of(27, 91, 49, 59, 50, 68), VK.SHIFT_LEFT_)); // terminator
		pairs.add(Pair.of(List.of(27, 91, 49, 59, 53, 65), VK.CTRL_UP___)); // terminator
		pairs.add(Pair.of(List.of(27, 91, 49, 59, 53, 66), VK.CTRL_DOWN_)); // terminator
		pairs.add(Pair.of(List.of(27, 91, 49, 59, 53, 67), VK.CTRL_RIGHT)); // terminator
		pairs.add(Pair.of(List.of(27, 91, 49, 59, 53, 68), VK.CTRL_LEFT_)); // terminator
		pairs.add(Pair.of(List.of(27, 91, 97), VK.SHIFT_UP___));// urxvt
		pairs.add(Pair.of(List.of(27, 91, 98), VK.SHIFT_DOWN_));// urxvt
		pairs.add(Pair.of(List.of(27, 91, 99), VK.SHIFT_RIGHT));// urxvt
		pairs.add(Pair.of(List.of(27, 91, 100), VK.SHIFT_LEFT_));// urxvt
		pairs.add(Pair.of(List.of(27, 91, 50, 126), VK.INS__));
		pairs.add(Pair.of(List.of(27, 91, 51, 126), VK.DEL__));
		pairs.add(Pair.of(List.of(27, 91, 53, 126), VK.PGUP_));
		pairs.add(Pair.of(List.of(27, 91, 54, 126), VK.PGDN_));
		pairs.add(Pair.of(List.of(27, 91, 65), VK.UP___));
		pairs.add(Pair.of(List.of(27, 91, 66), VK.DOWN_));
		pairs.add(Pair.of(List.of(27, 91, 67), VK.RIGHT));
		pairs.add(Pair.of(List.of(27, 91, 68), VK.LEFT_));
		pairs.add(Pair.of(List.of(27, 91, 70), VK.END__));
		pairs.add(Pair.of(List.of(27, 91, 72), VK.HOME_));
		pairs.add(Pair.of(List.of(27, 106), VK.ALT_J____)); // alt-j
		pairs.add(Pair.of(List.of(127), VK.BKSP_));

		for (Pair<List<Integer>, VK> pair : pairs)
			trie.add(pair.t0, pair.t1);
	}

	public void loop(Sink<Signal<Pair<VK, Character>>> sink) {
		Signal.loop(this::get, signal -> sink.sink(signal.redirect(redirector)));
	}

	public Signal<Pair<VK, Character>> signal() {
		return Signal.from(this::get).redirect(redirector);
	}

	private Character get() {
		int ch = libc.getchar();
		return 0 <= ch ? (char) ch : null;
	}

	private Redirector<Character, Pair<VK, Character>> redirector = new Redirector<>() {
		private List<Character> chs = new ArrayList<>();
		private Trie<Integer, VK> t = trie;

		public void accept(Character ch_, Sink<Pair<VK, Character>> fire) {
			if (ch_ != null) {
				Trie<Integer, VK> t1 = t.getMap().get((int) ch_);
				VK vk;

				chs.add(ch_);

				if (t1 != null)
					if ((vk = (t = t1).getValue()) != null) {
						fire.sink(Pair.of(vk, null));
						reset();
					} else
						;
				else
					flush(fire);
			} else
				flush(fire);
		}

		private void flush(Sink<Pair<VK, Character>> fire) {
			Read.from(chs).sink(ch -> fire.sink(Pair.of(null, ch)));
			reset();
		}

		private void reset() {
			chs.clear();
			t = trie;
		}
	};

}
