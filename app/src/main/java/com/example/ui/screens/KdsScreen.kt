package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.KotEntity
import com.example.data.entity.KotPriority
import com.example.data.entity.KotStatus
import com.example.ui.theme.*
import com.example.ui.viewmodel.KdsUiState
import com.example.ui.viewmodel.KdsViewModel

@Composable
fun KdsScreen(
    kdsViewModel: KdsViewModel,
    modifier: Modifier = Modifier
) {
    val state by kdsViewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // KDS Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SoupKitchen, contentDescription = null, tint = AmberPrimary, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Kitchen Display System (KDS)",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${state.activeKots.size} Live Kitchen Orders",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3-Column Kanban Board for Kitchen
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. New Orders Column
            KdsKanbanColumn(
                title = "New Orders",
                count = state.newKots.size,
                columnColor = CrimsonRed,
                kots = state.newKots,
                onAdvance = { kdsViewModel.advanceKotStatus(it) },
                advanceLabel = "Start Cooking",
                modifier = Modifier.weight(1f)
            )

            // 2. Preparing Column
            KdsKanbanColumn(
                title = "In Preparation",
                count = state.preparingKots.size,
                columnColor = AmberPrimary,
                kots = state.preparingKots,
                onAdvance = { kdsViewModel.advanceKotStatus(it) },
                advanceLabel = "Mark Ready",
                modifier = Modifier.weight(1f)
            )

            // 3. Ready Column
            KdsKanbanColumn(
                title = "Ready for Serving",
                count = state.readyKots.size,
                columnColor = EmeraldGreen,
                kots = state.readyKots,
                onAdvance = { kdsViewModel.advanceKotStatus(it) },
                advanceLabel = "Mark Served",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun KdsKanbanColumn(
    title: String,
    count: Int,
    columnColor: Color,
    kots: List<KotEntity>,
    onAdvance: (KotEntity) -> Unit,
    advanceLabel: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Column Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = columnColor,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(10.dp))

            if (kots.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No orders in queue",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(kots, key = { it.id }) { kot ->
                        KotCard(
                            kot = kot,
                            onAdvance = { onAdvance(kot) },
                            advanceLabel = advanceLabel,
                            columnColor = columnColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KotCard(
    kot: KotEntity,
    onAdvance: () -> Unit,
    advanceLabel: String,
    columnColor: Color
) {
    val elapsedMinutes = remember(kot.createdAt) {
        ((System.currentTimeMillis() - kot.createdAt) / (60 * 1000)).coerceAtLeast(1)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("kot_card_${kot.kotNumber}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = kot.kotNumber,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = if (elapsedMinutes > 15) CrimsonRed.copy(alpha = 0.15f) else AmberLight.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            tint = if (elapsedMinutes > 15) CrimsonRed else AmberDark,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${elapsedMinutes}m ago",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = if (elapsedMinutes > 15) CrimsonRed else AmberDark
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "Table: ${kot.tableName} (${kot.orderType.name})",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = kot.itemsSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (kot.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Note: ${kot.notes}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = CrimsonRed
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onAdvance,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = columnColor)
            ) {
                Text(advanceLabel, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
