package com.livevault.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.livevault.core.common.formatBytes
import com.livevault.core.common.formatDuration
import com.livevault.core.network.dto.ChannelDto
import com.livevault.core.network.dto.RecordingItemDto
import com.livevault.core.ui.components.*
import com.livevault.core.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToAddChannel: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToPlayer: (recordingId: String) -> Unit,
    onNavigateToChannelDetail: (channelId: String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "LiveVault",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                        if (state.liveChannels.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            LiveBadge(text = "${state.liveChannels.size} LIVE")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToNotifications) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBg,
                    scrolledContainerColor = DarkBg
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddChannel,
                containerColor = PrimaryRed,
                contentColor = TextPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Channel")
            }
        },
        containerColor = DarkBg
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (state.isLoading && !state.isRefreshing) {
                // Shimmer Loading Skeletons
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ShimmerCardPlaceholder(height = 60.dp)
                    ShimmerCardPlaceholder(height = 140.dp)
                    ShimmerCardPlaceholder(height = 140.dp)
                }
            } else if (state.channels.isEmpty() && state.recentRecordings.isEmpty()) {
                EmptyStateView(
                    modifier = Modifier.fillMaxSize(),
                    icon = Icons.Default.Videocam,
                    title = "No Channels Monitored",
                    description = "Add your owned YouTube or RTMP streams to automatically record every live session 24/7.",
                    actionButtonText = "Add Channel",
                    onActionClick = onNavigateToAddChannel
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Active Recording Banner
                    if (state.activeRecordingsCount > 0) {
                        item {
                            ActiveRecordingBanner(activeCount = state.activeRecordingsCount)
                        }
                    }

                    // Channels Row
                    item {
                        ChannelsHeader(
                            channelCount = state.channels.size,
                            onAddChannel = onNavigateToAddChannel
                        )
                    }

                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.channels, key = { it.id }) { channel ->
                                ChannelCard(
                                    channel = channel,
                                    onClick = { onNavigateToChannelDetail(channel.id) }
                                )
                            }
                        }
                    }

                    // Recent Recordings
                    item {
                        Text(
                            text = "Recent Recordings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }

                    if (state.recentRecordings.isEmpty()) {
                        item {
                            EmptyStateView(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                icon = Icons.Default.PlayArrow,
                                title = "No Recordings Yet",
                                description = "When your monitored channels go live, LiveVault will automatically record and store videos here."
                            )
                        }
                    } else {
                        items(state.recentRecordings, key = { it.id }) { recording ->
                            RecordingCard(
                                recording = recording,
                                onClick = { onNavigateToPlayer(recording.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveRecordingBanner(activeCount: Int) {
    Surface(
        color = PrimaryRedDark.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryRed),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LiveBadge(text = "REC")
            Text(
                text = "$activeCount livestream recording session(s) in progress",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun ChannelsHeader(channelCount: Int, onAddChannel: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Monitored Channels ($channelCount)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        TextButton(onClick = onAddChannel) {
            Text("+ Add", color = IndigoSecondary, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ChannelCard(channel: ChannelDto, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.BottomCenter) {
            ChannelAvatar(
                avatarUrl = channel.avatarUrl,
                displayName = channel.displayName,
                platform = channel.platform,
                size = 56.dp
            )
            if (channel.isLive) {
                Box(modifier = Modifier.offset(y = 6.dp)) {
                    LiveBadge()
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = channel.displayName,
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun RecordingCard(
    recording: RecordingItemDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceVariant)
            ) {
                if (!recording.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = recording.thumbnailUrl,
                        contentDescription = recording.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Duration badge
                if (recording.durationSeconds > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.8f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = recording.durationSeconds.formatDuration(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = recording.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = recording.channelName,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = recording.sizeBytes.formatBytes(),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )

                    if (recording.status == "RECORDING") {
                        LiveBadge(text = "RECORDING")
                    }
                }
            }
        }
    }
}
