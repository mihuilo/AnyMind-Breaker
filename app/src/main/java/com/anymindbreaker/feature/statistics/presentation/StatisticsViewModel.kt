package com.anymindbreaker.feature.statistics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anymindbreaker.core.common.game.Difficulty
import com.anymindbreaker.core.common.game.GameRepository
import com.anymindbreaker.core.common.game.GameSession
import com.anymindbreaker.core.common.game.GameType
import com.anymindbreaker.core.common.game.Language
import com.anymindbreaker.feature.statistics.domain.Achievement
import com.anymindbreaker.feature.statistics.domain.GameSummary
import com.anymindbreaker.feature.statistics.domain.Highlights
import com.anymindbreaker.feature.statistics.domain.StatisticsCalculator
import com.anymindbreaker.feature.statistics.domain.StatisticsFilter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class StatisticsUiState(
    val loading: Boolean = true,
    val filter: StatisticsFilter = StatisticsFilter(GameType.entries.first()),
    /** Headline numbers and details of the games matching [filter]. */
    val highlights: Highlights = Highlights(),
    val summary: GameSummary = GameSummary(),
    /** Finished sessions of all games, newest first. */
    val history: List<GameSession> = emptyList(),
    val achievements: Set<Achievement> = emptySet(),
)

class StatisticsViewModel(
    repository: GameRepository,
    computationDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val filter = MutableStateFlow(StatisticsFilter(GameType.entries.first()))

    val uiState: StateFlow<StatisticsUiState> =
        combine(repository.observeFinishedSessions(), filter) { sessions, filter ->
            val selected = sessions.filter(filter::matches)
            StatisticsUiState(
                loading = false,
                filter = filter,
                highlights = StatisticsCalculator.highlights(selected, now() - RECENT_PERIOD_MILLIS),
                summary = StatisticsCalculator.summarize(selected),
                history = sessions.take(HISTORY_LIMIT),
                achievements = StatisticsCalculator.achievements(sessions),
            )
        }
            // Statistics are computed off the main thread so the screen never blocks on them.
            .flowOn(computationDispatcher)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatisticsUiState())

    /** Switching the game starts again from all difficulties and languages. */
    fun onGameSelected(gameType: GameType) {
        filter.update { if (it.gameType == gameType) it else StatisticsFilter(gameType) }
    }

    fun onDifficultySelected(difficulty: Difficulty?) {
        filter.update { it.copy(difficulty = difficulty) }
    }

    fun onLanguageSelected(language: Language?) {
        filter.update { it.copy(language = language) }
    }

    private companion object {
        const val HISTORY_LIMIT = 200
        const val RECENT_PERIOD_MILLIS = 7 * 24 * 60 * 60 * 1000L
    }
}
