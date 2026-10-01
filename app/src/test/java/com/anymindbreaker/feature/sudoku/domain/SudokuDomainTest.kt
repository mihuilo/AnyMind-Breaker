package com.anymindbreaker.feature.sudoku.domain

import com.anymindbreaker.core.common.game.DefaultScoreCalculator
import com.anymindbreaker.core.common.game.Difficulty
import com.anymindbreaker.core.common.game.GameResult
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

private fun grid(text: String): IntArray = text.filter { it.isDigit() || it == '.' }
    .map { if (it == '.') 0 else it.digitToInt() }
    .toIntArray()

private val KNOWN_PUZZLE = grid(
    """
    53..7....
    6..195...
    .98....6.
    8...6...3
    4..8.3..1
    7...2...6
    .6....28.
    ...419..5
    ....8..79
    """,
)

private val KNOWN_SOLUTION = grid(
    """
    534678912
    672195348
    198342567
    859761423
    426853791
    713924856
    961537284
    287419635
    345286179
    """,
)

class SudokuSolverTest {

    @Test
    fun solvesKnownPuzzle() {
        assertArrayEquals(KNOWN_SOLUTION, SudokuSolver.solve(KNOWN_PUZZLE))
    }

    @Test
    fun knownPuzzleHasExactlyOneSolution() {
        assertEquals(1, SudokuSolver.countSolutions(KNOWN_PUZZLE))
    }

    @Test
    fun emptyGridHasManySolutions() {
        assertEquals(2, SudokuSolver.countSolutions(IntArray(81), limit = 2))
    }

    @Test
    fun removingTooManyCluesIsDetectedAsNonUnique() {
        val sparse = KNOWN_PUZZLE.copyOf().also { for (i in 0 until 27) it[i] = 0 }
        assertEquals(2, SudokuSolver.countSolutions(sparse, limit = 2))
    }

    @Test
    fun contradictoryGridHasNoSolutions() {
        val broken = KNOWN_PUZZLE.copyOf().also { it[2] = 5 }
        assertEquals(0, SudokuSolver.countSolutions(broken))
        assertNull(SudokuSolver.solve(broken))
    }

    @Test
    fun validatesSolutions() {
        assertTrue(SudokuSolver.isValidSolution(KNOWN_SOLUTION))
        assertFalse(SudokuSolver.isValidSolution(KNOWN_PUZZLE))
        val swapped = KNOWN_SOLUTION.copyOf().also { it[0] = it[1].also { _ -> it[1] = it[0] } }
        assertFalse(SudokuSolver.isValidSolution(swapped))
    }

    @Test
    fun randomSolutionIsValid() {
        repeat(20) { seed ->
            assertTrue(SudokuSolver.isValidSolution(SudokuSolver.randomSolution(Random(seed))))
        }
    }
}

class SudokuRaterTest {

    @Test
    fun knownEasyPuzzleNeedsOnlySingles() {
        assertEquals(SudokuTechniqueLevel.SINGLES, SudokuRater.rate(KNOWN_PUZZLE))
    }

    @Test
    fun solvedGridIsRatedAsSingles() {
        assertEquals(SudokuTechniqueLevel.SINGLES, SudokuRater.rate(KNOWN_SOLUTION))
    }

    @Test
    fun emptyGridCannotBeSolvedLogically() {
        assertEquals(SudokuTechniqueLevel.ADVANCED, SudokuRater.rate(IntArray(81)))
    }
}

class SudokuGeneratorTest {

    private fun generate(difficulty: Difficulty, seed: Int) = SudokuGenerator(Random(seed)).generate(difficulty)

    @Test
    fun everyGeneratedPuzzleHasValidUniqueSolution() {
        for (difficulty in Difficulty.entries) {
            repeat(5) { seed ->
                val puzzle = generate(difficulty, seed)
                val givens = puzzle.givens.toIntArray()
                val solution = puzzle.solution.toIntArray()

                assertTrue("solution valid", SudokuSolver.isValidSolution(solution))
                assertEquals("unique solution", 1, SudokuSolver.countSolutions(givens))
                assertArrayEquals("stored solution is the solution", solution, SudokuSolver.solve(givens))
                assertTrue(
                    "givens match solution",
                    givens.indices.all { givens[it] == 0 || givens[it] == solution[it] },
                )
            }
        }
    }

    @Test
    fun easyAndNormalNeedOnlySingles() {
        repeat(5) { seed ->
            assertEquals("SINGLES", generate(Difficulty.EASY, seed).metadata["technique"])
            assertEquals("SINGLES", generate(Difficulty.NORMAL, seed).metadata["technique"])
        }
    }

    @Test
    fun harderLevelsNeedHarderTechniques() {
        repeat(5) { seed ->
            assertEquals("INTERMEDIATE", generate(Difficulty.HARD, seed).metadata["technique"])
            assertEquals("ADVANCED", generate(Difficulty.EXPERT, seed).metadata["technique"])
        }
    }

