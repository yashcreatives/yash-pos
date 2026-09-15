package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.PaymentMethod
import com.example.data.entity.UserEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.ExpenseTallyUiState
import com.example.ui.viewmodel.ExpenseTallyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseTallyScreen(
    tallyViewModel: ExpenseTallyViewModel,
    currentUser: UserEntity?,
    modifier: Modifier = Modifier
) {
    val state by tallyViewModel.uiState.collectAsState()

    // Add Expense Dialog
    if (state.showAddExpenseDialog) {
        AddExpenseDialog(
            onDismiss = { tallyViewModel.dismissAddExpenseDialog() },
            onConfirm = { cat, amt, method, desc ->
                tallyViewModel.addExpense(cat, amt, method, desc, currentUser)
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Daily Cash Drawer Tally Sheet
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = AmberPrimary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Daily Cash Drawer Tally & Settlement",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Button(
                            onClick = { tallyViewModel.performDayClosing(currentUser) },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Perform Day Closing")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Formula Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = state.openingCashInput,
                            onValueChange = { tallyViewModel.onOpeningCashChange(it) },
                            label = { Text("Opening Cash Float") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = "₹${String.format("%.2f", state.todayCashSales)}",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("+ Cash Sales Today") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = "₹${String.format("%.2f", state.totalCashExpenses)}",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("- Cash Expenses") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = AmberLight.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Expected Cash in Drawer", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    "₹${String.format("%.2f", state.expectedClosingCash)}",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = AmberDark
                                    )
                                )
                            }
                        }

                        OutlinedTextField(
                            value = state.actualClosingCashInput,
                            onValueChange = { tallyViewModel.onActualCashChange(it) },
                            label = { Text("Actual Physical Cash Counted") },
                            placeholder = { Text("Enter physical cash") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )

                        state.cashDifference?.let { diff ->
                            Surface(
                                color = if (diff >= 0) EmeraldGreen.copy(alpha = 0.15f) else CrimsonRed.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        if (diff >= 0) "Cash Excess / Match" else "Cash Shortage",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (diff >= 0) EmeraldGreen else CrimsonRed
                                    )
                                    Text(
                                        "${if (diff >= 0) "+" else ""}₹${String.format("%.2f", diff)}",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (diff >= 0) EmeraldGreen else CrimsonRed
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Expense Records Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Daily Expense Register",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Total Expenses: ₹${String.format("%.2f", state.totalExpenses)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { tallyViewModel.openAddExpenseDialog() },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Expense")
                }
            }
        }

        if (state.expenses.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No expenses recorded today", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(state.expenses, key = { it.id }) { expense ->
                ExpenseRowCard(
                    expense = expense,
                    onDelete = { tallyViewModel.deleteExpense(expense.id) }
                )
            }
        }
    }
}

@Composable
fun ExpenseRowCard(
    expense: ExpenseEntity,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    Surface(
                        color = AmberPrimary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = expense.category,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = AmberPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Paid via ${expense.paymentMethod.name}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = expense.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "By ${expense.employeeName} | ${expense.referenceNumber}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "₹${String.format("%.2f", expense.amount)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = CrimsonRed
                    )
                )

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

@Composable
fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, PaymentMethod, String) -> Unit
) {
    val categories = listOf("Vegetables", "Meat & Chicken", "Dairy & Milk", "Gas Refill", "Electricity", "Staff Salary", "Packaging", "Maintenance", "Miscellaneous")
    var selectedCat by remember { mutableStateOf(categories.first()) }
    var amountStr by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Expense", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Expense Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Expense Category:", style = MaterialTheme.typography.labelMedium)
                // Dropdown or list chips
                LazyColumn(modifier = Modifier.height(120.dp)) {
                    items(categories) { cat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selectedCat == cat) AmberPrimary.copy(alpha = 0.15f) else Color.Transparent)
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedCat == cat, onClick = { selectedCat = cat })
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(cat, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Expense Notes / Reason") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onConfirm(selectedCat, amt, selectedMethod, desc)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
            ) {
                Text("Save Expense")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
