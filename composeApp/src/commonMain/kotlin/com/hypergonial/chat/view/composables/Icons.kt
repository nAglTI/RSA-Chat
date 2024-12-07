package com.hypergonial.chat.view.composables

import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import chat.composeapp.generated.resources.Res
import chat.composeapp.generated.resources.visibility
import chat.composeapp.generated.resources.visibility_off
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource


/**
 * A composable that displays the "Eye" icon.
 * */
@Composable
fun VisibilityIcon() {
    Icon(painterResource(Res.drawable.visibility), contentDescription = "Visible")
}

/**
 * A composable that displays the "Eye Crossed Out" icon.
 * */
@Composable
fun VisibilityOffIcon() {
    Icon(painterResource(Res.drawable.visibility_off), contentDescription = "Not Visible")
}
