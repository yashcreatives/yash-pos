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
import com.example.data.entity.CategoryEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.UserEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.MenuManagementUiState
import com.example.ui.viewmodel.MenuManagementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuManagementScreen(
    menuViewModel: MenuManagementViewModel,
    currentUser: UserEntity?,
    modifier: Modifier = Modifier
) {
    val state by menuViewModel.uiState.collectAsState()

    // Add / Edit Product Dialog
    if (state.showAddEditProductDialog) {
        AddEditProductDialog(
            product = state.editingProduct,
            categories = state.categories,
            onDismiss = { menuViewModel.dismissAddEditProduct() },
            onSave = { name, short, code, catId, price, cost, tax, veg, best, track, stk, alert, prep, stn ->
                menuViewModel.saveProduct(name, short, code, catId, price, cost, tax, veg, best, track, stk, alert, prep, stn, currentUser)
            },
            onDelete = { id -> menuViewModel.deleteProduct(id) }
        )
    }

    // Add / Edit Category Dialog
    if (state.showAddEditCategoryDialog) {
        AddEditCategoryDialog(
            category = state.editingCategory,
            onDismiss = { menuViewModel.dismissAddEditCategory() },
            onSave = { name, icon, color -> menuViewModel.saveCategory(name, icon, color) },
            onDelete = { id -> menuViewModel.deleteCategory(id) }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Tab Row
        TabRow(
            selectedTabIndex = state.selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
        ) {
            Tab(selected = state.selectedTab == 0, onClick = { menuViewModel.selectTab(0) }, text = { Text("Menu Items / Dishes", fontWeight = FontWeight.SemiBold) })
            Tab(selected = state.selectedTab == 1, onClick = { menuViewModel.selectTab(1) }, text = { Text("Categories", fontWeight = FontWeight.SemiBold) })
            Tab(selected = state.selectedTab == 2, onClick = { menuViewModel.selectTab(2) }, text = { Text("Live In-Stock Toggle (86)", fontWeight = FontWeight.SemiBold) })
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (state.selectedTab) {
            0 -> {
                // Products List Tab
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { menuViewModel.onSearchQueryChange(it) },
                        placeholder = { Text("Search dishes...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = { menuViewModel.openAddProduct() },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Dish")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.filteredProducts, key = { it.id }) { product ->
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
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            color = if (product.isVeg) EmeraldGreen.copy(alpha = 0.15f) else CrimsonRed.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                if (product.isVeg) "VEG 🟢" else "NON-VEG 🔴",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                color = if (product.isVeg) EmeraldGreen else CrimsonRed,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text("Code: ${product.code} | Station: ${product.kitchenStation}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        "₹${String.format("%.0f", product.price)}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = AmberPrimary)
                                    )
                                    IconButton(onClick = { menuViewModel.openEditProduct(product) }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                // Categories List Tab
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Product Categories", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = { menuViewModel.openAddCategory() },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Category")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.categories, key = { it.id }) { cat ->
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Folder, contentDescription = null, tint = AmberPrimary)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(cat.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                }

                                IconButton(onClick = { menuViewModel.openEditCategory(cat) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                // Live 86 / In-Stock Switcher Tab
                Text("Instant 86'd Kitchen Out-of-Stock Toggles", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.products, key = { it.id }) { product ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(product.name, fontWeight = FontWeight.Bold)
                                    Text(
                                        if (product.isAvailable) "Available in POS" else "OUT OF STOCK (Hidden)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (product.isAvailable) EmeraldGreen else CrimsonRed
                                    )
                                }

                                Switch(
                                    checked = product.isAvailable,
                                    onCheckedChange = { menuViewModel.toggleProductAvailability(product) }
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
fun AddEditProductDialog(
    product: ProductEntity?,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Long, Double, Double, Double, Boolean, Boolean, Boolean, Double, Double, Int, String) -> Unit,
    onDelete: (Long) -> Unit
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var shortName by remember { mutableStateOf(product?.shortName ?: "") }
    var code by remember { mutableStateOf(product?.code ?: "") }
    var selectedCatId by remember { mutableStateOf(product?.categoryId ?: (categories.firstOrNull()?.id ?: 1L)) }
    var priceStr by remember { mutableStateOf("${product?.price ?: 200.0}") }
    var isVeg by remember { mutableStateOf(product?.isVeg ?: true) }
    var isBestseller by remember { mutableStateOf(product?.isBestseller ?: false) }
    var trackStock by remember { mutableStateOf(product?.trackStock ?: false) }
    var stockStr by remember { mutableStateOf("${product?.currentStock ?: 25.0}") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (product != null) "Edit Dish" else "Add New Dish", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Dish Full Name") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Item Code") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = priceStr, onValueChange = { priceStr = it }, label = { Text("Selling Price (₹)") }, modifier = Modifier.weight(1f), singleLine = true)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(selected = isVeg, onClick = { isVeg = true }, label = { Text("Veg 🟢") })
                    FilterChip(selected = !isVeg, onClick = { isVeg = false }, label = { Text("Non-Veg 🔴") })
                    FilterChip(selected = isBestseller, onClick = { isBestseller = !isBestseller }, label = { Text("★ Best") })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceStr.toDoubleOrNull() ?: 100.0
                    val stk = stockStr.toDoubleOrNull() ?: 20.0
                    onSave(name, shortName, code, selectedCatId, price, 0.0, 5.0, isVeg, isBestseller, trackStock, stk, 5.0, 15, "Main Kitchen")
                }
            ) {
                Text("Save Dish")
            }
        },
        dismissButton = {
            if (product != null) {
                TextButton(onClick = { onDelete(product.id) }) { Text("Delete", color = CrimsonRed) }
            } else {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@Composable
fun AddEditCategoryDialog(
    category: CategoryEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
    onDelete: (Long) -> Unit
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var colorHex by remember { mutableStateOf(category?.colorHex ?: "#D97706") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category != null) "Edit Category" else "Add Category", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Category Name") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name, "folder", colorHex) }) { Text("Save Category") }
        },
        dismissButton = {
            if (category != null) {
                TextButton(onClick = { onDelete(category.id) }) { Text("Delete", color = CrimsonRed) }
            } else {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
