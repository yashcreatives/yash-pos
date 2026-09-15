package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.example.data.entity.FloorSectionEntity
import com.example.data.entity.RestaurantTableEntity
import com.example.data.entity.TableStatus
import com.example.ui.theme.*
import com.example.ui.viewmodel.TableUiState
import com.example.ui.viewmodel.TableViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableManagementScreen(
    tableViewModel: TableViewModel,
    onSelectTableForOrder: (RestaurantTableEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by tableViewModel.uiState.collectAsState()

    // Table Action Sheet / Dialog
    state.selectedTableForAction?.let { table ->
        TableActionDialog(
            table = table,
            onDismiss = { tableViewModel.dismissTableAction() },
            onTakeOrder = {
                tableViewModel.dismissTableAction()
                onSelectTableForOrder(table)
            },
            onMarkAvailable = { tableViewModel.markTableAvailable(table.id) },
            onMarkCleaning = { tableViewModel.markTableCleaning(table.id) },
            onOpenReserve = { tableViewModel.openReserveDialog(table) },
            onOpenTransfer = { tableViewModel.openTransferDialog(table) },
            onEdit = { tableViewModel.openEditTable(table) }
        )
    }

    // Add / Edit Table Dialog
    if (state.showAddEditTableDialog) {
        AddEditTableDialog(
            editingTable = state.editingTable,
            floors = state.floors,
            selectedFloorId = state.selectedFloorId,
            onDismiss = { tableViewModel.dismissAddEditTable() },
            onSave = { num, cap, flId, id -> tableViewModel.saveTable(num, cap, flId, id) },
            onDelete = { id -> tableViewModel.deleteTable(id) }
        )
    }

    // Reserve Dialog
    if (state.showReserveTableDialog && state.selectedTableForAction != null) {
        ReserveTableDialog(
            table = state.selectedTableForAction!!,
            onDismiss = { tableViewModel.dismissReserveDialog() },
            onConfirm = { name, phone, time ->
                tableViewModel.reserveTable(state.selectedTableForAction!!.id, name, phone, time)
            }
        )
    }

    // Transfer Dialog
    if (state.showTransferTableDialog && state.selectedTableForAction != null) {
        TransferTableDialog(
            fromTable = state.selectedTableForAction!!,
            availableTables = state.tables.filter { it.id != state.selectedTableForAction!!.id && it.status == TableStatus.AVAILABLE },
            onDismiss = { tableViewModel.dismissTransferDialog() },
            onConfirm = { toTableId ->
                tableViewModel.transferTable(state.selectedTableForAction!!.id, toTableId)
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Floor Sections Row & Action Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(state.floors) { floor ->
                    val isSelected = state.selectedFloorId == floor.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { tableViewModel.selectFloor(floor.id) },
                        label = { Text(floor.name, fontWeight = FontWeight.SemiBold) },
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = {
                            Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }

            Button(
                onClick = { tableViewModel.openAddTable() },
                colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("add_table_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Table")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Legend Bar
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusLegendItem(label = "Available", color = EmeraldGreen)
                StatusLegendItem(label = "Occupied", color = CrimsonRed)
                StatusLegendItem(label = "Waiting Bill", color = AmberLight)
                StatusLegendItem(label = "Reserved", color = SkyBlue)
                StatusLegendItem(label = "Cleaning", color = PurpleAccent)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        val floorTables = remember(state.tables, state.selectedFloorId) {
            state.tables.filter { it.floorId == state.selectedFloorId }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("table_grid"),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(floorTables, key = { it.id }) { table ->
                TableCard(
                    table = table,
                    onClick = { tableViewModel.openTableAction(table) }
                )
            }
        }
    }
}

@Composable
fun StatusLegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
    }
}

@Composable
fun TableCard(
    table: RestaurantTableEntity,
    onClick: () -> Unit
) {
    val statusColor = when (table.status) {
        TableStatus.AVAILABLE -> EmeraldGreen
        TableStatus.OCCUPIED -> CrimsonRed
        TableStatus.WAITING_FOR_BILL -> AmberLight
        TableStatus.PAYMENT_PENDING -> AmberLight
        TableStatus.COMPLETED -> EmeraldGreen
        TableStatus.RESERVED -> SkyBlue
        TableStatus.CLEANING -> PurpleAccent
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("table_card_${table.tableNumber}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, statusColor.copy(alpha = 0.8f))
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
                    text = table.tableNumber,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = table.status.name.replace("_", " "),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        ),
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.People,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${table.capacity} Seats",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (table.status == TableStatus.OCCUPIED || table.status == TableStatus.WAITING_FOR_BILL) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Running Bill:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "₹${String.format("%.0f", table.currentOrderAmount)}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = AmberPrimary
                        )
                    )
                }
            }

            if (table.status == TableStatus.RESERVED) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Res: ${table.reservedCustomerName ?: ""} (${table.reservedTime ?: ""})",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = SkyBlue
                )
            }
        }
    }
}

