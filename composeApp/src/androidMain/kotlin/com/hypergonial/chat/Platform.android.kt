package com.hypergonial.chat

import android.os.Build

object AndroidPlatform : Platform {
    override val platformType: PlatformType = PlatformType.ANDROID
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual val platform: Platform = AndroidPlatform
