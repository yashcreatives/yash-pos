package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.UserEntity
import com.example.data.entity.UserRole
import com.example.ui.theme.*
import com.example.ui.viewmodel.*

enum class PosNavDestination(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val minRole: UserRole) {
    POS_BILLING("Billing POS", Icons.Default.PointOfSale, UserRole.CASHIER),
    TABLES("Tables & Floor", Icons.Default.TableBar, UserRole.CASHIER),
    DASHBOARD("Dashboard", Icons.Default.Dashboard, UserRole.CASHIER),
    KDS("Kitchen (KDS)", Icons.Default.SoupKitchen, UserRole.CASHIER),
    DAILY_TALLY("Daily Closing & Exp", Icons.Default.AccountBalanceWallet, UserRole.CASHIER),
    INVENTORY("Inventory & Stock", Icons.Default.Inventory2, UserRole.MANAGER),
    REPORTS("Reports & P&L", Icons.Default.Assessment, UserRole.MANAGER),
    BILL_HISTORY("Bill History", Icons.Default.ReceiptLong, UserRole.CASHIER),
    CUSTOMERS("Customer CRM", Icons.Default.People, UserRole.MANAGER),
    MENU_ITEMS("Menu Catalog", Icons.Default.RestaurantMenu, UserRole.MANAGER),
    SETTINGS("Settings & Backup", Icons.Default.Settings, UserRole.ADMIN)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    authViewModel: AuthViewModel,
    posViewModel: PosViewModel,
    tableViewModel: TableViewModel,
    kdsViewModel: KdsViewModel,
    dashboardViewModel: DashboardViewModel,
    expenseTallyViewModel: ExpenseTallyViewModel,
    inventoryViewModel: InventoryViewModel,
    reportsViewModel: ReportsViewModel,
    billHistoryViewModel: BillHistoryViewModel,
    customerViewModel: CustomerViewModel,
    menuViewModel: MenuManagementViewModel,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val authState by authViewModel.uiState.collectAsState()
    val posState by posViewModel.uiState.collectAsState()
    val tableState by tableViewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()

    var currentDestination by remember { mutableStateOf(PosNavDestination.POS_BILLING) }
    val currentUser = authState.currentUser

    // Lock Screen Dialog
    if (authState.isLocked) {
        LockScreenOverlay(
            currentUser = currentUser,
            onUnlock = { pin -> authViewModel.unlockScreen(pin) }
        )
    }

