package com.hypergonial.chat.view.colors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.hypergonial.chat.PlatformType
import com.hypergonial.chat.platform

interface ColorProvider {
    fun getAccentColorOfOS(): Color?
}

/** The accent color provider for the current platform. */
val colorProvider: ColorProvider = when (platform.platformType) {
    PlatformType.LINUX -> LinuxColorProvider
    PlatformType.MAC -> MacOSColorProvider
    PlatformType.WINDOWS -> WindowsColorProvider
    else -> object : ColorProvider {
        override fun getAccentColorOfOS(): Color? = null
    }
}
