package com.anymindbreaker.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private val Indigo40 = Color(0xFF4355B9)
private val Indigo80 = Color(0xFFBAC3FF)
private val Indigo90 = Color(0xFFDEE0FF)
private val Indigo10 = Color(0xFF00105C)
private val Indigo30 = Color(0xFF293CA0)

private val Teal40 = Color(0xFF006B5E)
private val Teal80 = Color(0xFF58DBC6)
private val Teal90 = Color(0xFF77F8E1)
private val Teal10 = Color(0xFF00201B)
private val Teal30 = Color(0xFF005047)

private val Red40 = Color(0xFFBA1A1A)
private val Red80 = Color(0xFFFFB4AB)
private val Red90 = Color(0xFFFFDAD6)
private val Red10 = Color(0xFF410002)
private val Red30 = Color(0xFF93000A)

val LightColors = lightColorScheme(
    primary = Indigo40,
    onPrimary = Color.White,
    primaryContainer = Indigo90,
    onPrimaryContainer = Indigo10,
    secondary = Teal40,
    onSecondary = Color.White,
    secondaryContainer = Teal90,
    onSecondaryContainer = Teal10,
    error = Red40,
    onError = Color.White,
    errorContainer = Red90,
    onErrorContainer = Red10,
    background = Color(0xFFFBF8FD),
    onBackground = Color(0xFF1B1B1F),
    surface = Color(0xFFFBF8FD),
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFFE3E1EC),
    onSurfaceVariant = Color(0xFF46464F),
    surfaceContainer = Color(0xFFF0EDF4),
    surfaceContainerHigh = Color(0xFFEAE7EF),
    outline = Color(0xFF767680),
    outlineVariant = Color(0xFFC7C5D0),
)

val DarkColors = darkColorScheme(
    primary = Indigo80,
    onPrimary = Color(0xFF08218A),
    primaryContainer = Indigo30,
    onPrimaryContainer = Indigo90,
    secondary = Teal80,
    onSecondary = Color(0xFF003730),
    secondaryContainer = Teal30,
    onSecondaryContainer = Teal90,
    error = Red80,
    onError = Color(0xFF690005),
    errorContainer = Red30,
    onErrorContainer = Red90,
    background = Color(0xFF131316),
    onBackground = Color(0xFFE4E1E6),
    surface = Color(0xFF131316),
    onSurface = Color(0xFFE4E1E6),
    surfaceVariant = Color(0xFF46464F),
    onSurfaceVariant = Color(0xFFC7C5D0),
    surfaceContainer = Color(0xFF1F1F23),
    surfaceContainerHigh = Color(0xFF2A2A2D),
    outline = Color(0xFF90909A),
    outlineVariant = Color(0xFF46464F),
)
