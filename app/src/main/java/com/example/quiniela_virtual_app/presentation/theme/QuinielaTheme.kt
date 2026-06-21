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

private val Teal20   = Color(0xFF003D36)
private val Teal30   = Color(0xFF005048)
private val Teal40   = Color(0xFF00695C)
private val Teal80   = Color(0xFF80CBC4)
private val Teal90   = Color(0xFFE0F2F1)

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

    secondary            = Teal40,
    onSecondary          = Color.White,
    secondaryContainer   = Teal90,
    onSecondaryContainer = Teal20,

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

    secondary            = Teal80,
    onSecondary          = Teal20,
    secondaryContainer   = Teal30,
    onSecondaryContainer = Teal90,

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
