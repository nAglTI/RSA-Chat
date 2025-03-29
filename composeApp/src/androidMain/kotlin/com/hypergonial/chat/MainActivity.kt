package com.hypergonial.chat

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import co.touchlab.kermit.Logger
import com.arkivanov.decompose.defaultComponentContext
import com.hypergonial.chat.model.AndroidSettings
import com.hypergonial.chat.model.settings
import com.hypergonial.chat.view.components.DefaultRootComponent
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.mmk.kmpnotifier.extensions.onCreateOrOnNewIntent
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import com.mmk.kmpnotifier.permission.permissionUtil
import io.github.vinceglb.filekit.core.FileKit

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FileKit.init(this)
        enableEdgeToEdge()
        initializeStorage()

        NotifierManager.initialize(
            configuration =
                NotificationPlatformConfiguration.Android(
                    notificationIconResId = R.drawable.ic_stat_chat_icon,
                    showPushNotification = false,
                )
        )
        NotifierManager.setLogger {
            Logger.withTag("NotifierManager").i(it)
        }

        val permissionUtil by permissionUtil()
        permissionUtil.askNotificationPermission()

        val root = DefaultRootComponent(defaultComponentContext())

        // Only resolve push notif data after the root component exists
        NotifierManager.onCreateOrOnNewIntent(intent)

        ContextHelper.retrieveAppContext = { this.applicationContext }

        setContent { AppTheme { App(root) } }
    }

    override fun onPause() {
        super.onPause()
        ContextHelper.retrieveAppContext = { null }
    }

    override fun onResume() {
        super.onResume()
        ContextHelper.retrieveAppContext = { this.applicationContext }
        initializeStorage()
    }

    override fun onDestroy() {
        super.onDestroy()
        ContextHelper.retrieveAppContext = { null }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        NotifierManager.onCreateOrOnNewIntent(intent)
    }

    private fun initializeStorage() {
        if (settings !is AndroidSettings) {
            return
        }

        settings.initialize(getSharedPreferences("settings", MODE_PRIVATE))
    }
}

/// Adaptive theming depending on system theme.
@Composable
fun AppTheme(useDarkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    // Material You is only supported on Android 12+
    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme =
        when {
            supportsDynamicColor && useDarkTheme -> dynamicDarkColorScheme(LocalContext.current)
            supportsDynamicColor && !useDarkTheme -> dynamicLightColorScheme(LocalContext.current)
            else ->
                dynamicColorScheme(
                    seedColor = Color(104, 165, 39),
                    isDark = useDarkTheme,
                    isAmoled = false,
                    style = PaletteStyle.TonalSpot,
                )
        }

    CompositionLocalProvider(LocalUsingDarkTheme provides useDarkTheme) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
