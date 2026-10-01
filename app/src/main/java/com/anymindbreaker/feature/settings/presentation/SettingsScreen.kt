package com.anymindbreaker.feature.settings.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.anymindbreaker.R
import com.anymindbreaker.core.ui.components.PlaceholderScreen

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
) {
    PlaceholderScreen(
        title = stringResource(R.string.settings_title),
        modifier = modifier,
    )
}
