package com.hypergonial.chat

class JVMPlatform: Platform {
    override val platformType: PlatformType = PlatformType.JVM
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual val platform: Platform = JVMPlatform()