    // Logout Confirmation Dialog
    if (authState.showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { authViewModel.dismissLogout() },
            title = { Text("Sign Out of Terminal", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to end your active session and return to the login screen?") },
            confirmButton = {
                Button(
                    onClick = { authViewModel.confirmLogout() },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed)
                ) {
                    Text("Yes, Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { authViewModel.dismissLogout() }) { Text("Cancel") }
            }
        )
    }

    // Floating Snackbar Toast
    val activeToast = posState.toastMessage ?: tableState.toastMessage
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(activeToast) {
        activeToast?.let {
            snackbarHostState.showSnackbar(it)
            posViewModel.clearToast()
            tableViewModel.clearToast()
        }
    }

    // Filter available navigation tabs based on user role
    val visibleDestinations = remember(currentUser) {
        val role = currentUser?.role ?: UserRole.CASHIER
        PosNavDestination.values().filter { dest ->
            when (dest.minRole) {
                UserRole.CASHIER -> true
                UserRole.MANAGER -> role == UserRole.MANAGER || role == UserRole.OWNER || role == UserRole.ADMIN
                UserRole.OWNER -> role == UserRole.OWNER || role == UserRole.ADMIN
                UserRole.ADMIN -> role == UserRole.ADMIN || role == UserRole.OWNER
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth > 840.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AmberPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Restaurant, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = settingsState.settings.restaurantName.ifEmpty { "Yash POS" },
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1
                                )
                                Text(
                                    text = "${settingsState.settings.branchName} • Offline Active 🟢",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = EmeraldGreen
                                )
                            }
                        }
                    },
                    actions = {
                        // User Profile Badge & Lock / Logout
                        currentUser?.let { user ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (user.role) {
                                                    UserRole.OWNER -> CrimsonRed
                                                    UserRole.MANAGER -> AmberPrimary
                                                    UserRole.CASHIER -> EmeraldGreen
                                                    UserRole.ADMIN -> PurpleAccent
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = user.fullName.take(1).uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column {
                                        Text(user.fullName, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                        Text(
                                            user.role.name,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                                            color = when (user.role) {
                                                UserRole.OWNER -> CrimsonRed
                                                UserRole.MANAGER -> AmberPrimary
                                                UserRole.CASHIER -> EmeraldGreen
                                                UserRole.ADMIN -> PurpleAccent
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        IconButton(onClick = { authViewModel.lockScreen() }, modifier = Modifier.testTag("lock_screen_btn")) {
                            Icon(Icons.Default.Lock, contentDescription = "Lock Screen")
                        }

                        IconButton(onClick = { authViewModel.promptLogout() }, modifier = Modifier.testTag("logout_btn")) {
                            Icon(Icons.Default.Logout, contentDescription = "Sign Out", tint = CrimsonRed)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                if (!isWide) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        visibleDestinations.take(5).forEach { dest ->
                            NavigationBarItem(
                                selected = currentDestination == dest,
                                onClick = { currentDestination = dest },
                                icon = { Icon(dest.icon, contentDescription = dest.label) },
                                label = { Text(dest.label.take(8), fontSize = 10.sp) }
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (isWide) {
                    // Modern Navigation Rail for Tablet / POS Terminal
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.widthIn(min = 90.dp, max = 110.dp)
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        visibleDestinations.forEach { dest ->
                            NavigationRailItem(
                                selected = currentDestination == dest,
                                onClick = { currentDestination = dest },
                                icon = { Icon(dest.icon, contentDescription = dest.label, modifier = Modifier.size(22.dp)) },
                                label = {
                                    Text(
                                        text = dest.label,
                                        fontSize = 10.sp,
                                        fontWeight = if (currentDestination == dest) FontWeight.Bold else FontWeight.Normal,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        maxLines = 2
                                    )
                                },
                                modifier = Modifier.padding(vertical = 4.dp).testTag("nav_rail_${dest.name}")
                            )
                        }
                    }
                    VerticalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                }

                // Active Main Screen Content
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (currentDestination) {
                        PosNavDestination.POS_BILLING -> {
                            PosBillingScreen(
                                posViewModel = posViewModel,
                                currentUser = currentUser,
                                onNavigateToTables = { currentDestination = PosNavDestination.TABLES }
                            )
                        }
                        PosNavDestination.TABLES -> {
                            TableManagementScreen(
                                tableViewModel = tableViewModel,
                                onSelectTableForOrder = { table ->
                                    posViewModel.setSelectedTable(table)
                                    currentDestination = PosNavDestination.POS_BILLING
                                }
                            )
                        }
                        PosNavDestination.DASHBOARD -> {
                            DashboardScreen(
                                dashboardViewModel = dashboardViewModel,
                                userRole = currentUser?.role ?: UserRole.CASHIER,
                                onNavigateToPos = { currentDestination = PosNavDestination.POS_BILLING },
                                onNavigateToTables = { currentDestination = PosNavDestination.TABLES },
                                onNavigateToKds = { currentDestination = PosNavDestination.KDS }
                            )
                        }
                        PosNavDestination.KDS -> {
                            KdsScreen(kdsViewModel = kdsViewModel)
                        }
                        PosNavDestination.DAILY_TALLY -> {
                            ExpenseTallyScreen(
                                tallyViewModel = expenseTallyViewModel,
                                currentUser = currentUser
                            )
                        }
                        PosNavDestination.INVENTORY -> {
                            InventoryScreen(
                                inventoryViewModel = inventoryViewModel,
                                currentUser = currentUser
                            )
                        }
                        PosNavDestination.REPORTS -> {
                            ReportsScreen(reportsViewModel = reportsViewModel)
                        }
                        PosNavDestination.BILL_HISTORY -> {
                            BillHistoryScreen(
                                billHistoryViewModel = billHistoryViewModel,
                                currentUser = currentUser
                            )
                        }
                        PosNavDestination.CUSTOMERS -> {
                            CustomerCrmScreen(customerViewModel = customerViewModel)
                        }
                        PosNavDestination.MENU_ITEMS -> {
                            MenuManagementScreen(
                                menuViewModel = menuViewModel,
                                currentUser = currentUser
                            )
                        }
                        PosNavDestination.SETTINGS -> {
                            SettingsScreen(
                                settingsViewModel = settingsViewModel,
                                currentUser = currentUser
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LockScreenOverlay(
    currentUser: UserEntity?,
    onUnlock: (String) -> Boolean
) {
    var pinInput by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = {}) {
        Card(
            modifier = Modifier
                .width(360.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = AmberPrimary, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("Terminal Locked", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Logged in as ${currentUser?.fullName ?: "Staff"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = pinInput,
                    onValueChange = {
                        pinInput = it
                        errorMsg = null
                    },
                    label = { Text("Enter PIN to Resume") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(errorMsg!!, color = CrimsonRed, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (!onUnlock(pinInput)) {
                            errorMsg = "Incorrect PIN code"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
                ) {
                    Text("Unlock Terminal")
                }
            }
        }
    }
}
