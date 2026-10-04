package com.anymindbreaker.feature.sudoku.presentation

import androidx.lifecycle.viewModelScope
import com.anymindbreaker.core.common.game.DEFAULT_LIVES
import com.anymindbreaker.core.common.game.DefaultScoreCalculator
import com.anymindbreaker.core.common.game.Difficulty
import com.anymindbreaker.core.common.game.GamePhase
import com.anymindbreaker.core.common.game.GameRepository
import com.anymindbreaker.core.common.game.GameType
import com.anymindbreaker.core.common.game.GameViewModel
import com.anymindbreaker.core.common.game.InMemoryGameRepository
import com.anymindbreaker.core.common.game.PuzzleGame
import com.anymindbreaker.core.common.game.ScoreCalculator
import com.anymindbreaker.core.datastore.InMemorySettingsRepository
import com.anymindbreaker.core.datastore.SettingsRepository
import com.anymindbreaker.feature.sudoku.domain.SudokuAction
import com.anymindbreaker.feature.sudoku.domain.SudokuCheckMode
import com.anymindbreaker.feature.sudoku.domain.SudokuGame
import com.anymindbreaker.feature.sudoku.domain.SudokuGenerator
import com.anymindbreaker.feature.sudoku.domain.SudokuPuzzle
import com.anymindbreaker.feature.sudoku.domain.SudokuState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

data class SudokuUiState(
    val phase: GamePhase = GamePhase.SETUP,
    val difficulty: Difficulty = Difficulty.EASY,
    val instantCheck: Boolean = true,
    val livesEnabled: Boolean = true,
    val game: SudokuState? = null,
    val score: Int = 0,
)

@Serializable
private data class SudokuSnapshot(
    val puzzle: SudokuPuzzle,
    val state: SudokuState,
)

class SudokuViewModel(
    repository: GameRepository = InMemoryGameRepository(),
    persistenceScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val scoreCalculator: ScoreCalculator = DefaultScoreCalculator(),
    private val settings: SettingsRepository = InMemorySettingsRepository(),
    private val generator: SudokuGenerator = SudokuGenerator(),
    private val generationDispatcher: CoroutineDispatcher = Dispatchers.Default,
    resumeSavedGame: Boolean = false,
) : GameViewModel<SudokuState, SudokuAction>(GameType.SUDOKU, SudokuAction.Tick, repository, persistenceScope) {

    private val _uiState = MutableStateFlow(SudokuUiState())
    val uiState: StateFlow<SudokuUiState> = _uiState.asStateFlow()

    private var puzzle: SudokuPuzzle? = null

    init {
        viewModelScope.launch {
            val instantCheck = settings.settings.first().instantSudokuValidation
            _uiState.update { if (it.phase == GamePhase.SETUP) it.copy(instantCheck = instantCheck) else it }
        }
        checkSavedGame(resumeSavedGame)
    }

    fun onDifficultySelected(difficulty: Difficulty) {
        _uiState.update { it.copy(difficulty = difficulty) }
    }

    fun onInstantCheckChanged(enabled: Boolean) {
        _uiState.update { it.copy(instantCheck = enabled) }
        viewModelScope.launch { settings.update { it.copy(instantSudokuValidation = enabled) } }
    }

    fun onLivesEnabledChanged(enabled: Boolean) {
        _uiState.update { it.copy(livesEnabled = enabled) }
    }

    fun onStartGame() {
        val setup = _uiState.value
        if (setup.phase == GamePhase.LOADING) return
        showLoading()
        viewModelScope.launch {
            // Generation is the only heavy step, so it is kept off the main thread.
            val newPuzzle = withContext(generationDispatcher) {
                generator.generate(setup.difficulty, createdAt = System.currentTimeMillis())
            }
            val mode = if (setup.instantCheck) SudokuCheckMode.INSTANT else SudokuCheckMode.CLASSIC
            puzzle = newPuzzle
            val lives = if (setup.livesEnabled) DEFAULT_LIVES else null
            startNewGame(SudokuGame(newPuzzle, mode, lives, scoreCalculator).also { it.start() }, newPuzzle)
        }
    }

    override fun render(phase: GamePhase, state: SudokuState?, score: Int) {
        _uiState.update { it.copy(phase = phase, game = state, score = score) }
    }

    override fun snapshot(state: SudokuState): String =
        Json.encodeToString(SudokuSnapshot(checkNotNull(puzzle), state))

    override fun restore(payload: String): PuzzleGame<SudokuState, SudokuAction>? {
        val saved = try {
            Json.decodeFromString<SudokuSnapshot>(payload)
        } catch (_: SerializationException) {
            return null
        } catch (_: IllegalArgumentException) {
            return null
        }
        puzzle = saved.puzzle
        _uiState.update {
            it.copy(
                difficulty = saved.puzzle.difficulty,
                instantCheck = saved.state.checkMode == SudokuCheckMode.INSTANT,
                livesEnabled = saved.state.livesLeft != null,
            )
        }
        return SudokuGame(saved.puzzle, saved.state.checkMode, saved.state.livesLeft, scoreCalculator, saved.state)
    }
}
