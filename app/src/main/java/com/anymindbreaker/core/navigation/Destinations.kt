package com.anymindbreaker.core.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.anymindbreaker.R
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object GamesRoute

@Serializable
data object StatisticsRoute

@Serializable
data object SettingsRoute

@Serializable
data class CryptogramRoute(val resume: Boolean = false)

@Serializable
data class SudokuRoute(val resume: Boolean = false)

/** Destinations shown in the bottom navigation bar. */
enum class TopLevelDestination(
    val route: Any,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
) {
    HOME(HomeRoute, R.string.nav_home, R.drawable.ic_home),
    GAMES(GamesRoute, R.string.nav_games, R.drawable.ic_games),
    STATISTICS(StatisticsRoute, R.string.nav_statistics, R.drawable.ic_statistics),
    SETTINGS(SettingsRoute, R.string.nav_settings, R.drawable.ic_settings),
}
