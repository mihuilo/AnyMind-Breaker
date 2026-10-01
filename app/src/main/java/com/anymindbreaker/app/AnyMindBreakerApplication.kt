package com.anymindbreaker.app

import android.app.Application
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.anymindbreaker.core.common.game.DefaultScoreCalculator
import com.anymindbreaker.core.common.game.ScoreCalculator
import com.anymindbreaker.feature.cryptogram.data.CryptogramTextRepository
import com.anymindbreaker.feature.cryptogram.data.CryptogramTextSource

class AnyMindBreakerApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

/** Application-wide dependencies, created once and handed to ViewModels. */
class AppContainer(context: Context) {
    val scoreCalculator: ScoreCalculator = DefaultScoreCalculator()
    val cryptogramTexts: CryptogramTextSource = CryptogramTextRepository(context.assets)
}

@Composable
fun appContainer(): AppContainer =
    (LocalContext.current.applicationContext as AnyMindBreakerApplication).container
