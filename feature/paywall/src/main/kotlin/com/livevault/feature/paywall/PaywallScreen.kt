package com.livevault.feature.paywall

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.livevault.core.ui.components.LiveVaultTopAppBar
import com.livevault.core.ui.theme.*

@Composable
fun PaywallScreen(
    onNavigateBack: () -> Unit,
    viewModel: PaywallViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is PaywallEvent.PurchaseSuccess -> onNavigateBack()
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            LiveVaultTopAppBar(
                title = "LiveVault Pro",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = DarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = IndigoSecondary,
                modifier = Modifier.size(54.dp)
            )

            Text(
                text = "Supercharge Your Cloud Recording",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Capture every live moment in pristine 1080p/4K with rapid stream detection.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            // Tier Option Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TierCard(
                    modifier = Modifier.weight(1f),
                    name = "PRO",
                    price = "$9.99 / mo",
                    description = "10 Channels, 1080p, 2m polling",
                    isSelected = state.selectedTier == "PRO",
                    onClick = { viewModel.selectTier("PRO") }
                )

                TierCard(
                    modifier = Modifier.weight(1f),
                    name = "PREMIUM",
                    price = "$19.99 / mo",
                    description = "Unlimited, 4K, 1m polling",
                    isSelected = state.selectedTier == "PREMIUM",
                    onClick = { viewModel.selectTier("PREMIUM") },
                    badge = "BEST VALUE"
                )
            }

            // Feature Comparison List
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkSurface,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    FeatureCheckRow("Unlimited background stream downloads")
                    FeatureCheckRow(if (state.selectedTier == "PREMIUM") "4K Ultra HD recording quality" else "1080p Full HD recording quality")
                    FeatureCheckRow(if (state.selectedTier == "PREMIUM") "1-minute instant live polling interval" else "2-minute live polling interval")
                    FeatureCheckRow(if (state.selectedTier == "PREMIUM") "90 days cloud retention" else "30 days cloud retention")
                    FeatureCheckRow("Priority recording workers & BullMQ queues")
                }
            }

            // Error
            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Subscribe Button
            Button(
                onClick = { activity?.let { viewModel.purchase(it) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                enabled = !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = "Upgrade to ${state.selectedTier}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            TextButton(onClick = viewModel::restorePurchases) {
                Text("Restore Purchases", color = TextSecondary, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun TierCard(
    modifier: Modifier = Modifier,
    name: String,
    price: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    badge: String? = null
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) IndigoPrimary.copy(alpha = 0.2f) else DarkSurface,
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) IndigoPrimary else DarkSurfaceBorder
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (badge != null) {
                Surface(
                    color = PrimaryRed,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = badge,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            Text(text = name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(text = price, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = IndigoSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun FeatureCheckRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = EmeraldSuccess,
            modifier = Modifier.size(18.dp)
        )
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
    }
}
