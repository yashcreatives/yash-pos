package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.PaymentDistribution
import com.example.ui.viewmodel.ReportDateFilter
import com.example.ui.viewmodel.ReportsUiState
import com.example.ui.viewmodel.ReportsViewModel
import kotlinx.coroutines.launch

@Composable
fun ReportsScreen(
    reportsViewModel: ReportsViewModel,
    modifier: Modifier = Modifier
) {
    val state by reportsViewModel.uiState.collectAsState()
    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Export Dialog
    if (state.showExportDialog) {
        AlertDialog(
            onDismissRequest = { reportsViewModel.dismissExport() },
            title = { Text(state.exportTitle, fontWeight = FontWeight.Bold) },
            text = {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)
                ) {
                    LazyColumn(modifier = Modifier.padding(10.dp)) {
                        item {
                            Text(
                                text = state.exportContent,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { reportsViewModel.dismissExport() }) { Text("Close") }
            }
        )
    }

    LazyColumn(
        state = scrollState,
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Filter Bar & Export Actions
        item(key = "filters_and_export") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(ReportDateFilter.values()) { filter ->
                        FilterChip(
                            selected = state.selectedFilter == filter,
                            onClick = {
                                reportsViewModel.selectFilter(filter)
                                coroutineScope.launch {
                                    scrollState.animateScrollToItem(0)
                                }
                            },
                            label = { Text(filter.label, fontWeight = if (state.selectedFilter == filter) FontWeight.Bold else FontWeight.Normal) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { reportsViewModel.exportSalesCsv() },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("CSV")
                    }

                    Button(
                        onClick = { reportsViewModel.exportTallyFormat() },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tally XML")
                    }
                }
            }
        }

        // Summary KPI Metrics with Animated Transitions
        item(key = "kpi_summary") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Financial & Sales Overview (${state.selectedFilter.label})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Instant Calculation",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    AnimatedContent(
                        targetState = state.summary,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "summary_kpi_transition"
                    ) { summary ->
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ReportStatBox(title = "Gross Revenue", value = "₹${String.format("%.2f", summary.grossSales)}", color = AmberPrimary, modifier = Modifier.weight(1f))
                                ReportStatBox(title = "Discounts Given", value = "-₹${String.format("%.2f", summary.totalDiscounts)}", color = CrimsonRed, modifier = Modifier.weight(1f))
                                ReportStatBox(title = "Net Revenue", value = "₹${String.format("%.2f", summary.netSales)}", color = EmeraldGreen, modifier = Modifier.weight(1f))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ReportStatBox(title = "GST Tax Collected", value = "₹${String.format("%.2f", summary.totalTax)}", color = SkyBlue, modifier = Modifier.weight(1f))
                                ReportStatBox(title = "Total Invoices", value = "${summary.totalBillsCount} Bills", color = PurpleAccent, modifier = Modifier.weight(1f))
                                ReportStatBox(title = "Avg Bill Value", value = "₹${String.format("%.2f", summary.avgBillValue)}", color = AmberDark, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // Hourly Peak Sales Trend Chart
        item(key = "hourly_sales_chart") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Hourly Revenue Distribution",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    val hourlyPoints = state.hourlyTrend
                    val maxAmount = remember(hourlyPoints) {
                        (hourlyPoints.maxOfOrNull { it.amount } ?: 1.0).coerceAtLeast(100.0)
                    }

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    ) {
                        val barCount = hourlyPoints.size
                        if (barCount == 0) return@Canvas
                        val spacing = 6.dp.toPx()
                        val totalSpacing = spacing * (barCount - 1)
                        val barWidth = ((size.width - totalSpacing) / barCount).coerceAtLeast(4.dp.toPx())

                        hourlyPoints.forEachIndexed { index, point ->
                            val x = index * (barWidth + spacing)
                            val barHeight = ((point.amount / maxAmount) * (size.height - 20.dp.toPx())).toFloat().coerceAtLeast(4.dp.toPx())
                            val y = size.height - barHeight

                            val barColor = if (point.amount > 0) AmberPrimary else Color.LightGray.copy(alpha = 0.3f)
                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("8 AM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("2 PM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("8 PM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("11 PM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Payment Method Breakdown
        item(key = "payment_breakdown") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Payment Mode Distribution",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val distributions = state.paymentDistributions
                    if (distributions.isEmpty() || distributions.all { it.count == 0 }) {
                        Text("No completed bills in this period.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        distributions.filter { it.count > 0 }.forEach { dist ->
                            PaymentDistRow(dist)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        // P&L Profitability Breakdown
        item(key = "pl_breakdown") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Restaurant P&L (Profit & Loss Estimate)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Surface(
                            color = if (state.estimatedProfit >= 0) EmeraldGreen.copy(alpha = 0.15f) else CrimsonRed.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Margin: ${String.format("%.1f", state.profitMarginPercent)}%",
                                fontWeight = FontWeight.Bold,
                                color = if (state.estimatedProfit >= 0) EmeraldGreen else CrimsonRed,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    PlLineItem(label = "(+) Net Sales Revenue", amount = state.netSales, isPositive = true)
                    PlLineItem(label = "(-) Estimated Food Cost (32% benchmark)", amount = -state.estimatedFoodCost, isPositive = false)
                    PlLineItem(label = "(-) Operating Expenses (Register)", amount = -state.totalExpenses, isPositive = false)
                    PlLineItem(label = "(-) Raw Material Purchases", amount = -state.totalPurchases, isPositive = false)

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Estimated Net Profit",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "₹${String.format("%.2f", state.estimatedProfit)}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (state.estimatedProfit >= 0) EmeraldGreen else CrimsonRed
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentDistRow(dist: PaymentDistribution) {
    val methodColor = when (dist.method) {
        com.example.data.entity.PaymentMethod.CASH -> EmeraldGreen
        com.example.data.entity.PaymentMethod.UPI -> SkyBlue
        com.example.data.entity.PaymentMethod.CARD -> AmberPrimary
        com.example.data.entity.PaymentMethod.CREDIT -> CrimsonRed
        com.example.data.entity.PaymentMethod.SPLIT -> PurpleAccent
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(methodColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(dist.method.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                Spacer(modifier = Modifier.width(6.dp))
                Text("(${dist.count} orders)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("₹${String.format("%.2f", dist.totalAmount)} (${String.format("%.1f", dist.percentage)}%)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (dist.percentage / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = methodColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun ReportStatBox(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = color)
        }
    }
}

@Composable
fun PlLineItem(label: String, amount: Double, isPositive: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            "${if (amount < 0) "-" else ""}₹${String.format("%.2f", kotlin.math.abs(amount))}",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (isPositive) EmeraldGreen else CrimsonRed
        )
    }
}
