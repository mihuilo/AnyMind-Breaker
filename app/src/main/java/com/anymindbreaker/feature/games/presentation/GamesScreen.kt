package com.anymindbreaker.feature.games.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import com.anymindbreaker.core.ui.theme.Spacing

@Composable
fun GamesScreen(
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
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = stringResource(R.string.games_title),
            style = MaterialTheme.typography.headlineMedium,
        )
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
