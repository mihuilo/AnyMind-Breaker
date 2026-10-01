package com.anymindbreaker.feature.statistics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anymindbreaker.core.common.game.GameRepository
import com.anymindbreaker.core.common.game.GameSession
import com.anymindbreaker.feature.statistics.domain.Achievement
import com.anymindbreaker.feature.statistics.domain.StatisticsCalculator
import com.anymindbreaker.feature.statistics.domain.StatisticsOverview
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

data class StatisticsUiState(
    val loading: Boolean = true,
    val overview: StatisticsOverview = StatisticsOverview(),
    /** Finished sessions, newest first. */
    val history: List<GameSession> = emptyList(),
    val achievements: Set<Achievement> = emptySet(),
)

class StatisticsViewModel(
    repository: GameRepository,
    computationDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    val uiState: StateFlow<StatisticsUiState> =
        combine(repository.observeFinishedSessions(), repository.observeStatistics()) { sessions, totals ->
            StatisticsUiState(
                loading = false,
                overview = StatisticsCalculator.overview(sessions, totals),
                history = sessions.take(HISTORY_LIMIT),
                achievements = StatisticsCalculator.achievements(sessions),
            )
        }
            // Statistics are computed off the main thread so the screen never blocks on them.
            .flowOn(computationDispatcher)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatisticsUiState())

    private companion object {
        const val HISTORY_LIMIT = 200
    }
}
