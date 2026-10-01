package com.anymindbreaker.feature.sudoku.presentation

import androidx.lifecycle.viewModelScope
import com.anymindbreaker.core.common.game.DefaultScoreCalculator
import com.anymindbreaker.core.common.game.Difficulty
import com.anymindbreaker.core.common.game.GamePhase
import com.anymindbreaker.core.common.game.GameViewModel
import com.anymindbreaker.core.common.game.ScoreCalculator
import com.anymindbreaker.feature.sudoku.domain.SudokuAction
import com.anymindbreaker.feature.sudoku.domain.SudokuCheckMode
import com.anymindbreaker.feature.sudoku.domain.SudokuGame
import com.anymindbreaker.feature.sudoku.domain.SudokuGenerator
import com.anymindbreaker.feature.sudoku.domain.SudokuState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SudokuUiState(
    val phase: GamePhase = GamePhase.SETUP,
    val difficulty: Difficulty = Difficulty.EASY,
    val instantCheck: Boolean = true,
    val game: SudokuState? = null,
    val score: Int = 0,
)

class SudokuViewModel(
    private val scoreCalculator: ScoreCalculator = DefaultScoreCalculator(),
    private val generator: SudokuGenerator = SudokuGenerator(),
    private val generationDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : GameViewModel<SudokuState, SudokuAction>(SudokuAction.Tick) {

    private val _uiState = MutableStateFlow(SudokuUiState())
    val uiState: StateFlow<SudokuUiState> = _uiState.asStateFlow()

    fun onDifficultySelected(difficulty: Difficulty) {
        _uiState.update { it.copy(difficulty = difficulty) }
    }

    fun onInstantCheckChanged(enabled: Boolean) {
        _uiState.update { it.copy(instantCheck = enabled) }
    }

    fun onStartGame() {
        val setup = _uiState.value
        if (setup.phase == GamePhase.LOADING) return
        showLoading()
        viewModelScope.launch {
            // Generation is the only heavy step, so it is kept off the main thread.
            val puzzle = withContext(generationDispatcher) {
                generator.generate(setup.difficulty, createdAt = System.currentTimeMillis())
            }
            val mode = if (setup.instantCheck) SudokuCheckMode.INSTANT else SudokuCheckMode.CLASSIC
            attach(SudokuGame(puzzle, mode, scoreCalculator).also { it.start() })
        }
    }

    override fun render(phase: GamePhase, state: SudokuState?, score: Int) {
        _uiState.update { it.copy(phase = phase, game = state, score = score) }
    }
}
