package com.anymindbreaker.feature.statistics.presentation

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anymindbreaker.R
import com.anymindbreaker.app.appContainer
import com.anymindbreaker.core.common.formatDuration
import com.anymindbreaker.core.common.game.GameResult
import com.anymindbreaker.core.common.game.GameSession
import com.anymindbreaker.core.ui.components.LabeledValue
import com.anymindbreaker.core.ui.components.LoadingContent
import com.anymindbreaker.core.ui.components.labelRes
import com.anymindbreaker.core.ui.components.titleRes
import com.anymindbreaker.core.ui.theme.Spacing
import com.anymindbreaker.feature.statistics.domain.Achievement
import com.anymindbreaker.feature.statistics.domain.GameSummary
import com.anymindbreaker.feature.statistics.domain.StatisticsOverview
import java.text.DateFormat
import java.util.Date

private enum class StatisticsTab(@StringRes val labelRes: Int) {
    GENERAL(R.string.statistics_tab_general),
    BY_GAME(R.string.statistics_tab_games),
    BY_LANGUAGE(R.string.statistics_tab_languages),
    HISTORY(R.string.statistics_tab_history),
    ACHIEVEMENTS(R.string.statistics_tab_achievements),
}

@Composable
fun StatisticsScreen(
    modifier: Modifier = Modifier,
    viewModel: StatisticsViewModel = run {
        val container = appContainer()
        viewModel { StatisticsViewModel(container.gameRepository) }
    },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(StatisticsTab.GENERAL) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = Spacing.lg),
    ) {
        Text(
            text = stringResource(R.string.statistics_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = Spacing.md),
        )
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = Spacing.md, vertical = Spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            for (option in StatisticsTab.entries) {
                FilterChip(
                    selected = option == tab,
                    onClick = { tab = option },
                    label = { Text(stringResource(option.labelRes)) },
                    modifier = Modifier.testTag("statistics_tab_${option.name}"),
                )
            }
        }

        val contentModifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.md)
        when {
            uiState.loading -> LoadingContent(modifier = contentModifier)
            tab == StatisticsTab.ACHIEVEMENTS -> AchievementList(uiState.achievements, contentModifier)
            uiState.history.isEmpty() -> EmptyStatistics(contentModifier)
            tab == StatisticsTab.HISTORY -> HistoryList(uiState.history, contentModifier)
            else -> Column(
                modifier = contentModifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                when (tab) {
                    StatisticsTab.GENERAL -> GeneralStatistics(uiState.overview)
                    StatisticsTab.BY_GAME -> uiState.overview.byGame.forEach { (game, summary) ->
                        SummaryCard(title = stringResource(game.titleRes()), summary = summary)
                    }
                    else -> uiState.overview.byLanguage.forEach { (language, summary) ->
                        SummaryCard(title = stringResource(language.labelRes()), summary = summary)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStatistics(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.Center) {
        Text(
            text = stringResource(R.string.statistics_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun percent(value: Int?): String =
    if (value != null) stringResource(R.string.percent_value, value) else stringResource(R.string.value_empty)

@Composable
private fun duration(seconds: Long?): String =
    if (seconds != null) formatDuration(seconds) else stringResource(R.string.value_empty)

@Composable
private fun StatisticsCard(
    title: String?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            if (title != null) Text(text = title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun GeneralStatistics(overview: StatisticsOverview) {
    val summary = overview.overall
    StatisticsCard(title = null, modifier = Modifier.testTag("statistics_general")) {
        LabeledValue(stringResource(R.string.statistics_played), summary.played.toString())
        LabeledValue(stringResource(R.string.statistics_completed), summary.completed.toString())
        LabeledValue(stringResource(R.string.statistics_failed), summary.failed.toString())
        LabeledValue(stringResource(R.string.statistics_success_rate), percent(summary.successPercent))
        LabeledValue(stringResource(R.string.statistics_average_time), duration(summary.averageSeconds))
        LabeledValue(stringResource(R.string.statistics_best_time), duration(summary.bestSeconds))
    }
    StatisticsCard(title = null) {
        LabeledValue(stringResource(R.string.result_mistakes), summary.mistakes.toString())
        LabeledValue(stringResource(R.string.result_hints), summary.hints.toString())
        LabeledValue(stringResource(R.string.result_accuracy), percent(summary.accuracyPercent))
        LabeledValue(stringResource(R.string.statistics_score), summary.score.toString())
    }
    StatisticsCard(title = null) {
        LabeledValue(stringResource(R.string.statistics_current_streak), overview.currentStreak.toString())
        LabeledValue(stringResource(R.string.statistics_best_streak), overview.bestStreak.toString())
    }
}

@Composable
private fun SummaryCard(title: String, summary: GameSummary) {
    StatisticsCard(title = title) {
        LabeledValue(stringResource(R.string.statistics_completed), summary.completed.toString())
        LabeledValue(stringResource(R.string.result_mistakes), summary.mistakes.toString())
        LabeledValue(stringResource(R.string.statistics_average_time), duration(summary.averageSeconds))
        LabeledValue(stringResource(R.string.statistics_best_time), duration(summary.bestSeconds))
        LabeledValue(stringResource(R.string.result_accuracy), percent(summary.accuracyPercent))
        LabeledValue(stringResource(R.string.result_hints), summary.hints.toString())
    }
}

@StringRes
private fun GameResult.labelRes(): Int = when (this) {
    GameResult.COMPLETED -> R.string.result_label_completed
    GameResult.FAILED -> R.string.result_label_failed
    GameResult.ABANDONED -> R.string.result_label_abandoned
    GameResult.IN_PROGRESS -> R.string.result_label_in_progress
}

@Composable
private fun HistoryList(sessions: List<GameSession>, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val dateFormat = remember(locale) { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale) }

    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        items(sessions, key = { it.sessionId }) { session ->
            StatisticsCard(title = null) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(session.gameType.titleRes()),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(session.result.labelRes()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = when (session.result) {
                            GameResult.COMPLETED -> MaterialTheme.colorScheme.secondary
                            GameResult.FAILED -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                val details = listOfNotNull(
                    stringResource(session.difficulty.labelRes()),
                    session.language?.let { stringResource(it.labelRes()) },
                    session.finishedAt?.let { dateFormat.format(Date(it)) },
                )
                Text(
                    text = details.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(
                        R.string.history_line,
                        formatDuration(session.durationSeconds),
                        session.score,
                        session.mistakes,
                        session.hintsUsed,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@StringRes
private fun Achievement.titleRes(): Int = when (this) {
    Achievement.FIRST_WIN -> R.string.achievement_first_win
    Achievement.SOLVED_10 -> R.string.achievement_solved_10
    Achievement.SOLVED_100 -> R.string.achievement_solved_100
    Achievement.FLAWLESS_10 -> R.string.achievement_flawless_10
    Achievement.SUDOKU_STREAK_10 -> R.string.achievement_sudoku_streak_10
    Achievement.CRYPTOGRAM_NO_HINTS -> R.string.achievement_cryptogram_no_hints
    Achievement.UNDER_A_MINUTE -> R.string.achievement_under_a_minute
}

@Composable
private fun AchievementList(unlocked: Set<Achievement>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        for (achievement in Achievement.entries) {
            val isUnlocked = achievement in unlocked
            StatisticsCard(title = null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(if (isUnlocked) R.drawable.ic_check else R.drawable.ic_cryptogram),
                        contentDescription = stringResource(
                            if (isUnlocked) R.string.achievement_unlocked else R.string.achievement_locked,
                        ),
                        modifier = Modifier.size(24.dp),
                        tint = if (isUnlocked) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                    )
                    Text(
                        text = stringResource(achievement.titleRes()),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isUnlocked) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}
