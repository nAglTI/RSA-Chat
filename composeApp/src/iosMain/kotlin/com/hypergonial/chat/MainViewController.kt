package com.hypergonial.chat

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.ComposeUIViewController

private val lightColorScheme = lightColorScheme(
    primary = Color(0xFF476810),
    onPrimary = Color(0xFF476810),
    primaryContainer = Color(0xFFC7F089),
    onPrimaryContainer = Color(0xFFC7F089),
)
private val darkColorScheme = darkColorScheme(
    primary = Color(0xFFACD370),
    onPrimary = Color(0xFF213600),
    primaryContainer = Color(0xFF324F00),
    onPrimaryContainer = Color(0xFF324F00),
)

/// Adaptive theming depending on system theme.
@Composable
fun AppTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit
) {

    val colorScheme = when {
        useDarkTheme -> darkColorScheme
        else -> lightColorScheme
    }

    CompositionLocalProvider(LocalUsingDarkTheme provides useDarkTheme) {
        MaterialTheme(
            colorScheme = colorScheme, content = content
        )
    }
}

@Suppress("FunctionNaming")
fun MainViewController() = ComposeUIViewController { AppTheme {App() } }
