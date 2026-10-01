package com.anymindbreaker

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anymindbreaker.app.AnyMindBreakerApp
import com.anymindbreaker.app.AnyMindBreakerApplication
import com.anymindbreaker.core.common.game.Language
import com.anymindbreaker.core.datastore.AppSettings
import com.anymindbreaker.core.datastore.ThemeMode
import com.anymindbreaker.core.ui.theme.AnyMindBreakerTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var initialSettings: AppSettings

    override fun attachBaseContext(newBase: Context) {
        val container = (newBase.applicationContext as AnyMindBreakerApplication).container
        // Read synchronously: resources must be in the chosen language before the first frame.
        initialSettings = runBlocking { container.settings.settings.first() }
        val configuration = Configuration(newBase.resources.configuration)
        configuration.setLocale(initialSettings.uiLanguage.toLocale())
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as AnyMindBreakerApplication).container
        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle(initialSettings)

            // Strings are resolved from the activity resources, so a new language needs a new activity.
            LaunchedEffect(settings.uiLanguage) {
                if (settings.uiLanguage != initialSettings.uiLanguage) recreate()
            }

            val darkTheme = when (settings.theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            DisposableEffect(darkTheme) {
                // Keeps status and navigation bar icons readable on the chosen theme.
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }

            AnyMindBreakerTheme(darkTheme = darkTheme) {
                AnyMindBreakerApp()
            }
        }
    }
}

private fun Language.toLocale(): Locale = when (this) {
    Language.RU -> Locale.forLanguageTag("ru")
    Language.EN -> Locale.ENGLISH
}
