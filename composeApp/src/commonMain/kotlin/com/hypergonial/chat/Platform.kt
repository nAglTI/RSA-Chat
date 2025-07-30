package com.hypergonial.chat

import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.animation.*
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.predictiveBackAnimation
import com.arkivanov.essenty.backhandler.BackHandler
import com.hypergonial.chat.view.iOSBackAnimation

enum class PlatformType {
    WINDOWS,
    MAC,
    LINUX,
    UNKNOWN_JVM,
    ANDROID,
    IOS,
    WEB,
    UNKNOWN;

    fun isDesktop(): Boolean {
        return this == WINDOWS || this == MAC || this == LINUX || this == UNKNOWN_JVM
    }

    fun isDesktopOrWeb(): Boolean {
        return isDesktop() || this == WEB
    }

    fun isMobile(): Boolean {
        return this == ANDROID || this == IOS
    }
}

/**
 * Platform information for the app.
 *
 * Contains information about the platform the app is running on.
 */
interface Platform {
    val platformType: PlatformType
    val name: String

    /** Returns whether the platform needs a back button to be displayed. */
    fun needsBackButton(): Boolean {
        return platformType != PlatformType.ANDROID
    }

    /** Returns whether the platform needs to suspend the client when the app is paused. */
    fun needsToSuspendClient(): Boolean {
        return !platformType.isDesktop()
    }

    /** Returns whether the platform is a desktop or web platform. */
    fun isDesktopOrWeb(): Boolean {
        // TODO: Detect if web is running on mobile or not?
        return platformType.isDesktopOrWeb()
    }

    /** Returns whether the platform is a desktop platform. */
    fun isDesktop(): Boolean {
        return platformType.isDesktop()
    }

    /** Returns whether the platform is a mobile platform. */
    fun isMobile(): Boolean {
        return platformType.isMobile()
    }

    fun tapVerb(): String {
        return if (isMobile()) {
            "tap"
        } else {
            "click"
        }
    }

    /**
     * Returns the appropriate back animation for the platform
     *
     * @param backHandler The back handler to use for the back gesture.
     * @param onBack The callback to call when the back gesture is detected.
     * @return The appropriate back animation for the platform
     */
    @OptIn(ExperimentalDecomposeApi::class)
    fun <C : Any, T : Any> backAnimation(backHandler: BackHandler, onBack: () -> Unit): StackAnimation<C, T> {
        return if (platformType == PlatformType.IOS) {
            iOSBackAnimation(backHandler, onBack = onBack)
        } else {
            predictiveBackAnimation(backHandler, onBack = onBack, fallbackAnimation = stackAnimation(scale() + fade()))
        }
    }
}

/** Information about the current platform. */
expect val platform: Platform
