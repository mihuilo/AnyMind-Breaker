package com.anymindbreaker.feature.cryptogram.presentation

import androidx.lifecycle.viewModelScope
import com.anymindbreaker.core.common.game.Difficulty
import com.anymindbreaker.core.common.game.GamePhase
import com.anymindbreaker.core.common.game.GameViewModel
import com.anymindbreaker.core.common.game.Language
import com.anymindbreaker.core.common.game.ScoreCalculator
import com.anymindbreaker.feature.cryptogram.data.CryptogramTextSource
import com.anymindbreaker.feature.cryptogram.domain.CryptogramAction
import com.anymindbreaker.feature.cryptogram.domain.CryptogramGame
import com.anymindbreaker.feature.cryptogram.domain.CryptogramGenerator
import com.anymindbreaker.feature.cryptogram.domain.CryptogramState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

class CryptogramViewModel(
    private val textSource: CryptogramTextSource,
    private val scoreCalculator: ScoreCalculator,
    defaultLanguage: Language = Language.RU,
    private val generator: CryptogramGenerator = CryptogramGenerator(),
    private val random: Random = Random.Default,
) : GameViewModel<CryptogramState, CryptogramAction>(CryptogramAction.Tick) {

    private val _uiState = MutableStateFlow(CryptogramUiState(language = defaultLanguage))
    val uiState: StateFlow<CryptogramUiState> = _uiState.asStateFlow()

    private var lastTextId: String? = null

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
            val puzzle = generator.generate(
                id = text.id,
                text = text.text,
                language = setup.language,
                difficulty = setup.difficulty,
                createdAt = System.currentTimeMillis(),
            )
            _uiState.update { it.copy(gameLanguage = setup.language) }
            val lives = if (setup.livesEnabled) livesFor(setup.difficulty) else null
            attach(CryptogramGame(puzzle, lives, scoreCalculator).also { it.start() })
        }
    }

    override fun render(phase: GamePhase, state: CryptogramState?, score: Int) {
        _uiState.update { it.copy(phase = phase, game = state, score = score) }
    }

    private fun livesFor(difficulty: Difficulty): Int = when (difficulty) {
        Difficulty.EASY -> 5
        Difficulty.NORMAL -> 4
        Difficulty.HARD, Difficulty.EXPERT -> 3
    }
}
