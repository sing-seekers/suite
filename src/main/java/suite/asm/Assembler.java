package suite.asm;

import static suite.util.Friends.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import suite.Suite;
import suite.adt.pair.Pair;
import suite.asm.Assembler.Asm;
import suite.assembler.Amd64Assemble;
import suite.assembler.Amd64Parse;
import suite.cfg.Defaults;
import suite.lp.Trail;
import suite.lp.doer.Binder;
import suite.lp.doer.Generalizer;
import suite.lp.kb.RuleSet;
import suite.lp.search.ProverBuilder.Finder;
import suite.lp.search.SewingProverBuilder2;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Str;
import suite.node.Tree;
import suite.node.io.SwitchNode;
import suite.parser.CommentPreprocessor;
import suite.primitive.Bytes;
import suite.primitive.Bytes.BytesBuilder;
import suite.streamlet.FunUtil.Fun;
import suite.streamlet.Read;
import suite.text.Preprocess;
import suite.text.Preprocess.Run;
import suite.util.Fail;
import suite.util.List_;
import suite.util.String_;
import suite.util.To;

public class Assembler {

	private Asm asm;
	private Fun<List<Pair<Reference, Node>>, List<Pair<Reference, Node>>> preassemble;

	public interface Asm {
		public Bytes assemble(boolean isPass2, int address, Node instruction);
	}

	public Assembler(int bits) {
		this(bits, false);
	}

	public Assembler(int bits, boolean isLongMode) {
		this(bits, isLongMode, lnis -> lnis);
	}

	public Assembler(int bits, boolean isLongMode,
			Fun<List<Pair<Reference, Node>>, List<Pair<Reference, Node>>> preassemble) {
		asm = Boolean.TRUE ? new AsmA(bits) : new AsmSl(bits, isLongMode);
		this.preassemble = preassemble;
	}

	public Bytes assemble(String in0) {
		var whitespaces = Collections.singleton('\n');
		Fun<String, List<Run>> gct = CommentPreprocessor.ofGroupComment(whitespaces)::preprocess;
		Fun<String, List<Run>> lct = CommentPreprocessor.ofLineComment(whitespaces)::preprocess;
		var in1 = Preprocess.transform(List.of(gct, lct), in0).t0;

		var generalizer = new Generalizer();
		var lines = List.of(in1.split("\n"));
		Pair<String, String> pe;
		var start = 0;

		while ((pe = String_.split2(lines.get(start), "=")) != null) {
			generalizer.getVariable(Atom.of(pe.t0)).bound(Suite.parse(pe.t1));
			start++;
		}

		var lnis = Read //
				.from(List_.right(lines, start)) //
				.map(line -> String_.split2l(line, "\t").map((label, command) -> {
					var reference = String_.isNotBlank(label) //
							? generalizer.getVariable(Atom.of(label)) //
							: new Reference();
					var instruction = generalizer.generalize(Suite.parse(command));
					return Pair.of(reference, instruction);
				})).toList();

		return assemble(generalizer, lnis);
	}

	public Bytes assemble(Node input) {
		var lnis = new ArrayList<Pair<Reference, Node>>();
		var generalizer = new Generalizer();
		var trail = new Trail();

		for (var node : Tree.read(input))
			new SwitchNode<Boolean>(generalizer.generalize(node) //
			).match(".0 = .1", (l, r) -> {
				return Binder.bind(l, r, trail) || Fail.b("bind failed");
			}).match(".0 .1", (l, r) -> {
				return lnis.add(Pair.of((Reference) l, r));
			}).nonNullResult();

		return assemble(generalizer, preassemble.apply(lnis));
	}

	private Bytes assemble(Generalizer generalizer, List<Pair<Reference, Node>> lnis) {
		var org = Int.num(generalizer.getVariable(Atom.of(".org")).finalNode());
		var out = new BytesBuilder();

		for (var isPass2 : new boolean[] { false, true, }) {
			AssemblePredicates.isPass2 = isPass2;
			out.clear();

			for (var lni : lnis) {
				Bytes bytes = lni.map((reference, instruction) -> {
					var address = org + out.size();

					if (!isPass2)
						reference.bound(Int.of(address));
					else if (Int.num(reference.finalNode()) != address)
						fail("address varied between passes at " + Integer.toHexString(address) + ": " + instruction);

					try {
						return asm.assemble(isPass2, address, instruction);
					} catch (Exception ex) {
						return fail("in " + instruction + " during pass " + (!isPass2 ? "1" : "2"), ex);
					}
				});
				out.append(bytes);
			}

			if (isPass2)
				for (var lni : lnis)
					lni.t0.unbound();
		}

		return out.toBytes();
	}

}

class AsmA implements Asm {

	private Amd64Assemble aa;
	private Amd64Parse ap;

	public AsmA(int bits) {
		aa = new Amd64Assemble(bits / 8);
		ap = new Amd64Parse();
	}

	public Bytes assemble(boolean isPass2, int address, Node instruction) {
		if (instruction == Atom.NIL)
			return Bytes.empty;
		else if (instruction instanceof Str)
			return Bytes.of(Str.str(instruction).getBytes(Defaults.charset));
		else
			return aa.assemble(isPass2, address, ap.parse(instruction));
	}

}

class AsmSl implements Asm {

	private RuleSet ruleSet;
	private Finder finder;
	private int bits;

	public AsmSl(int bits, boolean isLongMode) {
		ruleSet = Suite.newRuleSet(List.of("asm.sl", "auto.sl"));

		if (isLongMode)
			Suite.addRule(ruleSet, "as-long-mode");

		finder = new SewingProverBuilder2() //
				.build(ruleSet) //
				.apply(Suite.parse("" //
						+ "source (.bits, .address, .instruction,)" //
						+ ", asi:.bits:.address .instruction .code" //
						+ ", sink .code"));

		this.bits = bits;
	}

	public Bytes assemble(boolean isPass2, int address, Node instruction) {
		var ins = Suite.substitute(".0, .1, .2,", Int.of(bits), Int.of(address), instruction);
		var bytesList = new ArrayList<Bytes>();
		finder.find(To.source(ins), node -> bytesList.add(convertByteStream(node)));
		return Read.from(bytesList).min((bytes0, bytes1) -> bytes0.size() - bytes1.size());

	}

	private Bytes convertByteStream(Node node) {
		var bb = new BytesBuilder();
		for (var n : Tree.read(node))
			bb.append((byte) Int.num(n));
		return bb.toBytes();
	}

}
