package com.hypergonial.chat

object WasmPlatform: Platform {
    override val platformType: PlatformType = PlatformType.WEB
    override val name: String = "Web with Kotlin/Wasm"
}

actual val platform: Platform = WasmPlatform
