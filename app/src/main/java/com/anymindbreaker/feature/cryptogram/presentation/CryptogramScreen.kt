package com.anymindbreaker.feature.cryptogram.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.anymindbreaker.core.common.game.Language
import com.anymindbreaker.core.ui.components.DifficultyGroup
import com.anymindbreaker.core.ui.components.GameActionButton
import com.anymindbreaker.core.ui.components.GameResultContent
import com.anymindbreaker.core.ui.components.GameScaffold
import com.anymindbreaker.core.ui.components.GameSetupColumn
import com.anymindbreaker.core.ui.components.GameStatusHeader
import com.anymindbreaker.core.ui.components.LoadingContent
import com.anymindbreaker.core.ui.components.OptionGroup
import com.anymindbreaker.core.ui.components.ResumeGameDialog
import com.anymindbreaker.core.ui.components.SwitchCard
import com.anymindbreaker.core.ui.components.accuracyPercent
import com.anymindbreaker.core.ui.components.labelRes
import com.anymindbreaker.core.ui.feedback.GameFeedback
import com.anymindbreaker.core.ui.feedback.GameFeedbackEffect
import com.anymindbreaker.core.ui.theme.Spacing
import com.anymindbreaker.feature.cryptogram.domain.CryptogramAction
import com.anymindbreaker.feature.cryptogram.domain.CryptogramState

@Composable
fun CryptogramScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    resumeSavedGame: Boolean = false,
    feedback: GameFeedback = appContainer().feedback,
    viewModel: CryptogramViewModel = run {
        val container = appContainer()
        viewModel {
            CryptogramViewModel(
                textSource = container.cryptogramTexts,
                scoreCalculator = container.scoreCalculator,
                repository = container.gameRepository,
                persistenceScope = container.applicationScope,
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
        title = stringResource(R.string.game_cryptogram),
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = Spacing.md)
        val game = uiState.game
        when {
            uiState.phase == GamePhase.SETUP -> GameSetupColumn(
                onStart = viewModel::onStartGame,
                modifier = contentModifier,
                startTestTag = "crypto_start",
            ) {
                OptionGroup(
                    title = stringResource(R.string.puzzle_language_title),
                    options = Language.entries,
                    selected = uiState.language,
                    label = { stringResource(it.labelRes()) },
                    onSelect = viewModel::onLanguageSelected,
                )
                DifficultyGroup(
                    selected = uiState.difficulty,
                    onSelect = viewModel::onDifficultySelected,
                )
                SwitchCard(
                    title = stringResource(R.string.game_lives),
                    description = stringResource(R.string.game_lives_description),
                    checked = uiState.livesEnabled,
                    onCheckedChange = viewModel::onLivesEnabledChanged,
                )
            }
            uiState.phase == GamePhase.FINISHED && game != null -> GameResultContent(
                result = game.result,
                elapsedSeconds = game.elapsedSeconds,
                score = uiState.score,
                mistakes = game.mistakes,
                hintsUsed = game.hintsUsed,
                accuracyPercent = accuracyPercent(game.entries, game.mistakes),
                onPlayAgain = viewModel::onStartGame,
                onChangeSettings = viewModel::onNewGame,
                modifier = contentModifier,
                testTag = "crypto_result",
            )
            uiState.phase == GamePhase.PLAYING && game != null -> CryptogramPlay(
                state = game,
                language = uiState.gameLanguage,
                difficulty = uiState.difficulty,
                onAction = viewModel::onAction,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = Spacing.xs),
            )
            else -> LoadingContent(modifier = contentModifier)
        }
    }
}

@Composable
private fun CryptogramPlay(
    state: CryptogramState,
    language: Language,
    difficulty: Difficulty,
    onAction: (CryptogramAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val select: (Int) -> Unit = { onAction(CryptogramAction.SelectPosition(it)) }
    val solvedLetters = state.guesses.filterKeys { it in state.locked }.values.toSet()

    Column(modifier = modifier) {
        GameStatusHeader(
            elapsedSeconds = state.elapsedSeconds,
            difficulty = difficulty,
            mistakes = state.mistakes,
            livesLeft = state.livesLeft,
        )
        // The text can be longer than the screen, so only this part scrolls.
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.xs),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            CryptogramText(state = state, language = language, onSelect = select)
            Text(
                text = stringResource(R.string.cryptogram_table_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CryptogramMappingTable(state = state, language = language, onSelect = select)
        }
        Spacer(Modifier.height(Spacing.xs))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            GameActionButton(
                iconRes = R.drawable.ic_close,
                label = stringResource(R.string.action_erase),
                onClick = { onAction(CryptogramAction.Erase) },
            )
            GameActionButton(
                iconRes = R.drawable.ic_hint,
                label = stringResource(R.string.hint_letter),
                onClick = { onAction(CryptogramAction.HintLetter) },
                modifier = Modifier.testTag("crypto_hint_letter"),
            )
            GameActionButton(
                iconRes = R.drawable.ic_hint_word,
                label = stringResource(R.string.hint_word),
                onClick = { onAction(CryptogramAction.HintWord) },
            )
            GameActionButton(
                iconRes = R.drawable.ic_hint_position,
                label = stringResource(R.string.hint_position),
                onClick = { onAction(CryptogramAction.HintPosition) },
            )
        }
        Spacer(Modifier.height(Spacing.xs))
        LetterKeyboard(
            language = language,
            solvedLetters = solvedLetters,
            guessedLetters = state.guesses.values.toSet() - solvedLetters,
            onLetter = { onAction(CryptogramAction.InputLetter(it)) },
        )
        Spacer(Modifier.height(Spacing.md))
    }
}
