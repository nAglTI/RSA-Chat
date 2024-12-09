package com.hypergonial.chat

class WasmPlatform: Platform {
    override val platformType: PlatformType = PlatformType.WEB
    override val name: String = "Web with Kotlin/Wasm"
}

actual val platform: Platform = WasmPlatform()
