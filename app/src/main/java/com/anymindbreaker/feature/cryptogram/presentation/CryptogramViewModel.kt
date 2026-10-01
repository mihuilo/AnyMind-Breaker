package com.anymindbreaker.feature.cryptogram.presentation

import androidx.lifecycle.viewModelScope
import com.anymindbreaker.core.common.game.Difficulty
import com.anymindbreaker.core.common.game.GamePhase
import com.anymindbreaker.core.common.game.GameRepository
import com.anymindbreaker.core.common.game.GameType
import com.anymindbreaker.core.common.game.GameViewModel
import com.anymindbreaker.core.common.game.InMemoryGameRepository
import com.anymindbreaker.core.common.game.Language
import com.anymindbreaker.core.common.game.PuzzleGame
import com.anymindbreaker.core.common.game.ScoreCalculator
import com.anymindbreaker.feature.cryptogram.data.CryptogramTextSource
import com.anymindbreaker.feature.cryptogram.domain.CryptogramAction
import com.anymindbreaker.feature.cryptogram.domain.CryptogramGame
import com.anymindbreaker.feature.cryptogram.domain.CryptogramGenerator
import com.anymindbreaker.feature.cryptogram.domain.CryptogramPuzzle
import com.anymindbreaker.feature.cryptogram.domain.CryptogramState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.random.Random

data class CryptogramUiState(
    val phase: GamePhase = GamePhase.SETUP,
    val difficulty: Difficulty = Difficulty.EASY,
    val language: Language = Language.RU,
    val livesEnabled: Boolean = false,
    /** Language of the puzzle being played; the setup choice may change while it is on screen. */
    val gameLanguage: Language = Language.RU,
    val game: CryptogramState? = null,
    val score: Int = 0,
)

@Serializable
private data class CryptogramSnapshot(
    val puzzle: CryptogramPuzzle,
    val state: CryptogramState,
    val lives: Int?,
)

class CryptogramViewModel(
    private val textSource: CryptogramTextSource,
    private val scoreCalculator: ScoreCalculator,
    repository: GameRepository = InMemoryGameRepository(),
    persistenceScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    defaultLanguage: Language = Language.RU,
    private val generator: CryptogramGenerator = CryptogramGenerator(),
    private val random: Random = Random.Default,
    resumeSavedGame: Boolean = false,
) : GameViewModel<CryptogramState, CryptogramAction>(
    GameType.CRYPTOGRAM,
    CryptogramAction.Tick,
    repository,
    persistenceScope,
) {

    private val _uiState = MutableStateFlow(CryptogramUiState(language = defaultLanguage))
    val uiState: StateFlow<CryptogramUiState> = _uiState.asStateFlow()

    private var puzzle: CryptogramPuzzle? = null
    private var lives: Int? = null
    private var lastTextId: String? = null

    init {
        checkSavedGame(resumeSavedGame)
    }

    fun onDifficultySelected(difficulty: Difficulty) {
        _uiState.update { it.copy(difficulty = difficulty) }
    }

    fun onLanguageSelected(language: Language) {
        _uiState.update { it.copy(language = language) }
    }

    fun onLivesEnabledChanged(enabled: Boolean) {
        _uiState.update { it.copy(livesEnabled = enabled) }
    }

    fun onStartGame() {
        val setup = _uiState.value
        if (setup.phase == GamePhase.LOADING) return
        showLoading()
        viewModelScope.launch {
            val candidates = textSource.texts(setup.language).filter { it.difficulty == setup.difficulty }
            // Avoid giving the same phrase twice in a row when there is a choice.
            val text = candidates.filter { it.id != lastTextId }.ifEmpty { candidates }.random(random)
            lastTextId = text.id
            val newPuzzle = generator.generate(
                id = text.id,
                text = text.text,
                language = setup.language,
                difficulty = setup.difficulty,
                createdAt = System.currentTimeMillis(),
            )
            val newLives = if (setup.livesEnabled) livesFor(setup.difficulty) else null
            puzzle = newPuzzle
            lives = newLives
            _uiState.update { it.copy(gameLanguage = setup.language) }
            startNewGame(CryptogramGame(newPuzzle, newLives, scoreCalculator).also { it.start() }, newPuzzle)
        }
    }

    override fun render(phase: GamePhase, state: CryptogramState?, score: Int) {
        _uiState.update { it.copy(phase = phase, game = state, score = score) }
    }

    override fun snapshot(state: CryptogramState): String =
        Json.encodeToString(CryptogramSnapshot(checkNotNull(puzzle), state, lives))

    override fun restore(payload: String): PuzzleGame<CryptogramState, CryptogramAction>? {
        val saved = try {
            Json.decodeFromString<CryptogramSnapshot>(payload)
        } catch (_: SerializationException) {
            return null
        } catch (_: IllegalArgumentException) {
            return null
        }
        puzzle = saved.puzzle
        lives = saved.lives
        lastTextId = saved.puzzle.id
        _uiState.update {
            it.copy(
                difficulty = saved.puzzle.difficulty,
                language = saved.puzzle.language,
                gameLanguage = saved.puzzle.language,
                livesEnabled = saved.lives != null,
            )
        }
        return CryptogramGame(saved.puzzle, saved.lives, scoreCalculator, saved.state)
    }

    private fun livesFor(difficulty: Difficulty): Int = when (difficulty) {
        Difficulty.EASY -> 5
        Difficulty.NORMAL -> 4
        Difficulty.HARD, Difficulty.EXPERT -> 3
    }
}
