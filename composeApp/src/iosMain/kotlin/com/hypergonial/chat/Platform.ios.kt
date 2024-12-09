package com.hypergonial.chat

import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val platformType: PlatformType = PlatformType.IOS
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual val platform: Platform =  IOSPlatform()
