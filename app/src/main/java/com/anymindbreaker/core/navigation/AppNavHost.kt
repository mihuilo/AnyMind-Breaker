package com.anymindbreaker.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.anymindbreaker.feature.cryptogram.presentation.CryptogramScreen
import com.anymindbreaker.feature.games.presentation.GamesScreen
import com.anymindbreaker.feature.home.presentation.HomeScreen
import com.anymindbreaker.feature.settings.presentation.SettingsScreen
import com.anymindbreaker.feature.statistics.presentation.StatisticsScreen
import com.anymindbreaker.feature.sudoku.presentation.SudokuScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier,
    ) {
        composable<HomeRoute> {
            HomeScreen(
                onOpenCryptogram = { navController.navigate(CryptogramRoute) },
                onOpenSudoku = { navController.navigate(SudokuRoute) },
            )
        }
        composable<GamesRoute> {
            GamesScreen(
                onOpenCryptogram = { navController.navigate(CryptogramRoute) },
                onOpenSudoku = { navController.navigate(SudokuRoute) },
            )
        }
        composable<StatisticsRoute> { StatisticsScreen() }
        composable<SettingsRoute> { SettingsScreen() }
        composable<CryptogramRoute> { CryptogramScreen(onBack = navController::popBackStack) }
        composable<SudokuRoute> { SudokuScreen(onBack = navController::popBackStack) }
    }
}
