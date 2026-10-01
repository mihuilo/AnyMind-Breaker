package com.anymindbreaker.feature.sudoku.presentation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anymindbreaker.R
import com.anymindbreaker.app.appContainer
import com.anymindbreaker.core.common.game.Difficulty
import com.anymindbreaker.core.common.game.GamePhase
import com.anymindbreaker.core.ui.components.DifficultyGroup
import com.anymindbreaker.core.ui.components.GameActionButton
import com.anymindbreaker.core.ui.components.GameResultContent
import com.anymindbreaker.core.ui.components.GameScaffold
import com.anymindbreaker.core.ui.components.GameSetupColumn
import com.anymindbreaker.core.ui.components.GameStatusHeader
import com.anymindbreaker.core.ui.components.LoadingContent
import com.anymindbreaker.core.ui.components.PHASE_FADE_MILLIS
import com.anymindbreaker.core.ui.components.ResumeGameDialog
import com.anymindbreaker.core.ui.components.SwitchCard
import com.anymindbreaker.core.ui.components.accuracyPercent
import com.anymindbreaker.core.ui.feedback.GameFeedback
import com.anymindbreaker.core.ui.feedback.GameFeedbackEffect
import com.anymindbreaker.core.ui.theme.Spacing
import com.anymindbreaker.feature.sudoku.domain.SudokuAction
import com.anymindbreaker.feature.sudoku.domain.SudokuCheckMode
import com.anymindbreaker.feature.sudoku.domain.SudokuState

@Composable
fun SudokuScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    resumeSavedGame: Boolean = false,
    onOpenStatistics: () -> Unit = {},
    feedback: GameFeedback = appContainer().feedback,
    viewModel: SudokuViewModel = run {
        val container = appContainer()
        viewModel {
            SudokuViewModel(
                repository = container.gameRepository,
                persistenceScope = container.applicationScope,
                scoreCalculator = container.scoreCalculator,
                settings = container.settings,
                resumeSavedGame = resumeSavedGame,
            )
        }
    },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val savedGameAvailable by viewModel.savedGameAvailable.collectAsStateWithLifecycle()

    GameFeedbackEffect(uiState.game, feedback)

    if (savedGameAvailable && uiState.phase == GamePhase.SETUP) {
        ResumeGameDialog(
            onResume = viewModel::onResumeSavedGame,
            onStartNew = viewModel::onDismissSavedGame,
        )
    }

    LifecycleResumeEffect(viewModel) {
        viewModel.onScreenVisibilityChanged(true)
        onPauseOrDispose { viewModel.onScreenVisibilityChanged(false) }
    }

    GameScaffold(
        title = stringResource(R.string.game_sudoku),
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = Spacing.md)
        val game = uiState.game
        // A short fade keeps the move between setup, play and result from feeling abrupt.
        Crossfade(
            targetState = uiState.phase,
            animationSpec = tween(PHASE_FADE_MILLIS),
            label = "game phase",
        ) { phase ->
            when {
                phase == GamePhase.SETUP -> GameSetupColumn(
                    onStart = viewModel::onStartGame,
                    modifier = contentModifier,
                    startTestTag = "sudoku_start",
                ) {
                    DifficultyGroup(
                        selected = uiState.difficulty,
                        onSelect = viewModel::onDifficultySelected,
                    )
                    SwitchCard(
                        title = stringResource(R.string.sudoku_instant_check),
                        description = stringResource(R.string.sudoku_instant_check_description),
                        checked = uiState.instantCheck,
                        onCheckedChange = viewModel::onInstantCheckChanged,
                    )
                }
                phase == GamePhase.FINISHED && game != null -> GameResultContent(
                    result = game.result,
                    elapsedSeconds = game.elapsedSeconds,
                    score = uiState.score,
                    mistakes = game.mistakes,
                    hintsUsed = game.hintsUsed,
                    accuracyPercent = accuracyPercent(game.entries, game.mistakes),
                    onPlayAgain = viewModel::onStartGame,
                    onChangeSettings = viewModel::onNewGame,
                    onOpenStatistics = onOpenStatistics,
                    modifier = contentModifier,
                    testTag = "sudoku_result",
                )
                phase == GamePhase.PLAYING && game != null -> SudokuPlay(
                    state = game,
                    difficulty = uiState.difficulty,
                    onAction = viewModel::onAction,
                    // Narrower side margins than the other phases leave more room for the grid.
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = Spacing.xs),
                )
                else -> LoadingContent(modifier = contentModifier)
            }
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
        GameStatusHeader(
            elapsedSeconds = state.elapsedSeconds,
            difficulty = difficulty,
            mistakes = state.mistakes,
            livesLeft = state.livesLeft,
        )
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
