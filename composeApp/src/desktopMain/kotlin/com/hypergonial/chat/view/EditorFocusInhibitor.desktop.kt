package com.hypergonial.chat.view

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@OptIn(DelicateCoroutinesApi::class)
actual val editorFocusInhibitor: EditorFocusInhibitor by lazy {
    val inhibitor = DefaultEditorFocusInhibitor()
    GlobalScope.launch { inhibitor.run() }
    inhibitor
}
