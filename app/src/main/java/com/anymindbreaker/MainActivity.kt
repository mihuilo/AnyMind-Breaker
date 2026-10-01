package com.anymindbreaker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.anymindbreaker.app.AnyMindBreakerApp
import com.anymindbreaker.core.ui.theme.AnyMindBreakerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AnyMindBreakerTheme {
                AnyMindBreakerApp()
            }
        }
    }
}
