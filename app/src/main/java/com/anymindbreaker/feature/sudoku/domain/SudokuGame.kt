package com.anymindbreaker.feature.sudoku.domain

import com.anymindbreaker.core.common.game.Difficulty
import com.anymindbreaker.core.common.game.GameAction
import com.anymindbreaker.core.common.game.GameResult
import com.anymindbreaker.core.common.game.GameState
import com.anymindbreaker.core.common.game.GameType
import com.anymindbreaker.core.common.game.PuzzleGame
import com.anymindbreaker.core.common.game.ScoreCalculator
import com.anymindbreaker.core.common.game.ScoreInput
import kotlinx.serialization.Serializable

enum class SudokuCheckMode {
    /** A wrong digit is reported as soon as it is entered. */
    INSTANT,

    /** Wrong digits are shown only when the player asks for a check. */
    CLASSIC,
}

@Serializable
data class SudokuState(
    val values: List<Int>,
    val given: List<Boolean>,
    val wrong: Set<Int> = emptySet(),
    val selected: Int? = null,
    val checkMode: SudokuCheckMode,
    override val elapsedSeconds: Long = 0,
    override val entries: Int = 0,
    override val mistakes: Int = 0,
    override val hintsUsed: Int = 0,
    override val livesLeft: Int? = null,
    override val result: GameResult = GameResult.IN_PROGRESS,
) : GameState

sealed interface SudokuAction : GameAction {
    data class SelectCell(val index: Int) : SudokuAction
    data class InputDigit(val digit: Int) : SudokuAction
    data object Erase : SudokuAction
    data object Check : SudokuAction
    data object Hint : SudokuAction
    data object Tick : SudokuAction
}

class SudokuGame(
    private val puzzle: SudokuPuzzle,
    private val checkMode: SudokuCheckMode,
    private val scoreCalculator: ScoreCalculator,
    initialState: SudokuState? = null,
) : PuzzleGame<SudokuState, SudokuAction> {

    override val gameType: GameType = GameType.SUDOKU

    private var state = initialState ?: newState()

    override fun start() {
        state = newState()
    }

    override fun getState(): SudokuState = state

    override fun handleAction(action: SudokuAction) {
        if (isFinished()) return
        state = when (action) {
            is SudokuAction.SelectCell ->
                if (action.index in state.values.indices) state.copy(selected = action.index) else state
            is SudokuAction.InputDigit -> input(action.digit)
            SudokuAction.Erase -> erase()
            SudokuAction.Check -> check()
            SudokuAction.Hint -> hint()
            SudokuAction.Tick -> state.copy(elapsedSeconds = state.elapsedSeconds + 1)
        }
    }

    override fun calculateScore(): Int = scoreCalculator.calculate(
        ScoreInput(
            difficulty = puzzle.difficulty,
            result = state.result,
            durationSeconds = state.elapsedSeconds,
            targetSeconds = targetSeconds(puzzle.difficulty),
            mistakes = state.mistakes,
            hintsUsed = state.hintsUsed,
            livesLeft = state.livesLeft,
        ),
    )

    private fun newState() = SudokuState(
        values = puzzle.givens,
        given = puzzle.givens.map { it != 0 },
        checkMode = checkMode,
    )

    private fun editableSelection(): Int? = state.selected?.takeIf { !state.given[it] }

    private fun input(digit: Int): SudokuState {
        val cell = editableSelection() ?: return state
        if (digit !in 1..9 || state.values[cell] == digit) return state

        val values = state.values.toMutableList().also { it[cell] = digit }
        val isWrong = checkMode == SudokuCheckMode.INSTANT && digit != puzzle.solution[cell]
        return state.copy(
            values = values,
            wrong = if (isWrong) state.wrong + cell else state.wrong - cell,
            entries = state.entries + 1,
            mistakes = state.mistakes + if (isWrong) 1 else 0,
            result = if (values == puzzle.solution) GameResult.COMPLETED else GameResult.IN_PROGRESS,
        )
    }

    private fun erase(): SudokuState {
        val cell = editableSelection() ?: return state
        if (state.values[cell] == 0) return state
        return state.copy(
            values = state.values.toMutableList().also { it[cell] = 0 },
            wrong = state.wrong - cell,
        )
    }

    private fun check(): SudokuState {
        val wrong = state.values.indices
            .filter { state.values[it] != 0 && state.values[it] != puzzle.solution[it] }
            .toSet()
        // A cell already marked wrong is not counted as a new mistake.
        return state.copy(wrong = wrong, mistakes = state.mistakes + (wrong - state.wrong).size)
    }

    /**
     * Reveals the selected cell, or the first unsolved cell when the selection is already correct.
     * A revealed cell is locked like a given one.
     */
    private fun hint(): SudokuState {
        fun unsolved(cell: Int) = !state.given[cell] && state.values[cell] != puzzle.solution[cell]
        val cell = state.selected?.takeIf(::unsolved)
            ?: state.values.indices.firstOrNull(::unsolved)
            ?: return state

        val values = state.values.toMutableList().also { it[cell] = puzzle.solution[cell] }
        return state.copy(
            values = values,
            given = state.given.toMutableList().also { it[cell] = true },
            wrong = state.wrong - cell,
            selected = cell,
            hintsUsed = state.hintsUsed + 1,
            result = if (values == puzzle.solution) GameResult.COMPLETED else GameResult.IN_PROGRESS,
        )
    }

    private fun targetSeconds(difficulty: Difficulty): Long = when (difficulty) {
        Difficulty.EASY -> 5 * 60L
        Difficulty.NORMAL -> 10 * 60L
        Difficulty.HARD -> 15 * 60L
        Difficulty.EXPERT -> 20 * 60L
    }
}
