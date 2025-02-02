package com.hypergonial.chat

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.defaultComponentContext
import com.hypergonial.chat.model.AndroidSettings
import com.hypergonial.chat.model.settings
import com.hypergonial.chat.view.components.DefaultRootComponent
import io.github.vinceglb.filekit.core.FileKit

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalDecomposeApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FileKit.init(this)
        enableEdgeToEdge()
        initializeStorage()
        val root = DefaultRootComponent(defaultComponentContext())

        setContent {
            AppTheme {
                App(root)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        initializeStorage()
    }

    private fun initializeStorage() {
        if (settings !is AndroidSettings) {
            return
        }

        settings.initialize(getSharedPreferences("settings", MODE_PRIVATE))
    }
}

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
    // Material You is only supported on Android 12+
    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = when {
        supportsDynamicColor && useDarkTheme -> dynamicDarkColorScheme(LocalContext.current)
        supportsDynamicColor && !useDarkTheme -> dynamicLightColorScheme(LocalContext.current)
        useDarkTheme -> darkColorScheme
        else -> lightColorScheme
    }

    CompositionLocalProvider(LocalUsingDarkTheme provides useDarkTheme) {
        MaterialTheme(
            colorScheme = colorScheme, content = content
        )
    }
}
