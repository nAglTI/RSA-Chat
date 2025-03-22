package com.hypergonial.chat.view

import java.util.concurrent.atomic.AtomicBoolean

internal object DesktopModifierStates : ModifierStates {
    private val _isShiftHeld = AtomicBoolean(false)
    private val _isCtrlHeld = AtomicBoolean(false)
    private val _isAltHeld = AtomicBoolean(false)
    private val _isMetaHeld = AtomicBoolean(false)

    override var isShiftHeld
        get() = _isShiftHeld.get()
        set(value) {
            _isShiftHeld.set(value)
        }

    override var isCtrlHeld
        get() = _isCtrlHeld.get()
        set(value) {
            _isCtrlHeld.set(value)
        }

    override var isAltHeld
        get() = _isAltHeld.get()
        set(value) {
            _isAltHeld.set(value)
        }

    override var isMetaHeld
        get() = _isMetaHeld.get()
        set(value) {
            _isMetaHeld.set(value)
        }
}

actual val modifierStates: ModifierStates = DesktopModifierStates
