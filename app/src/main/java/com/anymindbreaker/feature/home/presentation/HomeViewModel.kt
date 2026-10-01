package com.anymindbreaker.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anymindbreaker.core.common.game.GameRepository
import com.anymindbreaker.core.common.game.GameType
import com.anymindbreaker.feature.statistics.domain.StatisticsCalculator
import com.anymindbreaker.feature.statistics.domain.TodaySummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

data class HomeUiState(
    /** The game played most recently among the unfinished ones, or null if there is none. */
    val continueGame: GameType? = null,
    val today: TodaySummary = TodaySummary(),
)

class HomeViewModel(
    repository: GameRepository,
    private val startOfToday: () -> Long = ::startOfTodayMillis,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> =
        combine(repository.observeSavedGames(), repository.observeFinishedSessions()) { saved, sessions ->
            HomeUiState(
                continueGame = saved.firstOrNull()?.session?.gameType,
                today = StatisticsCalculator.today(sessions, startOfToday()),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}

private fun startOfTodayMillis(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis
