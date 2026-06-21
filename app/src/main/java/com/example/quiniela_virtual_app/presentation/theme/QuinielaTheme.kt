package com.example.quiniela_virtual_app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Paleta base ──────────────────────────────────────────────────────────────

private val Verde20  = Color(0xFF00391A)
private val Verde30  = Color(0xFF005229)
private val Verde40  = Color(0xFF1B6B3A)
private val Verde80  = Color(0xFF8CD9A3)
private val Verde90  = Color(0xFFA8F5BE)

private val Naranja20 = Color(0xFF4E1500)
private val Naranja30 = Color(0xFF772100)
private val Naranja40 = Color(0xFFD84315)
private val Naranja80 = Color(0xFFFF8A65)
private val Naranja90 = Color(0xFFFBE9E7)

private val Azul20   = Color(0xFF00306A)
private val Azul30   = Color(0xFF004793)
private val Azul40   = Color(0xFF1565C0)
private val Azul80   = Color(0xFFA6C8FF)
private val Azul90   = Color(0xFFD4E4FF)

private val Rojo10   = Color(0xFF410002)
private val Rojo40   = Color(0xFFBA1A1A)
private val Rojo80   = Color(0xFFFFB4AB)
private val Rojo90   = Color(0xFFFFDAD6)
private val Rojo20   = Color(0xFF690005)
private val Rojo30   = Color(0xFF93000A)

// ── Esquemas de color ────────────────────────────────────────────────────────

private val LightColors = lightColorScheme(
    primary              = Verde40,
    onPrimary            = Color.White,
    primaryContainer     = Verde90,
    onPrimaryContainer   = Verde20,

    secondary            = Naranja40,
    onSecondary          = Color.White,
    secondaryContainer   = Naranja90,
    onSecondaryContainer = Naranja20,

    tertiary             = Azul40,
    onTertiary           = Color.White,
    tertiaryContainer    = Azul90,
    onTertiaryContainer  = Azul20,

    error                = Rojo40,
    onError              = Color.White,
    errorContainer       = Rojo90,
    onErrorContainer     = Rojo10,
)

private val DarkColors = darkColorScheme(
    primary              = Verde80,
    onPrimary            = Verde20,
    primaryContainer     = Verde30,
    onPrimaryContainer   = Verde90,

    secondary            = Naranja80,
    onSecondary          = Naranja20,
    secondaryContainer   = Naranja30,
    onSecondaryContainer = Naranja90,

    tertiary             = Azul80,
    onTertiary           = Azul20,
    tertiaryContainer    = Azul30,
    onTertiaryContainer  = Azul90,

    error                = Rojo80,
    onError              = Rojo20,
    errorContainer       = Rojo30,
    onErrorContainer     = Rojo90,
)

// ── Tema ─────────────────────────────────────────────────────────────────────

@Composable
fun QuinielaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
