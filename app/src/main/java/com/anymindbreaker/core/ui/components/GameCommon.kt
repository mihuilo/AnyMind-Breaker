package com.anymindbreaker.core.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.anymindbreaker.R
import com.anymindbreaker.core.common.formatDuration
import com.anymindbreaker.core.common.game.Difficulty
import com.anymindbreaker.core.common.game.GameResult
import com.anymindbreaker.core.common.game.Language
import com.anymindbreaker.core.ui.theme.Spacing

@StringRes
fun Difficulty.labelRes(): Int = when (this) {
    Difficulty.EASY -> R.string.difficulty_easy
    Difficulty.NORMAL -> R.string.difficulty_normal
    Difficulty.HARD -> R.string.difficulty_hard
    Difficulty.EXPERT -> R.string.difficulty_expert
}

@StringRes
fun Language.labelRes(): Int = when (this) {
    Language.RU -> R.string.language_ru
    Language.EN -> R.string.language_en
}

/** Screen frame of a game: a title with a back button. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
        content = content,
    )
}

/** Asks whether to continue the unfinished game found when a game screen is opened. */
@Composable
fun ResumeGameDialog(
    onResume: () -> Unit,
    onStartNew: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onStartNew,
        title = { Text(stringResource(R.string.resume_title)) },
        text = { Text(stringResource(R.string.resume_text)) },
        confirmButton = {
            TextButton(onClick = onResume, modifier = Modifier.testTag("resume_continue")) {
                Text(stringResource(R.string.action_continue))
            }
        },
        dismissButton = {
            TextButton(onClick = onStartNew) {
                Text(stringResource(R.string.action_start_new))
            }
        },
    )
}

@Composable
fun LoadingContent(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

/** Scrollable column of setup options that ends with the start button. */
@Composable
fun GameSetupColumn(
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
    startTestTag: String = "game_start",
    options: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        options()
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(startTestTag),
        ) {
            Text(stringResource(R.string.action_start_game))
        }
        Spacer(Modifier.height(Spacing.md))
    }
}

@Composable
fun <T> OptionGroup(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
            Column(modifier = Modifier.selectableGroup()) {
                for (option in options) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = option == selected,
                                onClick = { onSelect(option) },
                                role = Role.RadioButton,
                            )
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = option == selected, onClick = null)
                        Text(text = label(option), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
fun DifficultyGroup(
    selected: Difficulty,
    onSelect: (Difficulty) -> Unit,
    modifier: Modifier = Modifier,
) {
    OptionGroup(
        title = stringResource(R.string.difficulty_title),
        options = Difficulty.entries,
        selected = selected,
        label = { stringResource(it.labelRes()) },
        onSelect = onSelect,
        modifier = modifier,
    )
}

@Composable
fun SwitchCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Switch)
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}

/** Timer on its own line, with difficulty on the left and mistakes and lives on the right below it. */
@Composable
fun GameStatusHeader(
    elapsedSeconds: Long,
    difficulty: Difficulty,
    mistakes: Int,
    livesLeft: Int?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = formatDuration(elapsedSeconds),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .testTag("game_timer"),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(difficulty.labelRes()),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (livesLeft != null) {
                    Text(
                        text = stringResource(R.string.game_lives_count, livesLeft),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Text(
                    text = stringResource(R.string.game_mistakes_count, mistakes),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (mistakes > 0) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

/** Share of correct entries in percent, or null when nothing was entered. */
fun accuracyPercent(entries: Int, mistakes: Int): Int? =
    if (entries > 0) (entries - mistakes).coerceAtLeast(0) * 100 / entries else null

@Composable
fun GameResultContent(
    result: GameResult,
    elapsedSeconds: Long,
    score: Int,
    mistakes: Int,
    hintsUsed: Int,
    accuracyPercent: Int?,
    onPlayAgain: () -> Unit,
    onChangeSettings: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "game_result",
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Spacing.xl))
        Text(
            text = stringResource(
                if (result == GameResult.COMPLETED) R.string.result_done else R.string.result_failed,
            ),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.testTag(testTag),
        )
        Spacer(Modifier.height(Spacing.lg))
        Text(text = formatDuration(elapsedSeconds), style = MaterialTheme.typography.displaySmall)
        Text(
            text = stringResource(R.string.result_score, score),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(Spacing.lg))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
            Column(
                modifier = Modifier.padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                LabeledValue(stringResource(R.string.result_mistakes), mistakes.toString())
                LabeledValue(stringResource(R.string.result_hints), hintsUsed.toString())
                LabeledValue(
                    label = stringResource(R.string.result_accuracy),
                    value = if (accuracyPercent != null) {
                        stringResource(R.string.percent_value, accuracyPercent)
                    } else {
                        stringResource(R.string.value_empty)
                    },
                )
            }
        }
        Spacer(Modifier.height(Spacing.lg))
        Button(onClick = onPlayAgain, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_play_again))
        }
        Spacer(Modifier.height(Spacing.xs))
        OutlinedButton(onClick = onChangeSettings, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_change_settings))
        }
        Spacer(Modifier.height(Spacing.md))
    }
}

@Composable
fun LabeledValue(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}
