package com.anymindbreaker.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anymindbreaker.core.common.game.GameRepository
import com.anymindbreaker.core.datastore.AppSettings
import com.anymindbreaker.core.datastore.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val gameRepository: GameRepository,
) : ViewModel() {

    /** Null until the stored settings are loaded. */
    val settings: StateFlow<AppSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }

    /** Deletes history, statistics and unfinished games. Settings are kept. */
    fun resetProgress() {
        viewModelScope.launch { gameRepository.resetProgress() }
    }
}
