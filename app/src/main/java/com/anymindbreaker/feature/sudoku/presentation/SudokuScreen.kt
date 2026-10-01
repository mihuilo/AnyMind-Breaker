package com.anymindbreaker.feature.sudoku.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.anymindbreaker.R
import com.anymindbreaker.core.ui.components.PlaceholderScreen

@Composable
fun SudokuScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlaceholderScreen(
        title = stringResource(R.string.game_sudoku),
        modifier = modifier,
        onBack = onBack,
    )
}
