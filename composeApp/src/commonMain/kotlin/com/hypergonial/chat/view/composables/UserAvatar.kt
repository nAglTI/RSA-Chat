package com.hypergonial.chat.view.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import chat.composeapp.generated.resources.Res
import chat.composeapp.generated.resources.avatar_placeholder
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.hypergonial.chat.LocalUsingDarkTheme
import org.jetbrains.compose.resources.painterResource

@Composable
fun UserAvatar(avatarUrl: String? = null, displayName: String, size: Dp = 40.dp) {
    val isDarkTheme = LocalUsingDarkTheme.current
    val imageModifier =
        Modifier.padding(vertical = 6.dp, horizontal = 14.dp)
            .clip(CircleShape)
            .height(size)
            .width(size)

    if (avatarUrl == null) {
        Image(
            painter = painterResource(Res.drawable.avatar_placeholder),
            contentDescription = "Avatar of $displayName",
            modifier = imageModifier,
            colorFilter = if (isDarkTheme) ColorFilter.tint(Color.White) else null,
        )
    } else {
        AsyncImage(
            model =
            ImageRequest.Builder(LocalPlatformContext.current)
                .data(avatarUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Avatar of $displayName",
            contentScale = ContentScale.Crop,
            modifier = imageModifier,
        )
    }
}
