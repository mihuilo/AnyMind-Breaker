package com.anymindbreaker.feature.home.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anymindbreaker.R
import com.anymindbreaker.app.appContainer
import com.anymindbreaker.core.common.game.GameType
import com.anymindbreaker.core.ui.components.GameList
import com.anymindbreaker.core.ui.components.StatCard
import com.anymindbreaker.core.ui.components.titleRes
import com.anymindbreaker.core.ui.theme.Spacing

@Composable
fun HomeScreen(
    onOpenGame: (GameType) -> Unit,
    onContinueGame: (GameType) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = run {
        val container = appContainer()
        viewModel { HomeViewModel(container.gameRepository) }
    },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.md, vertical = Spacing.lg),
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(R.string.home_welcome),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        uiState.continueGame?.let { game ->
            Spacer(Modifier.height(Spacing.lg))
            Button(
                onClick = { onContinueGame(game) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_continue"),
            ) {
                Text(stringResource(R.string.home_continue, stringResource(game.titleRes())))
            }
        }

        Spacer(Modifier.height(Spacing.lg))
        Text(
            text = stringResource(R.string.home_today),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(Spacing.xs))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            StatCard(
                value = stringResource(R.string.minutes_short, 0),
                label = stringResource(R.string.home_today_play_time),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                value = stringResource(R.string.value_empty),
                label = stringResource(R.string.home_today_accuracy),
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(Spacing.lg))
        Text(
            text = stringResource(R.string.home_games),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(Spacing.xs))
        GameList(onOpenGame = onOpenGame)
    }
}
