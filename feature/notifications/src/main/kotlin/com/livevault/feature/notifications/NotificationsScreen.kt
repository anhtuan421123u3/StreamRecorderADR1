package com.livevault.feature.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.livevault.core.network.dto.NotificationDto
import com.livevault.core.ui.components.EmptyStateView
import com.livevault.core.ui.components.LiveVaultTopAppBar
import com.livevault.core.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (recordingId: String) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            LiveVaultTopAppBar(
                title = "Notifications",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                actions = {
                    if (state.notifications.any { !it.read }) {
                        TextButton(onClick = viewModel::markAllAllRead) {
                            Text("Mark all read", color = IndigoSecondary)
                        }
                    }
                }
            )
        },
        containerColor = DarkBg
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.loadNotifications(isRefresh = true) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (state.notifications.isEmpty() && !state.isLoading) {
                EmptyStateView(
                    modifier = Modifier.fillMaxSize(),
                    icon = Icons.Default.Notifications,
                    title = "No Notifications",
                    description = "You'll receive alerts when your monitored channels go live and when recordings are ready to view."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.notifications, key = { it.id }) { notification ->
                        NotificationItemRow(
                            notification = notification,
                            onClick = {
                                viewModel.markAsRead(notification.id)
                                notification.recordingId?.let { onNavigateToPlayer(it) }
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun NotificationsViewModel.markAllAllRead() = this.markAllAsRead()

@Composable
fun NotificationItemRow(
    notification: NotificationDto,
    onClick: () -> Unit
) {
    val iconColor = when (notification.type) {
        "RECORDING_STARTED" -> PrimaryRed
        "RECORDING_FINISHED" -> EmeraldSuccess
        "RECORDING_FAILED" -> AmberWarning
        else -> IndigoPrimary
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = if (notification.read) DarkSurface else DarkSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (notification.read) DarkSurfaceBorder else IndigoPrimary.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (notification.type) {
                        "RECORDING_FINISHED" -> Icons.Default.CheckCircle
                        else -> Icons.Default.Videocam
                    },
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = if (notification.read) FontWeight.Normal else FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }

            if (!notification.read) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(PrimaryRed)
                        .align(Alignment.CenterVertically)
                )
            }
        }
    }
}
