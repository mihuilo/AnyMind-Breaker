package com.anymindbreaker.feature.sudoku.presentation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anymindbreaker.R
import com.anymindbreaker.core.common.formatDuration
import com.anymindbreaker.core.common.game.Difficulty
import com.anymindbreaker.core.ui.components.GameActionButton
import com.anymindbreaker.core.ui.theme.Spacing
import com.anymindbreaker.feature.sudoku.domain.SudokuAction
import com.anymindbreaker.feature.sudoku.domain.SudokuCheckMode
import com.anymindbreaker.feature.sudoku.domain.SudokuState

@StringRes
private fun Difficulty.labelRes(): Int = when (this) {
    Difficulty.EASY -> R.string.difficulty_easy
    Difficulty.NORMAL -> R.string.difficulty_normal
    Difficulty.HARD -> R.string.difficulty_hard
    Difficulty.EXPERT -> R.string.difficulty_expert
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SudokuScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SudokuViewModel = viewModel { SudokuViewModel() },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(viewModel) {
        viewModel.onScreenVisibilityChanged(true)
        onPauseOrDispose { viewModel.onScreenVisibilityChanged(false) }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.game_sudoku)) },
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
    ) { padding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = Spacing.md)
        val game = uiState.game
        when {
            uiState.phase == SudokuPhase.SETUP -> SudokuSetup(
                difficulty = uiState.difficulty,
                instantCheck = uiState.instantCheck,
                onDifficultySelected = viewModel::onDifficultySelected,
                onInstantCheckChanged = viewModel::onInstantCheckChanged,
                onStart = viewModel::onStartGame,
                modifier = contentModifier,
            )
            uiState.phase == SudokuPhase.FINISHED && game != null -> SudokuResult(
                state = game,
                score = uiState.score,
                onPlayAgain = viewModel::onStartGame,
                onChangeSettings = viewModel::onNewGame,
                modifier = contentModifier,
            )
            uiState.phase == SudokuPhase.PLAYING && game != null -> SudokuPlay(
                state = game,
                difficulty = uiState.difficulty,
                onAction = viewModel::onAction,
                // Narrower side margins than the other phases leave more room for the grid.
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = Spacing.xs),
            )
            else -> Box(modifier = contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun SudokuSetup(
    difficulty: Difficulty,
    instantCheck: Boolean,
    onDifficultySelected: (Difficulty) -> Unit,
    onInstantCheckChanged: (Boolean) -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(
            text = stringResource(R.string.difficulty_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
            Column(modifier = Modifier.selectableGroup()) {
                for (option in Difficulty.entries) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = option == difficulty,
                                onClick = { onDifficultySelected(option) },
                                role = Role.RadioButton,
                            )
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = option == difficulty, onClick = null)
                        Text(
                            text = stringResource(option.labelRes()),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = instantCheck,
                        onValueChange = onInstantCheckChanged,
                        role = Role.Switch,
                    )
                    .padding(Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.sudoku_instant_check),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(R.string.sudoku_instant_check_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = instantCheck, onCheckedChange = null)
            }
        }
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("sudoku_start"),
        ) {
            Text(stringResource(R.string.action_start_game))
        }
    }
}

@Composable
private fun SudokuPlay(
    state: SudokuState,
    difficulty: Difficulty,
    onAction: (SudokuAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = formatDuration(state.elapsedSeconds),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .testTag("sudoku_timer"),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.xs, vertical = Spacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(difficulty.labelRes()),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.game_mistakes_count, state.mistakes),
                style = MaterialTheme.typography.bodyLarge,
                color = if (state.mistakes > 0) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        SudokuBoard(
            state = state,
            onCellClick = { onAction(SudokuAction.SelectCell(it)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            GameActionButton(
                iconRes = R.drawable.ic_close,
                label = stringResource(R.string.action_erase),
                onClick = { onAction(SudokuAction.Erase) },
                modifier = Modifier.testTag("sudoku_erase"),
            )
            GameActionButton(
                iconRes = R.drawable.ic_hint,
                label = stringResource(R.string.action_hint),
                onClick = { onAction(SudokuAction.Hint) },
                modifier = Modifier.testTag("sudoku_hint"),
            )
            // Reserved for a second kind of hint.
            GameActionButton(
                iconRes = R.drawable.ic_hint_outline,
                label = stringResource(R.string.action_coming_soon),
                onClick = {},
                enabled = false,
            )
            if (state.checkMode == SudokuCheckMode.CLASSIC) {
                GameActionButton(
                    iconRes = R.drawable.ic_check,
                    label = stringResource(R.string.action_check),
                    onClick = { onAction(SudokuAction.Check) },
                )
            }
        }
        Spacer(Modifier.weight(1f))
        SudokuNumberPad(
            values = state.values,
            onDigit = { onAction(SudokuAction.InputDigit(it)) },
        )
        Spacer(Modifier.height(Spacing.lg))
    }
}

@Composable
private fun SudokuResult(
    state: SudokuState,
    score: Int,
    onPlayAgain: () -> Unit,
    onChangeSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Spacing.xl))
        Text(
            text = stringResource(R.string.result_done),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.testTag("sudoku_result"),
        )
        Spacer(Modifier.height(Spacing.lg))
        Text(
            text = formatDuration(state.elapsedSeconds),
            style = MaterialTheme.typography.displaySmall,
        )
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
                ResultRow(stringResource(R.string.result_mistakes), state.mistakes.toString())
                ResultRow(stringResource(R.string.result_hints), state.hintsUsed.toString())
                ResultRow(
                    label = stringResource(R.string.result_accuracy),
                    value = if (state.entries > 0) {
                        val correct = (state.entries - state.mistakes).coerceAtLeast(0)
                        stringResource(R.string.percent_value, correct * 100 / state.entries)
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
            Text(stringResource(R.string.action_change_difficulty))
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
