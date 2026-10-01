package com.anymindbreaker.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.anymindbreaker.core.common.game.GameType
import com.anymindbreaker.feature.cryptogram.presentation.CryptogramScreen
import com.anymindbreaker.feature.games.presentation.GamesScreen
import com.anymindbreaker.feature.home.presentation.HomeScreen
import com.anymindbreaker.feature.settings.presentation.SettingsScreen
import com.anymindbreaker.feature.statistics.presentation.StatisticsScreen
import com.anymindbreaker.feature.sudoku.presentation.SudokuScreen

/** The route that opens a game, optionally continuing its unfinished game straight away. */
fun gameRoute(gameType: GameType, resume: Boolean = false): Any = when (gameType) {
    GameType.CRYPTOGRAM -> CryptogramRoute(resume)
    GameType.SUDOKU -> SudokuRoute(resume)
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val openStatistics: () -> Unit = {
        navController.navigate(StatisticsRoute) {
            // Leaves the finished game behind, as if the tab had been chosen in the bottom bar.
            popUpTo(navController.graph.findStartDestination().id)
            launchSingleTop = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier,
    ) {
        composable<HomeRoute> {
            HomeScreen(
                onOpenGame = { navController.navigate(gameRoute(it)) },
                onContinueGame = { navController.navigate(gameRoute(it, resume = true)) },
            )
        }
        composable<GamesRoute> {
            GamesScreen(onOpenGame = { navController.navigate(gameRoute(it)) })
        }
        composable<StatisticsRoute> { StatisticsScreen() }
        composable<SettingsRoute> { SettingsScreen() }
        composable<CryptogramRoute> { entry ->
            CryptogramScreen(
                onBack = navController::popBackStack,
                resumeSavedGame = entry.toRoute<CryptogramRoute>().resume,
                onOpenStatistics = openStatistics,
            )
        }
        composable<SudokuRoute> { entry ->
            SudokuScreen(
                onBack = navController::popBackStack,
                resumeSavedGame = entry.toRoute<SudokuRoute>().resume,
                onOpenStatistics = openStatistics,
            )
        }
    }
}