    @Test
    fun easyHasMoreCluesThanNormal() {
        repeat(5) { seed ->
            val easy = generate(Difficulty.EASY, seed).givens.count { it != 0 }
            val normal = generate(Difficulty.NORMAL, seed).givens.count { it != 0 }
            assertTrue(easy >= 38)
            assertTrue(normal in 30 until easy)
        }
    }

    @Test
    fun sameSeedGivesSamePuzzle() {
        assertEquals(generate(Difficulty.NORMAL, 7), generate(Difficulty.NORMAL, 7))
    }
}

class SudokuGameTest {

    private val puzzle = SudokuPuzzle(
        id = "test",
        difficulty = Difficulty.EASY,
        givens = KNOWN_PUZZLE.toList(),
        solution = KNOWN_SOLUTION.toList(),
    )

    private fun game(mode: SudokuCheckMode = SudokuCheckMode.INSTANT) =
        SudokuGame(puzzle, mode, DefaultScoreCalculator()).also { it.start() }

    private fun SudokuGame.enter(cell: Int, digit: Int) {
        handleAction(SudokuAction.SelectCell(cell))
        handleAction(SudokuAction.InputDigit(digit))
    }

    private fun SudokuGame.solveAllExcept(skip: Int? = null) {
        for (cell in KNOWN_PUZZLE.indices) {
            if (KNOWN_PUZZLE[cell] == 0 && cell != skip) enter(cell, KNOWN_SOLUTION[cell])
        }
    }

    @Test
    fun correctDigitIsPlacedWithoutMistake() {
        val game = game()
        game.enter(2, 4)
        assertEquals(4, game.getState().values[2])
        assertEquals(0, game.getState().mistakes)
        assertTrue(game.getState().wrong.isEmpty())
    }

    @Test
    fun wrongDigitIsReportedImmediatelyInInstantMode() {
        val game = game()
        game.enter(2, 1)
        assertEquals(1, game.getState().mistakes)
        assertEquals(setOf(2), game.getState().wrong)

        game.enter(2, 4)
        assertEquals(1, game.getState().mistakes)
        assertTrue(game.getState().wrong.isEmpty())
    }

    @Test
    fun repeatingTheSameDigitIsNotCountedTwice() {
        val game = game()
        game.enter(2, 1)
        game.enter(2, 1)
        assertEquals(1, game.getState().mistakes)
        assertEquals(1, game.getState().entries)
    }

    @Test
    fun givenCellsCannotBeChanged() {
        val game = game()
        game.enter(0, 9)
        game.handleAction(SudokuAction.Erase)
        assertEquals(5, game.getState().values[0])
        assertEquals(0, game.getState().mistakes)
    }

    @Test
    fun eraseClearsCellAndErrorMark() {
        val game = game()
        game.enter(2, 1)
        game.handleAction(SudokuAction.Erase)
        assertEquals(0, game.getState().values[2])
        assertTrue(game.getState().wrong.isEmpty())
        assertEquals(1, game.getState().mistakes)
    }

    @Test
    fun classicModeHidesMistakesUntilCheck() {
        val game = game(SudokuCheckMode.CLASSIC)
        game.enter(2, 1)
        game.enter(3, 6)
        assertEquals(0, game.getState().mistakes)
        assertTrue(game.getState().wrong.isEmpty())

        game.handleAction(SudokuAction.Check)
        assertEquals(setOf(2), game.getState().wrong)
        assertEquals(1, game.getState().mistakes)

        game.handleAction(SudokuAction.Check)
        assertEquals(1, game.getState().mistakes)
    }

    @Test
    fun fillingTheGridCorrectlyCompletesTheGame() {
        val game = game()
        game.solveAllExcept(skip = 2)
        assertFalse(game.isFinished())

        game.enter(2, 4)
        assertTrue(game.isFinished())
        assertEquals(GameResult.COMPLETED, game.getState().result)
        assertTrue(game.calculateScore() > 0)
    }

    @Test
    fun fullGridWithWrongDigitIsNotCompleted() {
        val game = game(SudokuCheckMode.CLASSIC)
        game.solveAllExcept(skip = 2)
        game.enter(2, 1)
        assertFalse(game.isFinished())
        assertEquals(0, game.calculateScore())
    }

    @Test
    fun finishedGameIgnoresFurtherActions() {
        val game = game()
        game.solveAllExcept()
        val finished = game.getState()
        game.handleAction(SudokuAction.Tick)
        game.handleAction(SudokuAction.Erase)
        assertEquals(finished, game.getState())
    }

    @Test
    fun tickAdvancesTimerAndStartResetsGame() {
        val game = game()
        repeat(3) { game.handleAction(SudokuAction.Tick) }
        game.enter(2, 1)
        assertEquals(3, game.getState().elapsedSeconds)

        game.start()
        assertEquals(0, game.getState().elapsedSeconds)
        assertEquals(0, game.getState().mistakes)
        assertEquals(KNOWN_PUZZLE.toList(), game.getState().values)
    }
}
