package com.anymindbreaker.feature.statistics.presentation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anymindbreaker.R
import com.anymindbreaker.app.appContainer
import com.anymindbreaker.core.common.formatDuration
import com.anymindbreaker.core.common.game.Difficulty
import com.anymindbreaker.core.common.game.GameResult
import com.anymindbreaker.core.common.game.GameSession
import com.anymindbreaker.core.common.game.GameType
import com.anymindbreaker.core.common.game.Language
import com.anymindbreaker.core.ui.components.LabeledValue
import com.anymindbreaker.core.ui.components.LoadingContent
import com.anymindbreaker.core.ui.components.iconRes
import com.anymindbreaker.core.ui.components.labelRes
import com.anymindbreaker.core.ui.components.titleRes
import com.anymindbreaker.core.ui.theme.Spacing
import com.anymindbreaker.feature.statistics.domain.Achievement
import com.anymindbreaker.feature.statistics.domain.GameSummary
import com.anymindbreaker.feature.statistics.domain.Highlights
import java.text.DateFormat
import java.util.Date

/** Sections of the screen that are not tied to one game. */
private enum class ExtraTab(@StringRes val labelRes: Int) {
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
    // Null means one of the game tabs is open; the game itself is part of the filter.
    var extraTab by rememberSaveable { mutableStateOf<ExtraTab?>(null) }

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
                .padding(horizontal = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            for (game in GameType.entries) {
                StatisticsTab(
                    label = stringResource(game.titleRes()),
                    selected = extraTab == null && uiState.filter.gameType == game,
                    onClick = {
                        extraTab = null
                        viewModel.onGameSelected(game)
                    },
                    modifier = Modifier.testTag("statistics_tab_${game.name}"),
                )
            }
            for (tab in ExtraTab.entries) {
                StatisticsTab(
                    label = stringResource(tab.labelRes),
                    selected = extraTab == tab,
                    onClick = { extraTab = tab },
                    modifier = Modifier.testTag("statistics_tab_${tab.name}"),
                )
            }
        }

        val contentModifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.md)
        when {
            uiState.loading -> LoadingContent(modifier = contentModifier)
            extraTab == ExtraTab.ACHIEVEMENTS -> AchievementList(uiState.achievements, contentModifier)
            extraTab == ExtraTab.HISTORY ->
                if (uiState.history.isEmpty()) EmptyHistory(contentModifier) else HistoryList(uiState.history, contentModifier)
            else -> GameStatistics(
                uiState = uiState,
                onDifficultySelected = viewModel::onDifficultySelected,
                onLanguageSelected = viewModel::onLanguageSelected,
            )
        }
    }
}

/** A text tab with an underline, used to choose the game. */
@Composable
private fun StatisticsTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(IntrinsicSize.Max)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(top = Spacing.sm),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = Spacing.xs),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface),
        )
    }
}

@Composable
private fun <T> ChipRow(
    options: List<T?>,
    selected: T?,
    label: @Composable (T?) -> String,
    onSelect: (T?) -> Unit,
    tagPrefix: String,
) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        for (option in options) {
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.testTag("${tagPrefix}_${option ?: "ALL"}"),
            )
        }
    }
}

