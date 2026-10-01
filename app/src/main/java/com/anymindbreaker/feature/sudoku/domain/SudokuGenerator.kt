package com.anymindbreaker.feature.sudoku.domain

import com.anymindbreaker.core.common.game.Difficulty
import com.anymindbreaker.core.common.game.GameType
import com.anymindbreaker.core.common.game.Language
import com.anymindbreaker.core.common.game.Puzzle
import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.random.Random

@Serializable
data class SudokuPuzzle(
    override val id: String,
    override val difficulty: Difficulty,
    /** 81 cells row by row, 0 for an empty cell. */
    val givens: List<Int>,
    val solution: List<Int>,
    override val createdAt: Long = 0,
    override val metadata: Map<String, String> = emptyMap(),
) : Puzzle {
    override val gameType: GameType get() = GameType.SUDOKU
    override val language: Language? get() = null
}

/**
 * Builds a filled grid and removes cells one at a time, keeping a removal only while the puzzle
 * still has a single solution and stays within the reasoning level of the requested difficulty.
 */
class SudokuGenerator(private val random: Random = Random.Default) {

    private class Spec(
        val minClues: Int,
        val target: SudokuTechniqueLevel,
    )

    private class Candidate(
        val givens: IntArray,
        val solution: IntArray,
        val level: SudokuTechniqueLevel,
    )

    fun generate(difficulty: Difficulty, createdAt: Long = 0): SudokuPuzzle {
        val spec = specOf(difficulty)
        var best: Candidate? = null
        for (attempt in 0 until MAX_ATTEMPTS) {
            val candidate = dig(spec)
            if (best == null || distance(candidate, spec) < distance(best, spec)) best = candidate
            if (candidate.level == spec.target) break
        }
        val result = checkNotNull(best)
        val givens = result.givens.toList()
        return SudokuPuzzle(
            id = "sudoku_" + givens.joinToString(""),
            difficulty = difficulty,
            givens = givens,
            solution = result.solution.toList(),
            createdAt = createdAt,
            metadata = mapOf(
                "clues" to givens.count { it != 0 }.toString(),
                "technique" to result.level.name,
            ),
        )
    }

    private fun dig(spec: Spec): Candidate {
        val solution = SudokuSolver.randomSolution(random)
        val grid = solution.copyOf()
        var clues = SudokuUnits.CELLS
        for (cell in (0 until SudokuUnits.CELLS).shuffled(random)) {
            if (clues <= spec.minClues) break
            val digit = grid[cell]
            grid[cell] = 0
            val keep = SudokuSolver.countSolutions(grid) == 1 && SudokuRater.rate(grid) <= spec.target
            if (keep) clues-- else grid[cell] = digit
        }
        return Candidate(grid, solution, SudokuRater.rate(grid))
    }

    private fun distance(candidate: Candidate, spec: Spec) = abs(candidate.level.ordinal - spec.target.ordinal)

    private fun specOf(difficulty: Difficulty) = when (difficulty) {
        Difficulty.EASY -> Spec(minClues = 38, target = SudokuTechniqueLevel.SINGLES)
        Difficulty.NORMAL -> Spec(minClues = 30, target = SudokuTechniqueLevel.SINGLES)
        Difficulty.HARD -> Spec(minClues = 26, target = SudokuTechniqueLevel.INTERMEDIATE)
        Difficulty.EXPERT -> Spec(minClues = 22, target = SudokuTechniqueLevel.ADVANCED)
    }

    private companion object {
        const val MAX_ATTEMPTS = 40
    }
}
