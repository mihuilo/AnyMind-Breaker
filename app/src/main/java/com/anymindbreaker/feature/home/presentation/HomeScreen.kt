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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.anymindbreaker.R
import com.anymindbreaker.core.ui.components.GameCard
import com.anymindbreaker.core.ui.components.StatCard
import com.anymindbreaker.core.ui.theme.Spacing

@Composable
fun HomeScreen(
    onOpenCryptogram: () -> Unit,
    onOpenSudoku: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            GameCard(
                title = stringResource(R.string.game_cryptogram),
                description = stringResource(R.string.game_cryptogram_description),
                iconRes = R.drawable.ic_cryptogram,
                onClick = onOpenCryptogram,
            )
            GameCard(
                title = stringResource(R.string.game_sudoku),
                description = stringResource(R.string.game_sudoku_description),
                iconRes = R.drawable.ic_sudoku,
                onClick = onOpenSudoku,
            )
        }
    }
}
