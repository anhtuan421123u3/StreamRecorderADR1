package com.livevault.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
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
import com.livevault.core.database.entity.DownloadEntity
import com.livevault.core.database.entity.DownloadStatus
import com.livevault.core.database.entity.RecordingEntity
import com.livevault.core.ui.components.EmptyStateView
import com.livevault.core.ui.components.LiveVaultTopAppBar
import com.livevault.core.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateToPlayer: (recordingId: String) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            LiveVaultTopAppBar(title = "Video Library")
        },
        containerColor = DarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Selector
            TabRow(
                selectedTabIndex = state.selectedTab.ordinal,
                containerColor = DarkBg,
                contentColor = TextPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[state.selectedTab.ordinal]),
                        color = PrimaryRed
                    )
                }
            ) {
                Tab(
                    selected = state.selectedTab == LibraryTab.ALL,
                    onClick = { viewModel.selectTab(LibraryTab.ALL) },
                    text = { Text("All Recordings", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = state.selectedTab == LibraryTab.DOWNLOADED,
                    onClick = { viewModel.selectTab(LibraryTab.DOWNLOADED) },
                    text = { Text("Downloaded", fontWeight = FontWeight.SemiBold) }
                )
            }

            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                if (state.recordings.isEmpty()) {
                    val isDownloadedTab = state.selectedTab == LibraryTab.DOWNLOADED
                    EmptyStateView(
                        modifier = Modifier.fillMaxSize(),
                        icon = Icons.Default.VideoLibrary,
                        title = if (isDownloadedTab) "No Downloaded Videos" else "Vault is Empty",
                        description = if (isDownloadedTab)
                            "Download your recorded streams to watch offline without consuming mobile data."
                        else "No recorded streams yet. Any recorded livestreams will appear in your cloud vault here."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.recordings, key = { it.id }) { recording ->
                            val download = state.downloads[recording.id]
                            LibraryRecordingCard(
                                recording = recording,
                                download = download,
                                onPlay = { onNavigateToPlayer(recording.id) },
                                onDownload = { viewModel.startDownload(recording) },
                                onDelete = { viewModel.deleteRecording(recording.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LibraryRecordingCard(
    recording: RecordingEntity,
    download: DownloadEntity?,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onPlay),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Thumbnail
                Box(
                    modifier = Modifier
                        .width(110.dp)
                        .height(68.dp)
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
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

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

                // Metadata
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
                        maxLines = 1
                    )

                    Text(
                        text = recording.sizeBytes.formatBytes(),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }

                // Actions: Download & Delete
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Download status or download trigger
                    when (download?.status) {
                        DownloadStatus.DOWNLOADING, DownloadStatus.PENDING -> {
                            Icon(
                                imageVector = Icons.Default.Downloading,
                                contentDescription = "Downloading",
                                tint = IndigoSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        DownloadStatus.COMPLETED -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Downloaded",
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        else -> {
                            IconButton(onClick = onDownload, modifier = Modifier.size(28.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download video",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Download progress bar
            if (download?.status == DownloadStatus.DOWNLOADING) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { download.progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = PrimaryRed,
                    trackColor = DarkBg
                )
            }
        }
    }
}
