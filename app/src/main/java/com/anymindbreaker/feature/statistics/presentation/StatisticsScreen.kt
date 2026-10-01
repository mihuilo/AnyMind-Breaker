package com.anymindbreaker.feature.statistics.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.anymindbreaker.R
import com.anymindbreaker.core.ui.components.PlaceholderScreen

@Composable
fun StatisticsScreen(
    modifier: Modifier = Modifier,
) {
    PlaceholderScreen(
        title = stringResource(R.string.statistics_title),
        modifier = modifier,
    )
}
