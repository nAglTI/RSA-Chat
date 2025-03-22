package com.hypergonial.chat.view

interface ModifierStates {
    /** If true, the shift key is currently held down. */
    val isShiftHeld: Boolean

    /** If true, the control key is currently held down. */
    val isCtrlHeld: Boolean

    /** If true, the alt key is currently held down. */
    val isAltHeld: Boolean

    /** If true, the meta (Windows/Command/Super) key is currently held down. */
    val isMetaHeld: Boolean
}

object NoopModifierStates : ModifierStates {
    override val isShiftHeld: Boolean = false

    override val isCtrlHeld: Boolean = false

    override val isAltHeld: Boolean = false

    override val isMetaHeld: Boolean = false
}

/** The current state of the different modifier keys. */
expect val modifierStates: ModifierStates
