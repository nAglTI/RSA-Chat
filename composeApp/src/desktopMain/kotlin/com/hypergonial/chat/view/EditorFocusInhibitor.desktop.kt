package com.hypergonial.chat.view

import kotlinx.coroutines.DelicateCoroutinesApi

@OptIn(DelicateCoroutinesApi::class)
actual val editorFocusInhibitor: EditorFocusInhibitor = DefaultEditorFocusInhibitor()
