package com.anymindbreaker.feature.sudoku.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anymindbreaker.core.common.game.DefaultScoreCalculator
import com.anymindbreaker.core.common.game.Difficulty
import com.anymindbreaker.core.common.game.ScoreCalculator
import com.anymindbreaker.feature.sudoku.domain.SudokuAction
import com.anymindbreaker.feature.sudoku.domain.SudokuCheckMode
import com.anymindbreaker.feature.sudoku.domain.SudokuGame
import com.anymindbreaker.feature.sudoku.domain.SudokuGenerator
import com.anymindbreaker.feature.sudoku.domain.SudokuState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SudokuPhase {
    SETUP,
    LOADING,
    PLAYING,
    FINISHED,
}

data class SudokuUiState(
    val phase: SudokuPhase = SudokuPhase.SETUP,
    val difficulty: Difficulty = Difficulty.EASY,
    val instantCheck: Boolean = true,
    val game: SudokuState? = null,
    val score: Int = 0,
)

class SudokuViewModel(
    private val generator: SudokuGenerator = SudokuGenerator(),
    private val scoreCalculator: ScoreCalculator = DefaultScoreCalculator(),
    private val generationDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SudokuUiState())
    val uiState: StateFlow<SudokuUiState> = _uiState.asStateFlow()

    private var game: SudokuGame? = null
    private var timerJob: Job? = null
    private var screenVisible = false

    fun onDifficultySelected(difficulty: Difficulty) {
        _uiState.update { it.copy(difficulty = difficulty) }
    }

    fun onInstantCheckChanged(enabled: Boolean) {
        _uiState.update { it.copy(instantCheck = enabled) }
    }

    fun onStartGame() {
        val setup = _uiState.value
        if (setup.phase == SudokuPhase.LOADING) return
        _uiState.update { it.copy(phase = SudokuPhase.LOADING, game = null, score = 0) }
        viewModelScope.launch {
            val puzzle = withContext(generationDispatcher) {
                generator.generate(setup.difficulty, createdAt = System.currentTimeMillis())
            }
            val mode = if (setup.instantCheck) SudokuCheckMode.INSTANT else SudokuCheckMode.CLASSIC
            game = SudokuGame(puzzle, mode, scoreCalculator).also { it.start() }
            publish()
            updateTimer()
        }
    }

    fun onAction(action: SudokuAction) {
        val current = game ?: return
        current.handleAction(action)
        publish()
        if (current.isFinished()) updateTimer()
    }

    fun onNewGame() {
        game = null
        updateTimer()
        _uiState.update { it.copy(phase = SudokuPhase.SETUP, game = null, score = 0) }
    }

    /** The timer only runs while the game screen is on screen. */
    fun onScreenVisibilityChanged(visible: Boolean) {
        screenVisible = visible
        updateTimer()
    }

    private fun publish() {
        val current = game ?: return
        _uiState.update {
            it.copy(
                phase = if (current.isFinished()) SudokuPhase.FINISHED else SudokuPhase.PLAYING,
                game = current.getState(),
                score = current.calculateScore(),
            )
        }
    }

    private fun updateTimer() {
        val shouldRun = screenVisible && game?.isFinished() == false
        if (shouldRun && timerJob?.isActive != true) {
            timerJob = viewModelScope.launch {
                while (isActive) {
                    delay(1000)
                    onAction(SudokuAction.Tick)
                }
            }
        } else if (!shouldRun) {
            timerJob?.cancel()
            timerJob = null
        }
    }
}
