package com.hypergonial.chat.model

import com.hypergonial.chat.model.payloads.User

interface CacheAware {
    val cache: Cache
}

class Cache {
    var ownUser: User? = null
}
