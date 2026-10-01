package com.anymindbreaker.feature.settings.presentation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anymindbreaker.R
import com.anymindbreaker.app.appContainer
import com.anymindbreaker.core.common.game.Language
import com.anymindbreaker.core.datastore.ThemeMode
import com.anymindbreaker.core.ui.components.LoadingContent
import com.anymindbreaker.core.ui.components.OptionGroup
import com.anymindbreaker.core.ui.components.SwitchCard
import com.anymindbreaker.core.ui.components.labelRes
import com.anymindbreaker.core.ui.theme.Spacing

@StringRes
private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = run {
        val container = appContainer()
        viewModel { SettingsViewModel(container.settings, container.gameRepository) }
    },
) {
    val settings = viewModel.settings.collectAsStateWithLifecycle().value
    var confirmReset by rememberSaveable { mutableStateOf(false) }

    if (settings == null) {
        LoadingContent(modifier = modifier.fillMaxSize())
        return
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.settings_reset_title)) },
            text = { Text(stringResource(R.string.settings_reset_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetProgress()
                        confirmReset = false
                    },
                    modifier = Modifier.testTag("settings_reset_confirm"),
                ) {
                    Text(
                        text = stringResource(R.string.settings_reset_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.md, vertical = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        OptionGroup(
            title = stringResource(R.string.settings_ui_language),
            options = Language.entries,
            selected = settings.uiLanguage,
            label = { stringResource(it.labelRes()) },
            onSelect = { language -> viewModel.update { it.copy(uiLanguage = language) } },
            modifier = Modifier.testTag("settings_ui_language"),
        )
        OptionGroup(
            title = stringResource(R.string.puzzle_language_title),
            options = Language.entries,
            selected = settings.contentLanguage,
            label = { stringResource(it.labelRes()) },
            onSelect = { language -> viewModel.update { it.copy(contentLanguage = language) } },
            modifier = Modifier.testTag("settings_content_language"),
        )
        OptionGroup(
            title = stringResource(R.string.settings_theme),
            options = ThemeMode.entries,
            selected = settings.theme,
            label = { stringResource(it.labelRes()) },
            onSelect = { theme -> viewModel.update { it.copy(theme = theme) } },
        )
        SwitchCard(
            title = stringResource(R.string.settings_sound),
            description = stringResource(R.string.settings_sound_description),
            checked = settings.soundEnabled,
            onCheckedChange = { enabled -> viewModel.update { it.copy(soundEnabled = enabled) } },
            modifier = Modifier.testTag("settings_sound"),
        )
        SwitchCard(
            title = stringResource(R.string.settings_vibration),
            description = stringResource(R.string.settings_vibration_description),
            checked = settings.vibrationEnabled,
            onCheckedChange = { enabled -> viewModel.update { it.copy(vibrationEnabled = enabled) } },
        )
        SwitchCard(
            title = stringResource(R.string.sudoku_instant_check),
            description = stringResource(R.string.sudoku_instant_check_description),
            checked = settings.instantSudokuValidation,
            onCheckedChange = { enabled -> viewModel.update { it.copy(instantSudokuValidation = enabled) } },
        )
        OutlinedButton(
            onClick = { confirmReset = true },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_reset"),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Text(stringResource(R.string.settings_reset))
        }
    }
}
