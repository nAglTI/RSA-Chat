package com.hypergonial.chat

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
