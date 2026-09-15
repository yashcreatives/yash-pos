package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.UserRole
import com.example.ui.theme.*
import com.example.ui.viewmodel.DashboardUiState
import com.example.ui.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    dashboardViewModel: DashboardViewModel,
    userRole: UserRole,
    onNavigateToPos: () -> Unit,
    onNavigateToTables: () -> Unit,
    onNavigateToKds: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by dashboardViewModel.uiState.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top KPI Cards Row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiStatCard(
                        title = "Today's Gross Sales",
                        value = "₹${String.format("%.2f", state.todayTotalSales)}",
                        subtitle = "${state.todayBillCount} Invoices Settled",
                        icon = Icons.Default.CurrencyRupee,
                        accentColor = AmberPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    KpiStatCard(
                        title = "Net Sales (After Exp)",
                        value = "₹${String.format("%.2f", state.netSales)}",
                        subtitle = "Expenses: ₹${String.format("%.2f", state.todayExpenses)}",
                        icon = Icons.Default.TrendingUp,
                        accentColor = EmeraldGreen,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiStatCard(
                        title = "Cash Sales",
                        value = "₹${String.format("%.0f", state.cashSales)}",
                        subtitle = "In Drawer",
                        icon = Icons.Default.AttachMoney,
                        accentColor = EmeraldGreen,
                        modifier = Modifier.weight(1f)
                    )
                    KpiStatCard(
                        title = "UPI / QR Sales",
                        value = "₹${String.format("%.0f", state.upiSales)}",
                        subtitle = "Direct Bank",
                        icon = Icons.Default.QrCode,
                        accentColor = SkyBlue,
                        modifier = Modifier.weight(1f)
                    )
                    KpiStatCard(
                        title = "Card / POS",
                        value = "₹${String.format("%.0f", state.cardSales)}",
                        subtitle = "Swipe Machine",
                        icon = Icons.Default.CreditCard,
                        accentColor = PurpleAccent,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Live Operations Quick Status
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QuickStatusItem(
                        title = "Active Dine-in Tables",
                        count = "${state.occupiedTablesCount} / ${state.totalTablesCount}",
                        badgeColor = if (state.occupiedTablesCount > 0) CrimsonRed else EmeraldGreen,
                        onClick = onNavigateToTables
                    )
                    Divider(modifier = Modifier.height(36.dp).width(1.dp))
                    QuickStatusItem(
                        title = "Kitchen KOTs in Queue",
                        count = "${state.pendingKotCount} Orders",
                        badgeColor = if (state.pendingKotCount > 0) AmberPrimary else EmeraldGreen,
                        onClick = onNavigateToKds
                    )
                    Divider(modifier = Modifier.height(36.dp).width(1.dp))
                    QuickStatusItem(
                        title = "Low Stock Alerts",
                        count = "${state.lowStockCount} Items",
                        badgeColor = if (state.lowStockCount > 0) CrimsonRed else EmeraldGreen,
                        onClick = {}
                    )
                }
            }
        }

        // Hourly Sales Bar Chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Hourly Sales Performance (Peak Hours)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Today",
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val maxAmount = state.hourlySales.maxOfOrNull { it.amount }?.coerceAtLeast(1000.0) ?: 1000.0

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val barWidth = size.width / (state.hourlySales.size * 1.5f)
                            val space = barWidth * 0.5f

                            state.hourlySales.forEachIndexed { index, point ->
                                val x = index * (barWidth + space) + space / 2
                                val barHeight = ((point.amount / maxAmount) * size.height * 0.85f).toFloat()
                                val y = size.height - barHeight

                                drawRoundRect(
                                    brush = Brush.verticalGradient(
                                        listOf(AmberLight, AmberPrimary)
                                    ),
                                    topLeft = Offset(x, y),
                                    size = Size(barWidth, barHeight.coerceAtLeast(4f)),
                                    cornerRadius = CornerRadius(6f, 6f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        state.hourlySales.forEach { pt ->
                            Text(
                                text = pt.hourLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Top Selling Dishes Leaderboard
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Top Selling Dishes & Revenue",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    state.topSellingItems.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = AmberPrimary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "#${index + 1}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = AmberPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(item.itemName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                    Text("${item.quantitySold} portions ordered", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Text(
                                text = "₹${String.format("%.0f", item.totalRevenue)}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = EmeraldGreen
                            )
                        }
                        if (index < state.topSellingItems.lastIndex) {
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KpiStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = accentColor
            )
        }
    }
}

@Composable
fun QuickStatusItem(
    title: String,
    count: String,
    badgeColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = count,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = badgeColor
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
