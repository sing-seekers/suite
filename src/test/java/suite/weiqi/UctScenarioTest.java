package suite.weiqi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import suite.uct.ShuffleUtil;
import suite.uct.UctSearch;
import suite.uct.UctWeiqi;
import suite.weiqi.Weiqi.Occupation;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UctScenarioTest {

	@BeforeEach
	public void before() {
		Weiqi.adjustSize(7);
	}

	@Test
	public void testEat() {
		ShuffleUtil.setSeed(-3089117486356251879l);
		var gameSet = new GameSet(UserInterface.importBoard("" //
				+ ". . . . . . . \n" //
				+ ". . X X O . . \n" //
				+ ". . X O O . . \n" //
				+ ". X X O X O . \n" //
				+ "X O O X X O . \n" //
				+ ". X O X O . . \n" //
				+ ". X . . . . . \n" //
		), Occupation.WHITE);
		testScenario(gameSet, Coordinate.c(6, 3));
	}

	@Test
	public void testCapture() {
		ShuffleUtil.setSeed(-5334561483001877403l);
		var gameSet = new GameSet(UserInterface.importBoard("" //
				+ ". . . . . . . \n" //
				+ ". . . X O . . \n" //
				+ ". . X O O . . \n" //
				+ ". . X O O . . \n" //
				+ ". . O X X O . \n" //
				+ ". . . X O O . \n" //
				+ ". . . . . . . \n" //
		), Occupation.BLACK);
		testScenario(gameSet, Coordinate.c(5, 2));
	}

	@Test
	public void testLiveAndDeath() {
		ShuffleUtil.setSeed(-1900234906508089780l);
		var gameSet = new GameSet(UserInterface.importBoard("" //
				+ "X X X X X X X \n" //
				+ "X . . X X . X \n" //
				+ "O X X O X . X \n" //
				+ "O . . O O X X \n" //
				+ "O O O . O O O \n" //
				+ "O O . . . O O \n" //
				+ "O O O . . O O \n" //
		), Occupation.BLACK);
		testScenario(gameSet, Coordinate.c(5, 3));
	}

	@Test
	public void testLiveAndDeath1() {
		ShuffleUtil.setSeed(1594738892904866155l);
		var gameSet = new GameSet(UserInterface.importBoard("" //
				+ ". . X O O O . \n" //
				+ ". . X O . O . \n" //
				+ ". X X X X X O \n" //
				+ ". X X X O O . \n" //
				+ "X . O O X O O \n" //
				+ ". . O X X O O \n" //
				+ "O O O O X . O \n" //
		), Occupation.BLACK);
		testScenario(gameSet, Coordinate.c(1, 6));
	}

	@Test
	public void testLiveAndDeath2() {
		ShuffleUtil.setSeed(2683853477210701753l);
		var gameSet = new GameSet(UserInterface.importBoard("" //
				+ ". O . O X . . \n" //
				+ "O . . O X . . \n" //
				+ ". . . O X X X \n" //
				+ ". O O O X . . \n" //
				+ "O O X X X O O \n" //
				+ "X X X . O . O \n" //
				+ ". . X . O . . \n" //
		), Occupation.BLACK);
		testScenario(gameSet, Coordinate.c(6, 5));
	}

	private void testScenario(GameSet gameSet, Coordinate bestMove) {
		var visitor = UctWeiqi.newVisitor(new GameSet(gameSet));
		var search = new UctSearch<>(visitor);
		search.setNumberOfSimulations(20000);
		search.setNumberOfThreads(1);

		var move = search.search();
		search.dumpPrincipalVariation();
		search.dumpSearch();
		search.dumpRave();

		assertEquals(bestMove, move);
	}

}
