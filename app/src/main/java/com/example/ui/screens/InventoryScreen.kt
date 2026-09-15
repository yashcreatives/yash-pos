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
import com.example.data.entity.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.InventoryUiState
import com.example.ui.viewmodel.InventoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    inventoryViewModel: InventoryViewModel,
    currentUser: UserEntity?,
    modifier: Modifier = Modifier
) {
    val state by inventoryViewModel.uiState.collectAsState()

    // Adjust Stock Dialog
    state.showAdjustStockDialog?.let { prod ->
        AdjustStockDialog(
            product = prod,
            onDismiss = { inventoryViewModel.dismissAdjustStock() },
            onConfirm = { delta, reason ->
                inventoryViewModel.adjustStock(prod.id, delta, reason, currentUser)
            }
        )
    }

    // Add Ingredient Dialog
    if (state.showAddIngredientDialog) {
        AddIngredientDialog(
            onDismiss = { inventoryViewModel.dismissAddIngredient() },
            onConfirm = { name, stk, alert, unit, cost, sup ->
                inventoryViewModel.addIngredient(name, stk, alert, unit, cost, sup)
            }
        )
    }

    // Add Supplier Dialog
    if (state.showAddSupplierDialog) {
        AddSupplierDialog(
            onDismiss = { inventoryViewModel.dismissAddSupplier() },
            onConfirm = { name, phone, email, addr ->
                inventoryViewModel.addSupplier(name, phone, email, addr)
            }
        )
    }

    // Add Purchase Dialog
    if (state.showAddPurchaseDialog) {
        AddPurchaseDialog(
            suppliers = state.suppliers,
            onDismiss = { inventoryViewModel.dismissAddPurchase() },
            onConfirm = { sup, inv, tot, paid, sum ->
                inventoryViewModel.addPurchase(sup, inv, tot, paid, sum)
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Tab Row: Products Stock vs Raw Ingredients vs Suppliers/Purchases
        TabRow(
            selectedTabIndex = state.selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = state.selectedTab == 0,
                onClick = { inventoryViewModel.selectTab(0) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Product Stock", fontWeight = FontWeight.SemiBold)
                    }
                }
            )
            Tab(
                selected = state.selectedTab == 1,
                onClick = { inventoryViewModel.selectTab(1) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Egg, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Raw Ingredients", fontWeight = FontWeight.SemiBold)
                    }
                }
            )
            Tab(
                selected = state.selectedTab == 2,
                onClick = { inventoryViewModel.selectTab(2) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Purchases & Vendors", fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (state.selectedTab) {
            0 -> {
                // Product Stock Tab
                val tracked = state.products.filter { it.trackStock }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(tracked, key = { it.id }) { product ->
                        val isLow = product.currentStock <= product.minStockAlert
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
                                        Text(product.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        if (isLow) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                color = CrimsonRed.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    "LOW STOCK",
                                                    color = CrimsonRed,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text("Min Alert Threshold: ${product.minStockAlert.toInt()} units", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        "${product.currentStock.toInt()} in stock",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isLow) CrimsonRed else EmeraldGreen
                                        )
                                    )
                                    Button(
                                        onClick = { inventoryViewModel.openAdjustStock(product) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
                                    ) {
                                        Text("Adjust")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                // Raw Ingredients Tab
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Kitchen Ingredients & Raw Materials", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = { inventoryViewModel.openAddIngredient() },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Ingredient")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.ingredients, key = { it.id }) { ing ->
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
                                Column {
                                    Text(ing.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text("Supplier: ${ing.supplierName} | Cost: ₹${ing.costPerUnit}/${ing.unit}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Text(
                                    "${ing.currentStock} ${ing.unit}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (ing.currentStock <= ing.minStockAlert) CrimsonRed else EmeraldGreen
                                    )
                                )
                            }
                        }
                    }
                }
            }
            2 -> {
                // Purchases & Suppliers Tab
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Vendor Purchases & Invoices", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { inventoryViewModel.openAddSupplier() },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("+ Vendor")
                        }
                        Button(
                            onClick = { inventoryViewModel.openAddPurchase() },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("+ Purchase Bill")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.purchases, key = { it.id }) { pur ->
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
                                Column {
                                    Text("Invoice #${pur.invoiceNumber}", fontWeight = FontWeight.Bold)
                                    Text("Vendor: ${pur.supplierName} | Date: ${pur.purchaseDate}", style = MaterialTheme.typography.bodySmall)
                                    if (pur.itemsSummary.isNotEmpty()) {
                                        Text(pur.itemsSummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("₹${String.format("%.2f", pur.totalAmount)}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = AmberPrimary)
                                    if (pur.dueAmount > 0) {
                                        Text("Due: ₹${String.format("%.0f", pur.dueAmount)}", style = MaterialTheme.typography.bodySmall, color = CrimsonRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdjustStockDialog(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var deltaStr by remember { mutableStateOf("") }
    var isAddition by remember { mutableStateOf(true) }
    var reason by remember { mutableStateOf("New purchase / Restock") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adjust Stock: ${product.name}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Current Stock: ${product.currentStock.toInt()} units")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(selected = isAddition, onClick = { isAddition = true }, label = { Text("+ Add Stock") })
                    FilterChip(selected = !isAddition, onClick = { isAddition = false }, label = { Text("- Deduct / Wastage") })
                }

                OutlinedTextField(
                    value = deltaStr,
                    onValueChange = { deltaStr = it },
                    label = { Text("Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason (e.g. Expired, Spillage, Recount)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = deltaStr.toDoubleOrNull() ?: 0.0
                    if (qty > 0) {
                        onConfirm(if (isAddition) qty else -qty, reason)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
            ) {
                Text("Apply Adjustment")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddIngredientDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Double, String, Double, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var stockStr by remember { mutableStateOf("10.0") }
    var alertStr by remember { mutableStateOf("2.0") }
    var unit by remember { mutableStateOf("Kg") }
    var costStr by remember { mutableStateOf("100.0") }
    var supplier by remember { mutableStateOf("Local Vendor") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Raw Ingredient", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Ingredient Name") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = stockStr, onValueChange = { stockStr = it }, label = { Text("Current Stock") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Unit (Kg/Ltr)") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                OutlinedTextField(value = costStr, onValueChange = { costStr = it }, label = { Text("Cost Per Unit (₹)") }, singleLine = true)
                OutlinedTextField(value = supplier, onValueChange = { supplier = it }, label = { Text("Supplier Name") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(name, stockStr.toDoubleOrNull() ?: 0.0, alertStr.toDoubleOrNull() ?: 0.0, unit, costStr.toDoubleOrNull() ?: 0.0, supplier)
                }
            ) {
                Text("Add Ingredient")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddSupplierDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var addr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register Supplier", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Supplier Business Name") }, singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, singleLine = true)
                OutlinedTextField(value = addr, onValueChange = { addr = it }, label = { Text("Market / Yard Address") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, phone, email, addr) }) { Text("Save Supplier") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddPurchaseDialog(
    suppliers: List<SupplierEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double, Double, String) -> Unit
) {
    var supName by remember { mutableStateOf(suppliers.firstOrNull()?.name ?: "Prime Poultry") }
    var invNo by remember { mutableStateOf("") }
    var totalStr by remember { mutableStateOf("") }
    var paidStr by remember { mutableStateOf("") }
    var summary by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Purchase Bill", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = supName, onValueChange = { supName = it }, label = { Text("Supplier Name") }, singleLine = true)
                OutlinedTextField(value = invNo, onValueChange = { invNo = it }, label = { Text("Invoice Number") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = totalStr, onValueChange = { totalStr = it }, label = { Text("Total (₹)") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = paidStr, onValueChange = { paidStr = it }, label = { Text("Paid (₹)") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                OutlinedTextField(value = summary, onValueChange = { summary = it }, label = { Text("Items Summary") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val tot = totalStr.toDoubleOrNull() ?: 0.0
                    val pd = paidStr.toDoubleOrNull() ?: tot
                    onConfirm(supName, invNo, tot, pd, summary)
                }
            ) {
                Text("Save Purchase")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
