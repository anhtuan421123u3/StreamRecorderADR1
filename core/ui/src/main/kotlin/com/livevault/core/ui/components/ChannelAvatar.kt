package com.livevault.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.livevault.core.ui.theme.*

@Composable
fun ChannelAvatar(
    modifier: Modifier = Modifier,
    avatarUrl: String?,
    displayName: String,
    platform: String,
    size: Dp = 48.dp
) {
    val platformColor = when (platform.uppercase()) {
        "YOUTUBE" -> PlatformYouTube
        "TWITCH" -> PlatformTwitch
        "KICK" -> PlatformKick
        "TIKTOK" -> PlatformTikTok
        else -> PlatformCustom
    }

    val platformInitial = when (platform.uppercase()) {
        "YOUTUBE" -> "YT"
        "TWITCH" -> "TW"
        "KICK" -> "KC"
        "TIKTOK" -> "TT"
        else -> "RTMP"
    }

    Box(modifier = modifier.size(size)) {
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayName.take(2).uppercase(),
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.35f).sp
                )
            }
        }

        // Platform mini badge
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(size * 0.42f)
                .clip(CircleShape)
                .background(platformColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = platformInitial.take(1),
                color = if (platform.uppercase() == "KICK") Color.Black else Color.White,
                fontSize = (size.value * 0.22f).sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}
