package com.hypergonial.chat.view.colors

import androidx.compose.ui.graphics.Color
import co.touchlab.kermit.Logger
import com.hypergonial.chat.PlatformType
import com.hypergonial.chat.platform
import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg

object WindowsColorProvider : ColorProvider {

    @Suppress("TooGenericExceptionCaught")
    override fun getAccentColorOfOS(): Color? {
        if (platform.platformType != PlatformType.WINDOWS) {
            return null
        }

        return try {
            val registryKey = "Software\\Microsoft\\Windows\\DWM"
            val valueName = "AccentColor"
            if (!Advapi32Util.registryKeyExists(WinReg.HKEY_CURRENT_USER, registryKey)) {
                return null
            }
            val accentColor = Advapi32Util.registryGetIntValue(WinReg.HKEY_CURRENT_USER, registryKey, valueName)
            // The value is stored as ABGR (alpha in highest 8 bits)
            val a = (accentColor shr 24) and 0xff
            val b = (accentColor shr 16) and 0xff
            val g = (accentColor shr 8) and 0xff
            val r = accentColor and 0xff
            Color(r / 255f, g / 255f, b / 255f, a / 255f)
        } catch (e: Exception) {
            Logger.w { "Failed to get accent color from Windows registry: $e" }
            null
        }
    }
}
