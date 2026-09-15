package com.example.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.BillEntity
import com.example.data.entity.BillStatus
import com.example.data.entity.UserEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.BillHistoryUiState
import com.example.ui.viewmodel.BillHistoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BillHistoryScreen(
    billHistoryViewModel: BillHistoryViewModel,
    currentUser: UserEntity?,
    modifier: Modifier = Modifier
) {
    val state by billHistoryViewModel.uiState.collectAsState()

    // Bill Detail Dialog
    state.selectedBillForDetail?.let { bill ->
        BillDetailDialog(
            bill = bill,
            onDismiss = { billHistoryViewModel.dismissDetail() },
            onRefund = { billHistoryViewModel.openRefundDialog(bill) }
        )
    }

    // Refund Dialog
    if (state.showRefundDialog && state.refundBill != null) {
        RefundAuthDialog(
            bill = state.refundBill!!,
            reason = state.refundReasonInput,
            onReasonChange = { billHistoryViewModel.onRefundReasonChange(it) },
            pin = state.authPinInput,
            onPinChange = { billHistoryViewModel.onAuthPinChange(it) },
            onDismiss = { billHistoryViewModel.dismissRefundDialog() },
            onConfirmRefund = {
                if (currentUser != null) {
                    billHistoryViewModel.processRefund(currentUser, isVoid = false)
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search & Toggle Action Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { billHistoryViewModel.onSearchChange(it) },
                placeholder = { Text("Search by Invoice #, Table, Customer...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )

            FilterChip(
                selected = state.showAuditLogs,
                onClick = { billHistoryViewModel.toggleAuditLogsView() },
                label = { Text("Audit Trail 🛡️") },
                shape = RoundedCornerShape(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (state.showAuditLogs) {
            Text("System Security & Operational Audit Trail", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.auditLogs, key = { it.id }) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = log.action,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = AmberPrimary
                                )
                                val timeStr = SimpleDateFormat("dd-MM HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                                Text(timeStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(log.details, style = MaterialTheme.typography.bodyMedium)
                            Text("User: ${log.userName} (${log.role})", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.filteredBills, key = { it.id }) { bill ->
                    BillHistoryRowCard(
                        bill = bill,
                        onClick = { billHistoryViewModel.selectBill(bill) }
                    )
                }
            }
        }
    }
}

@Composable
fun BillHistoryRowCard(
    bill: BillEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("bill_card_${bill.invoiceNumber}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(bill.invoiceNumber, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = when (bill.status) {
                            BillStatus.COMPLETED -> EmeraldGreen.copy(alpha = 0.15f)
                            BillStatus.REFUNDED -> CrimsonRed.copy(alpha = 0.15f)
                            BillStatus.VOIDED -> CrimsonRed.copy(alpha = 0.15f)
                            else -> AmberPrimary.copy(alpha = 0.15f)
                        },
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = bill.status.name,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            ),
                            color = when (bill.status) {
                                BillStatus.COMPLETED -> EmeraldGreen
                                else -> CrimsonRed
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(bill.createdAt))
                Text(
                    text = "$dateStr | ${bill.orderType.name} ${if (bill.tableName.isNotEmpty()) "(${bill.tableName})" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Guest: ${bill.customerName} | Cashier: ${bill.cashierName}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${String.format("%.2f", bill.grandTotal)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = AmberPrimary
                    )
                )
                Text(
                    text = bill.paymentMethod.name,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun BillDetailDialog(
    bill: BillEntity,
    onDismiss: () -> Unit,
    onRefund: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Receipt, contentDescription = null, tint = AmberPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Bill Details: ${bill.invoiceNumber}", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Customer: ${bill.customerName} (${bill.customerPhone})")
                Text("Cashier: ${bill.cashierName} | Type: ${bill.orderType}")
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Text("Items Ordered:\n${bill.itemsSummary}", style = MaterialTheme.typography.bodySmall)
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Subtotal:", style = MaterialTheme.typography.bodySmall)
                    Text("₹${String.format("%.2f", bill.subtotal)}", style = MaterialTheme.typography.bodySmall)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("GST (5%):", style = MaterialTheme.typography.bodySmall)
                    Text("₹${String.format("%.2f", bill.taxAmount)}", style = MaterialTheme.typography.bodySmall)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Grand Total:", fontWeight = FontWeight.Bold)
                    Text("₹${String.format("%.2f", bill.grandTotal)}", fontWeight = FontWeight.Bold, color = AmberPrimary)
                }
            }
        },
        confirmButton = {
            if (bill.status == BillStatus.COMPLETED) {
                Button(
                    onClick = onRefund,
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed)
                ) {
                    Text("Refund / Cancel Bill")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun RefundAuthDialog(
    bill: BillEntity,
    reason: String,
    onReasonChange: (String) -> Unit,
    pin: String,
    onPinChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirmRefund: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Authorize Refund: ${bill.invoiceNumber}", fontWeight = FontWeight.Bold, color = CrimsonRed) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Refunding: ₹${String.format("%.2f", bill.grandTotal)} to customer.")
                OutlinedTextField(
                    value = reason,
                    onValueChange = onReasonChange,
                    label = { Text("Refund Reason (e.g. Order Mistake, Return)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = onPinChange,
                    label = { Text("Manager / Owner PIN (e.g. 1111 or 2222)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmRefund,
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed)
            ) {
                Text("Authorize & Refund")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Back") } }
    )
}
