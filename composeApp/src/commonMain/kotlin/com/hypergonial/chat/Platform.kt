package com.hypergonial.chat

enum class PlatformType {
    JVM,
    WEB,
    ANDROID,
    IOS,
    UNKNOWN;

    fun needsBackButton(): Boolean {
        return this != ANDROID
    }

    fun needsToSuspendClient(): Boolean {
        return this != JVM
    }
}

interface Platform {
    val platformType: PlatformType
    val name: String
}

expect val platform: Platform
