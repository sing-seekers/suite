package org.instructionexecutor;

import java.util.ArrayList;
import java.util.List;

import org.instructionexecutor.InstructionUtil.Activation;
import org.instructionexecutor.InstructionUtil.CutPoint;
import org.instructionexecutor.InstructionUtil.Frame;
import org.instructionexecutor.InstructionUtil.Instruction;
import org.suite.Binder;
import org.suite.Journal;
import org.suite.doer.Prover;
import org.suite.node.Node;
import org.suite.predicates.SystemPredicates;
import org.util.Util;
import org.util.Util.Sink;

public class LogicInstructionExecutor extends InstructionExecutor {

	private Prover prover;
	private Journal journal;
	private SystemPredicates systemPredicates;
	private Sink<Node> sink;

	private static final int stackSize = 4096;

	private int bindPoints[] = new int[stackSize];
	private List<CutPoint> cutPoints = new ArrayList<CutPoint>();
	private int bsp = 0;

	public LogicInstructionExecutor(Prover prover, Node node, Sink<Node> sink) {
		super(node);
		this.prover = prover;
		journal = prover.getJournal();
		systemPredicates = new SystemPredicates(prover);
		this.sink = sink;
	}

	@Override
	protected void execute(Exec exec, Instruction insn) {
		Activation current = exec.current;
		Frame frame = current.frame;
		Node regs[] = frame != null ? frame.registers : null;

		switch (insn.insn) {
		case BIND__________:
			bindPoints[bsp++] = journal.getPointInTime();
			Node node0 = regs[insn.op0];
			Node node1 = regs[insn.op1];
			if (!Binder.bind(node0, node1, journal))
				current.ip = insn.op2; // Fail
			break;
		case BINDUNDO______:
			journal.undoBinds(bindPoints[--bsp]);
			break;
		case CUTBEGIN______:
			regs[insn.op0] = i(cutPoints.size());
			cutPoints.add(new CutPoint(current, bsp, journal.getPointInTime()));
			break;
		case CUTFAIL_______:
			int cutPointIndex = g(regs[insn.op0]);
			CutPoint cutPoint = cutPoints.get(cutPointIndex);
			Util.truncate(cutPoints, cutPointIndex);
			current = cutPoint.activation;
			bsp = cutPoint.bindStackPointer;
			journal.undoBinds(cutPoint.journalPointer);
			current.ip = insn.op1;
			break;
		case PROVEINTERPRET:
			if (!prover.prove(regs[insn.op0]))
				current.ip = insn.op1;
			break;
		case PROVESYS______:
			if (!systemPredicates.call(regs[insn.op0]))
				current.ip = insn.op1;
			break;
		case SINK__________:
			sink.apply(regs[insn.op0]);
			break;
		default:
			super.execute(exec, insn);
		}
	}

}
