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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.UserEntity
import com.example.data.entity.UserRole
import com.example.ui.theme.*
import com.example.ui.viewmodel.SettingsUiState
import com.example.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    currentUser: UserEntity?,
    modifier: Modifier = Modifier
) {
    val state by settingsViewModel.uiState.collectAsState()

    var restaurantName by remember(state.settings) { mutableStateOf(state.settings.restaurantName) }
    var address by remember(state.settings) { mutableStateOf(state.settings.address) }
    var phone by remember(state.settings) { mutableStateOf(state.settings.phone) }
    var gstNumber by remember(state.settings) { mutableStateOf(state.settings.gstNumber) }
    var fssaiNumber by remember(state.settings) { mutableStateOf(state.settings.fssaiNumber) }
    var invoicePrefix by remember(state.settings) { mutableStateOf(state.settings.invoicePrefix) }
    var taxPercentStr by remember(state.settings) { mutableStateOf("${state.settings.defaultTaxPercent}") }

    // Add User Dialog
    if (state.showAddUserDialog) {
        AddUserDialog(
            onDismiss = { settingsViewModel.dismissAddUser() },
            onSave = { name, user, pin, pwd, role, mobile ->
                settingsViewModel.addUser(name, user, pin, pwd, role, mobile, currentUser)
            }
        )
    }

    // Backup Dump Dialog
    if (state.showBackupDialog) {
        AlertDialog(
            onDismissRequest = { settingsViewModel.dismissBackup() },
            title = { Text("Offline Database Backup Verified", fontWeight = FontWeight.Bold) },
            text = {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)
                ) {
                    Text(
                        text = state.backupDataJson,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(onClick = { settingsViewModel.dismissBackup() }) { Text("Done") }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Business Profile Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Restaurant & Tax Configuration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = restaurantName,
                        onValueChange = { restaurantName = it },
                        label = { Text("Restaurant / Brand Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Store Address & Outlet") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Contact Phone") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = invoicePrefix,
                            onValueChange = { invoicePrefix = it },
                            label = { Text("Invoice Prefix") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = gstNumber,
                            onValueChange = { gstNumber = it },
                            label = { Text("GSTIN Number") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = fssaiNumber,
                            onValueChange = { fssaiNumber = it },
                            label = { Text("FSSAI License #") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = taxPercentStr,
                        onValueChange = { taxPercentStr = it },
                        label = { Text("Default Tax / GST (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val tax = taxPercentStr.toDoubleOrNull() ?: 5.0
                            settingsViewModel.saveBusinessSettings(
                                restaurantName = restaurantName,
                                tagline = state.settings.tagline,
                                address = address,
                                phone = phone,
                                email = state.settings.email,
                                gstNumber = gstNumber,
                                fssaiNumber = fssaiNumber,
                                invoicePrefix = invoicePrefix,
                                taxPercent = tax,
                                serviceChargePercent = 5.0,
                                enableServiceCharge = false,
                                branchName = state.settings.branchName
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Business Profile")
                    }
                }
            }
        }

        // Database Backup & Offline Security
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Offline Database Backup & Security", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "All data is stored safely in local Room database on device storage. Create instant offline JSON dumps or sync snapshots.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { settingsViewModel.generateBackupDump(currentUser) },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Offline Backup Dump")
                    }
                }
            }
        }

        // Staff Accounts Management
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
                        Text("Staff Accounts & Roles", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Button(
                            onClick = { settingsViewModel.openAddUser() },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Staff")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    state.users.forEach { user ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(user.fullName, fontWeight = FontWeight.Bold)
                                Text("Role: ${user.role.name} | PIN: ${user.pin} | Mobile: ${user.mobile}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(
                                color = when (user.role) {
                                    UserRole.OWNER -> CrimsonRed.copy(alpha = 0.15f)
                                    UserRole.MANAGER -> AmberPrimary.copy(alpha = 0.15f)
                                    UserRole.CASHIER -> EmeraldGreen.copy(alpha = 0.15f)
                                    UserRole.ADMIN -> PurpleAccent.copy(alpha = 0.15f)
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    user.role.name,
                                    color = when (user.role) {
                                        UserRole.OWNER -> CrimsonRed
                                        UserRole.MANAGER -> AmberPrimary
                                        UserRole.CASHIER -> EmeraldGreen
                                        UserRole.ADMIN -> PurpleAccent
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    }
                }
            }
        }
    }
}

@Composable
fun AddUserDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, UserRole, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.CASHIER) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Staff Member", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, singleLine = true)
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") }, singleLine = true)
                OutlinedTextField(value = pin, onValueChange = { pin = it }, label = { Text("4-Digit PIN") }, singleLine = true)
                OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text("Mobile Phone") }, singleLine = true)

                Text("Role & Permission Level:", style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    UserRole.values().forEach { role ->
                        FilterChip(
                            selected = selectedRole == role,
                            onClick = { selectedRole = role },
                            label = { Text(role.name.take(4), fontSize = 10.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, username, pin, pin, selectedRole, mobile) }
            ) {
                Text("Create Account")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
