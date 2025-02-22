package com.hypergonial.chat.view.composables

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun UploadStateCard(progress: Float) {
    val progressBarState by
        animateFloatAsState(progress, animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec)

    Card(Modifier.widthIn(max = 200.dp), elevation = CardDefaults.elevatedCardElevation()) {
        Column {
            Text("Uploading files... (${(progress*100).roundToInt()}%)")
            LinearProgressIndicator(progress = { progressBarState }, Modifier.fillMaxWidth())
        }
    }
}
