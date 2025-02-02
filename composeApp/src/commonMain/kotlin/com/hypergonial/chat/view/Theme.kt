package com.hypergonial.chat.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.hypergonial.chat.LocalUsingDarkTheme

/**
 * A custom color scheme for the chat theme.
 *
 * @param success The color to be used for success messages.
 * @param onSuccess The color to be used for text on success messages.
 * @param warn The color to be used for warning messages.
 * @param onWarn The color to be used for text on warning messages.
 * @param misc The color to be used for miscellaneous messages.
 * @param onMisc The color to be used for text on miscellaneous messages.
 */
data class ChatColorScheme(
    val success: Color,
    val onSuccess: Color,
    val warn: Color,
    val onWarn: Color,
    val misc: Color,
    val onMisc: Color,
) {
    companion object {
        fun darkColors(): ChatColorScheme {
            return ChatColorScheme(
                success = Color(160, 213, 123),
                onSuccess = Color(22, 56, 0),
                warn = Color(232, 161, 61),
                onWarn = Color(60, 47, 0),
                misc = Color(149, 204, 255),
                onMisc = Color(0, 51, 83),
            )
        }

        fun lightColors(): ChatColorScheme {
            return ChatColorScheme(
                success = Color(59, 106, 28),
                onSuccess = Color(252, 252, 252),
                warn = Color(232, 161, 61),
                onWarn = Color(60, 47, 0),
                misc = Color(33, 99, 146),
                onMisc = Color(252, 252, 252),
            )
        }
    }
}

/** Inject custom palette to call site. */
object ChatTheme {
    val colorScheme: ChatColorScheme
        @Composable get() = LocalChatColorScheme.current
}

val LocalChatColorScheme = compositionLocalOf<ChatColorScheme> { error("No color scheme provided for ChatTheme.") }

/**
 * Apply custom palette to the content that supports it.
 *
 * @param content The content to be wrapped in the theme.
 *
 * Note: [LocalUsingDarkTheme] is used to determine if the theme should be dark or light. This should be queried on the
 * respective platform then passed into the composition.
 *
 * @see ChatTheme
 */
@Composable
fun ChatTheme(content: @Composable () -> Unit) {
    val isDark = LocalUsingDarkTheme.current

    val colorScheme = if (isDark) ChatColorScheme.darkColors() else ChatColorScheme.lightColors()

    CompositionLocalProvider(LocalChatColorScheme provides colorScheme) { content() }
}
