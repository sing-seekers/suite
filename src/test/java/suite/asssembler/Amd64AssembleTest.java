package suite.asssembler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import primal.primitive.adt.Bytes;
import primal.primitive.puller.IntPuller;
import suite.assembler.Amd64;
import suite.assembler.Amd64.Insn;
import suite.assembler.Amd64Assemble;
import suite.assembler.Amd64Mode;

public class Amd64AssembleTest {

	private Amd64 amd64 = Amd64.me;

	@Test
	public void test() {
		var push = amd64.instruction(Insn.PUSH, amd64.imm(0l, 1));
		var bytes = new Amd64Assemble(Amd64Mode.LONG64).assemble(0l, List.of(push), true);
		System.out.println(bytes);
		assertEquals(2, bytes.size());
	}

	@Test
	public void test16() {
		var mov = amd64.instruction(Insn.MOV, amd64.mem(amd64.imm(0, 2), 2), amd64.imm(0x7042, 2));
		var bytes = new Amd64Assemble(Amd64Mode.REAL16).assemble(0l, List.of(mov), true);
		System.out.println(bytes);

		var is = new int[] { 0xC7, 0x06, 0x00, 0x00, 0x42, 0x70, };
		var bs = IntPuller.of(is).map(i -> Bytes.of((byte) i));
		assertEquals(Bytes.of(bs), bytes);
	}

	@Test
	public void test64() {
		var mov = amd64.instruction(Insn.MOV, amd64.mem(amd64.reg("RBX"), 0l, 8), amd64.imm64(1l << 4));
		var bytes = new Amd64Assemble(Amd64Mode.LONG64).assemble(0l, List.of(mov), true);
		System.out.println(bytes);

		var is = new int[] { 0x48, 0xC7, 0x03, 0x10, 0x00, 0x00, 0x00, };
		var bs = IntPuller.of(is).map(i -> Bytes.of((byte) i));
		assertEquals(Bytes.of(bs), bytes);
	}

	@Test
	public void testFail() {
		try {
			var mov = amd64.instruction(Insn.MOV, amd64.reg("BPL"), amd64.reg("AH"));
			new Amd64Assemble(Amd64Mode.LONG64).assemble(0l, List.of(mov), true);
			assertTrue(false);
		} catch (RuntimeException e) {
			assertEquals("bad instruction", e.getCause().getMessage());
		}

		try {
			var mov = amd64.instruction(Insn.MOV, amd64.mem(amd64.reg("RBX"), 0l, 8), amd64.imm64(1l << 40));
			new Amd64Assemble(Amd64Mode.LONG64).assemble(0l, List.of(mov), true);
			assertTrue(false);
		} catch (RuntimeException e) {
			assertEquals("bad instruction", e.getCause().getMessage());
		}
	}

}