@Composable
private fun GameStatistics(
    uiState: StatisticsUiState,
    onDifficultySelected: (Difficulty?) -> Unit,
    onLanguageSelected: (Language?) -> Unit,
) {
    val filter = uiState.filter
    val highlights = uiState.highlights

    Column(modifier = Modifier.fillMaxSize()) {
        ChipRow(
            options = listOf<Difficulty?>(null) + Difficulty.entries,
            selected = filter.difficulty,
            label = { stringResource(it?.labelRes() ?: R.string.statistics_all) },
            onSelect = onDifficultySelected,
            tagPrefix = "statistics_difficulty",
        )
        // Only word games have a puzzle language.
        if (filter.gameType == GameType.CRYPTOGRAM) {
            ChipRow(
                options = listOf<Language?>(null) + Language.entries,
                selected = filter.language,
                label = { stringResource(it?.labelRes() ?: R.string.statistics_all_languages) },
                onSelect = onLanguageSelected,
                tagPrefix = "statistics_language",
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.md, vertical = Spacing.xs),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            HighlightCard(
                iconRes = filter.gameType.iconRes(),
                label = stringResource(R.string.statistics_wins),
                value = highlights.wins.toString(),
                gain = highlights.recentWins,
                modifier = Modifier.testTag("statistics_card_wins"),
            )
            HighlightCard(
                iconRes = R.drawable.ic_thumb_up,
                label = stringResource(R.string.statistics_flawless_wins),
                value = highlights.flawlessWins.toString(),
                gain = highlights.recentFlawlessWins,
                modifier = Modifier.testTag("statistics_card_flawless"),
            )
            HighlightCard(
                iconRes = R.drawable.ic_flag,
                label = stringResource(R.string.statistics_best_streak),
                value = highlights.bestStreak.toString(),
                modifier = Modifier.testTag("statistics_card_streak"),
            )
            HighlightCard(
                iconRes = R.drawable.ic_timer,
                label = stringResource(R.string.statistics_best_time),
                value = duration(highlights.bestSeconds),
                modifier = Modifier.testTag("statistics_card_best_time"),
            )
            Text(
                text = stringResource(R.string.statistics_gain_caption),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DetailsCard(uiState.summary, highlights)
        }
    }
}

@Composable
private fun HighlightCard(
    @DrawableRes iconRes: Int,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    /** Gain over the recent period; shown only when there is one. */
    gain: Int = 0,
) {
    Card(
        // Read out as one item: label, gain and value belong together.
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(text = label, style = MaterialTheme.typography.bodyLarge)
            }
            if (gain > 0) {
                Row(
                    modifier = Modifier.padding(end = Spacing.sm, bottom = Spacing.xxs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_up),
                        contentDescription = stringResource(R.string.statistics_gain_description),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                    Text(
                        text = stringResource(R.string.statistics_gain_value, gain),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun percent(value: Int?): String =
    if (value != null) stringResource(R.string.percent_value, value) else stringResource(R.string.value_empty)

@Composable
private fun duration(seconds: Long?): String =
    if (seconds != null) formatDuration(seconds) else stringResource(R.string.value_empty)

@Composable
private fun playTime(seconds: Long): String {
    val minutes = (seconds / 60).toInt()
    return if (minutes >= 60) {
        stringResource(R.string.hours_minutes, minutes / 60, minutes % 60)
    } else {
        stringResource(R.string.minutes_short, minutes)
    }
}

@Composable
private fun StatisticsCard(
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
            content()
        }
    }
}

/** Everything that does not fit the headline cards. */
@Composable
private fun DetailsCard(summary: GameSummary, highlights: Highlights) {
    StatisticsCard(modifier = Modifier.testTag("statistics_details")) {
        Text(text = stringResource(R.string.statistics_details), style = MaterialTheme.typography.titleMedium)
        LabeledValue(stringResource(R.string.statistics_played), summary.played.toString())
        LabeledValue(stringResource(R.string.statistics_failed), summary.failed.toString())
        LabeledValue(stringResource(R.string.statistics_success_rate), percent(summary.successPercent))
        LabeledValue(stringResource(R.string.statistics_current_streak), highlights.currentStreak.toString())
        LabeledValue(stringResource(R.string.statistics_average_time), duration(summary.averageSeconds))
        LabeledValue(stringResource(R.string.statistics_play_time), playTime(highlights.playSeconds))
        LabeledValue(stringResource(R.string.result_accuracy), percent(summary.accuracyPercent))
        LabeledValue(stringResource(R.string.result_mistakes), summary.mistakes.toString())
        LabeledValue(stringResource(R.string.result_hints), summary.hints.toString())
        LabeledValue(stringResource(R.string.statistics_score), summary.score.toString())
    }
}

@Composable
private fun EmptyHistory(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.Center) {
        Text(
            text = stringResource(R.string.statistics_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
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

    LazyColumn(
        modifier = modifier.padding(top = Spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        items(sessions, key = { it.sessionId }) { session ->
            StatisticsCard {
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
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(top = Spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        for (achievement in Achievement.entries) {
            val isUnlocked = achievement in unlocked
            StatisticsCard {
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
