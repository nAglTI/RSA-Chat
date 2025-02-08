package com.hypergonial.chat

import android.content.Context

object ContextHelper {
    var retrieveAppContext: () -> Context? = { null }
}
