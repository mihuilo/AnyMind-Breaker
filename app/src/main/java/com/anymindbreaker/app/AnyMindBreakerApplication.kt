package com.anymindbreaker.app

import android.app.Application
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.anymindbreaker.core.common.game.DefaultScoreCalculator
import com.anymindbreaker.core.common.game.GameRepository
import com.anymindbreaker.core.common.game.Language
import com.anymindbreaker.core.common.game.ScoreCalculator
import com.anymindbreaker.core.database.AppDatabase
import com.anymindbreaker.core.database.RoomGameRepository
import com.anymindbreaker.core.datastore.DataStoreSettingsRepository
import com.anymindbreaker.core.datastore.SettingsRepository
import com.anymindbreaker.core.ui.feedback.AndroidGameFeedback
import com.anymindbreaker.core.ui.feedback.GameFeedback
import com.anymindbreaker.feature.cryptogram.data.CryptogramTextRepository
import com.anymindbreaker.feature.cryptogram.data.CryptogramTextSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.Locale

class AnyMindBreakerApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

/** Application-wide dependencies, created once and handed to ViewModels. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    /** Lives as long as the process; used for writes that must outlive a screen. */
    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val scoreCalculator: ScoreCalculator = DefaultScoreCalculator()
    val cryptogramTexts: CryptogramTextSource = CryptogramTextRepository(appContext.assets)

    val settings: SettingsRepository = DataStoreSettingsRepository(
        dataStore = PreferenceDataStoreFactory.create { appContext.preferencesDataStoreFile("settings") },
        // Before the player chooses, the app follows the system language.
        defaultLanguage = if (Locale.getDefault().language == "ru") Language.RU else Language.EN,
    )

    private val database: AppDatabase by lazy {
        Room.databaseBuilder(appContext, AppDatabase::class.java, "any_mind_breaker.db")
            .addMigrations(*AppDatabase.MIGRATIONS)
            .build()
    }

    val gameRepository: GameRepository by lazy { RoomGameRepository(database.gameDao()) }

    val feedback: GameFeedback by lazy { AndroidGameFeedback(appContext, settings, applicationScope) }
}

@Composable
fun appContainer(): AppContainer =
    (LocalContext.current.applicationContext as AnyMindBreakerApplication).container
