package com.anymindbreaker.app

import android.app.Application
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.room.Room
import com.anymindbreaker.core.common.game.DefaultScoreCalculator
import com.anymindbreaker.core.common.game.GameRepository
import com.anymindbreaker.core.common.game.ScoreCalculator
import com.anymindbreaker.core.database.AppDatabase
import com.anymindbreaker.core.database.RoomGameRepository
import com.anymindbreaker.feature.cryptogram.data.CryptogramTextRepository
import com.anymindbreaker.feature.cryptogram.data.CryptogramTextSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AnyMindBreakerApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

/** Application-wide dependencies, created once and handed to ViewModels. */
class AppContainer(context: Context) {
    /** Lives as long as the process; used for writes that must outlive a screen. */
    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val scoreCalculator: ScoreCalculator = DefaultScoreCalculator()
    val cryptogramTexts: CryptogramTextSource = CryptogramTextRepository(context.assets)

    private val database: AppDatabase by lazy {
        Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "any_mind_breaker.db").build()
    }

    val gameRepository: GameRepository by lazy { RoomGameRepository(database.gameDao()) }
}

@Composable
fun appContainer(): AppContainer =
    (LocalContext.current.applicationContext as AnyMindBreakerApplication).container
