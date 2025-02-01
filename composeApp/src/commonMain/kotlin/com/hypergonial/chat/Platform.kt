package com.hypergonial.chat

import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.animation.StackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.predictiveBackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.scale
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.essenty.backhandler.BackHandler
import com.hypergonial.chat.view.iOSBackAnimation

enum class PlatformType {
    JVM,
    WEB,
    ANDROID,
    IOS,
    UNKNOWN;


}

interface Platform {
    val platformType: PlatformType
    val name: String

    fun needsBackButton(): Boolean {
        return platformType != PlatformType.ANDROID
    }

    fun needsToSuspendClient(): Boolean {
        return platformType != PlatformType.JVM
    }

    /**
     * Returns the appropriate back animation for the platform
     */
    @OptIn(ExperimentalDecomposeApi::class)
    fun <C : Any, T : Any> backAnimation(
        backHandler: BackHandler,
        onBack: () -> Unit,
    ): StackAnimation<C, T> {
        return if (platformType == PlatformType.IOS) {
            iOSBackAnimation(backHandler, onBack = onBack)
        } else {
            predictiveBackAnimation(
                backHandler,
                onBack = onBack,
                fallbackAnimation = stackAnimation(scale() + fade())
            )
        }
    }
}

expect val platform: Platform
