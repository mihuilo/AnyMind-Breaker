package com.anymindbreaker.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anymindbreaker.core.common.game.GameRepository
import com.anymindbreaker.core.common.game.GameType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    /** The game played most recently among the unfinished ones, or null if there is none. */
    val continueGame: GameType? = null,
)

class HomeViewModel(repository: GameRepository) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = repository.observeSavedGames()
        .map { saved -> HomeUiState(continueGame = saved.firstOrNull()?.session?.gameType) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