@Composable
fun TableActionDialog(
    table: RestaurantTableEntity,
    onDismiss: () -> Unit,
    onTakeOrder: () -> Unit,
    onMarkAvailable: () -> Unit,
    onMarkCleaning: () -> Unit,
    onOpenReserve: () -> Unit,
    onOpenTransfer: () -> Unit,
    onEdit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Table ${table.tableNumber} Actions (${table.status.name})", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onTakeOrder,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
                ) {
                    Icon(Icons.Default.RestaurantMenu, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Take Order / Bill for this Table")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onMarkAvailable,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Mark Available")
                    }

                    OutlinedButton(
                        onClick = onMarkCleaning,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Mark Cleaning")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenReserve,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Reserve Table")
                    }

                    OutlinedButton(
                        onClick = onOpenTransfer,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Transfer Table")
                    }
                }

                TextButton(
                    onClick = onEdit,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit Table Settings")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun AddEditTableDialog(
    editingTable: RestaurantTableEntity?,
    floors: List<FloorSectionEntity>,
    selectedFloorId: Long,
    onDismiss: () -> Unit,
    onSave: (String, Int, Long, Long) -> Unit,
    onDelete: (Long) -> Unit
) {
    var tableNumber by remember { mutableStateOf(editingTable?.tableNumber ?: "T-") }
    var capacityStr by remember { mutableStateOf("${editingTable?.capacity ?: 4}") }
    var targetFloorId by remember { mutableStateOf(editingTable?.floorId ?: selectedFloorId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (editingTable != null) "Edit Table" else "Add New Table", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = tableNumber,
                    onValueChange = { tableNumber = it },
                    label = { Text("Table Identifier / Number (e.g. T-06, AC-04)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = capacityStr,
                    onValueChange = { capacityStr = it },
                    label = { Text("Seating Capacity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cap = capacityStr.toIntOrNull() ?: 4
                    onSave(tableNumber, cap, targetFloorId, editingTable?.id ?: 0L)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
            ) {
                Text("Save Table")
            }
        },
        dismissButton = {
            if (editingTable != null) {
                TextButton(onClick = { onDelete(editingTable.id) }) {
                    Text("Delete", color = CrimsonRed)
                }
            } else {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@Composable
fun ReserveTableDialog(
    table: RestaurantTableEntity,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("08:00 PM") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reserve Table ${table.tableNumber}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Customer Name") }, singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Reservation Time (e.g. 08:30 PM)") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, phone, time) },
                colors = ButtonDefaults.buttonColors(containerColor = SkyBlue)
            ) {
                Text("Save Reservation")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun TransferTableDialog(
    fromTable: RestaurantTableEntity,
    availableTables: List<RestaurantTableEntity>,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var selectedTargetId by remember { mutableStateOf(availableTables.firstOrNull()?.id ?: 0L) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transfer Table ${fromTable.tableNumber}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Select Target Table:")
                if (availableTables.isEmpty()) {
                    Text("No other available tables found on the floor.", color = CrimsonRed)
                } else {
                    availableTables.forEach { target ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedTargetId = target.id }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedTargetId == target.id, onClick = { selectedTargetId = target.id })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${target.tableNumber} (${target.capacity} seats)")
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (availableTables.isNotEmpty()) {
                Button(onClick = { onConfirm(selectedTargetId) }) { Text("Confirm Transfer") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
