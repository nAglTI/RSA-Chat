package com.hypergonial.chat

import org.apache.commons.lang3.SystemUtils

object JVMPlatform : Platform {
    override val platformType: PlatformType = resolveCurrentOsType()
    override val name: String = "Java ${System.getProperty("java.version")}"
}

fun resolveCurrentOsType(): PlatformType {
    return when {
        SystemUtils.IS_OS_WINDOWS -> PlatformType.WINDOWS
        SystemUtils.IS_OS_MAC -> PlatformType.MAC
        SystemUtils.IS_OS_LINUX -> PlatformType.LINUX
        else -> PlatformType.UNKNOWN_JVM
    }
}

actual val platform: Platform = JVMPlatform
