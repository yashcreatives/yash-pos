package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.data.entity.CustomerEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.CustomerUiState
import com.example.ui.viewmodel.CustomerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerCrmScreen(
    customerViewModel: CustomerViewModel,
    modifier: Modifier = Modifier
) {
    val state by customerViewModel.uiState.collectAsState()

    // Add / Edit Dialog
    if (state.showAddEditDialog) {
        AddEditCustomerDialog(
            customer = state.editingCustomer,
            onDismiss = { customerViewModel.dismissAddEdit() },
            onSave = { name, phone, addr, bday, notes, cred ->
                customerViewModel.saveCustomer(name, phone, addr, bday, notes, cred)
            }
        )
    }

    // Detail Dialog
    state.selectedCustomerDetail?.let { cust ->
        CustomerDetailDialog(
            customer = cust,
            onDismiss = { customerViewModel.dismissDetail() },
            onEdit = { customerViewModel.openEditCustomer(cust) },
            onSettleCredit = { amt -> customerViewModel.adjustCredit(cust, amt) }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search & Add Customer Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { customerViewModel.onSearchChange(it) },
                placeholder = { Text("Search customer by name, phone, address...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = { customerViewModel.openAddCustomer() },
                colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Customer")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(state.filteredCustomers, key = { it.id }) { customer ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { customerViewModel.selectCustomer(customer) }
                        .testTag("customer_card_${customer.id}"),
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
                            Text(customer.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(customer.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (customer.address.isNotEmpty()) {
                                Text(customer.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Surface(
                                color = AmberPrimary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "★ ${customer.loyaltyPoints} Pts",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = AmberDark,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${customer.totalOrders} Orders | ₹${String.format("%.0f", customer.totalSpending)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (customer.creditBalance > 0) {
                                Text(
                                    text = "Due: ₹${String.format("%.0f", customer.creditBalance)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                    color = CrimsonRed
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerDetailDialog(
    customer: CustomerEntity,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onSettleCredit: (Double) -> Unit
) {
    var settleAmountStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = AmberPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(customer.name, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Phone: ${customer.phone}")
                Text("Address: ${customer.address}")
                if (customer.birthday.isNotEmpty()) Text("Birthday: ${customer.birthday}")
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Text("Total Visits: ${customer.totalOrders}")
                Text("Lifetime Spending: ₹${String.format("%.2f", customer.totalSpending)}")
                Text("Loyalty Reward Points: ${customer.loyaltyPoints} Points")
                Text("Outstanding Credit Due: ₹${String.format("%.2f", customer.creditBalance)}", color = if (customer.creditBalance > 0) CrimsonRed else EmeraldGreen, fontWeight = FontWeight.Bold)

                if (customer.creditBalance > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = settleAmountStr,
                        onValueChange = { settleAmountStr = it },
                        label = { Text("Enter Amount to Settle Due") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (customer.creditBalance > 0) {
                Button(
                    onClick = {
                        val amt = settleAmountStr.toDoubleOrNull() ?: 0.0
                        if (amt > 0) onSettleCredit(amt)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                ) {
                    Text("Settle Credit")
                }
            } else {
                Button(onClick = onEdit) { Text("Edit Customer") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun AddEditCustomerDialog(
    customer: CustomerEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, Double) -> Unit
) {
    var name by remember { mutableStateOf(customer?.name ?: "") }
    var phone by remember { mutableStateOf(customer?.phone ?: "") }
    var address by remember { mutableStateOf(customer?.address ?: "") }
    var birthday by remember { mutableStateOf(customer?.birthday ?: "") }
    var notes by remember { mutableStateOf(customer?.notes ?: "") }
    var creditStr by remember { mutableStateOf("${customer?.creditBalance ?: 0.0}") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (customer != null) "Edit Customer" else "Add New Customer", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Customer Full Name") }, singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Mobile Phone") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true)
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address / Locality") }, singleLine = true)
                OutlinedTextField(value = birthday, onValueChange = { birthday = it }, label = { Text("Birthday (YYYY-MM-DD)") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(name, phone, address, birthday, notes, creditStr.toDoubleOrNull() ?: 0.0)
                }
            ) {
                Text("Save Customer")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
