package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.CartItem
import com.example.ui.viewmodel.PosUiState
import com.example.ui.viewmodel.PosViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosBillingScreen(
    posViewModel: PosViewModel,
    currentUser: UserEntity?,
    onNavigateToTables: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by posViewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Variant / Modifier Dialog
    state.showVariantDialogForProduct?.let { product ->
        VariantModifierDialog(
            product = product,
            variants = state.availableVariants,
            modifiers = state.availableModifiers,
            onDismiss = { posViewModel.dismissVariantDialog() },
            onAddToCart = { variant, selectedMods, qty, notes ->
                posViewModel.addToCartWithCustomization(product, variant, selectedMods, qty, notes)
            }
        )
    }

    // Checkout Dialog
    if (state.showCheckoutDialog) {
        CheckoutDialog(
            posUiState = state,
            onDismiss = { posViewModel.dismissCheckout() },
            onConfirmPayment = { method, paidAmount, splitInfo ->
                posViewModel.completeCheckout(method, paidAmount, splitInfo, currentUser)
            }
        )
    }

    // Thermal Receipt Dialog
    if (state.showReceiptDialog) {
        ReceiptPreviewDialog(
            receiptText = state.receiptContent,
            onDismiss = { posViewModel.dismissReceipt() }
        )
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth > 840.dp

        if (isWideScreen) {
            // Tablet / Desktop 2-Column Split POS Layout
            Row(modifier = Modifier.fillMaxSize()) {
                val gridState = rememberLazyGridState()

                // Smooth scroll to top whenever category or search filter changes
                LaunchedEffect(state.selectedCategoryId, state.searchQuery, state.vegOnlyFilter) {
                    if (gridState.firstVisibleItemIndex > 0) {
                        gridState.animateScrollToItem(0)
                    }
                }

                // Left Column: Catalog & Products Grid (60% width)
                Box(
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight()
                        .padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        PosSearchBarAndFilters(
                            searchQuery = state.searchQuery,
                            onSearchChange = { posViewModel.onSearchQueryChange(it) },
                            vegOnlyFilter = state.vegOnlyFilter,
                            onToggleVeg = { posViewModel.toggleVegFilter() }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        CategoryFilterChipsRow(
                            categories = state.categories,
                            selectedCategoryId = state.selectedCategoryId,
                            onSelectCategory = { posViewModel.selectCategory(it) }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val filteredProducts = remember(state.products, state.selectedCategoryId, state.searchQuery, state.vegOnlyFilter) {
                            var list = state.products
                            if (state.selectedCategoryId > 0L) {
                                list = list.filter { it.categoryId == state.selectedCategoryId }
                            }
                            if (state.vegOnlyFilter) {
                                list = list.filter { it.isVeg }
                            }
                            if (state.searchQuery.isNotBlank()) {
                                val q = state.searchQuery.trim().lowercase()
                                list = list.filter { it.name.lowercase().contains(q) || it.code.lowercase().contains(q) }
                            }
                            list
                        }

                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Adaptive(minSize = 150.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("pos_product_grid"),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(filteredProducts, key = { it.id }) { product ->
                                ProductPosCard(
                                    product = product,
                                    onClick = { posViewModel.onProductClick(product) }
                                )
                            }
                        }
                    }

                    // Floating Scroll-to-Top Button for smooth navigation
                    val showScrollTop by remember {
                        derivedStateOf { gridState.firstVisibleItemIndex > 4 }
                    }
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showScrollTop,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut(),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 16.dp, end = 16.dp)
                    ) {
                        FloatingActionButton(
                            onClick = {
                                coroutineScope.launch {
                                    gridState.animateScrollToItem(0)
                                }
                            },
                            containerColor = AmberPrimary,
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Scroll to top")
                        }
                    }
                }

                // Right Column: Order Ticket / Cart Panel (40% width)
                Card(
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight()
                        .padding(start = 8.dp, top = 12.dp, end = 16.dp, bottom = 12.dp)
                        .testTag("pos_cart_panel"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    CartPanelContent(
                        state = state,
                        posViewModel = posViewModel,
                        currentUser = currentUser,
                        onNavigateToTables = onNavigateToTables
                    )
                }
            }
        } else {
            // Mobile Vertical Flow Layout with Smooth Grid Scroll
            val mobileGridState = rememberLazyGridState()

            LaunchedEffect(state.selectedCategoryId, state.searchQuery, state.vegOnlyFilter) {
                if (mobileGridState.firstVisibleItemIndex > 0) {
                    mobileGridState.animateScrollToItem(0)
                }
            }

            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                PosSearchBarAndFilters(
                    searchQuery = state.searchQuery,
                    onSearchChange = { posViewModel.onSearchQueryChange(it) },
                    vegOnlyFilter = state.vegOnlyFilter,
                    onToggleVeg = { posViewModel.toggleVegFilter() }
                )

                Spacer(modifier = Modifier.height(8.dp))

                CategoryFilterChipsRow(
                    categories = state.categories,
                    selectedCategoryId = state.selectedCategoryId,
                    onSelectCategory = { posViewModel.selectCategory(it) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                val filteredProducts = remember(state.products, state.selectedCategoryId, state.searchQuery, state.vegOnlyFilter) {
                    var list = state.products
                    if (state.selectedCategoryId > 0L) list = list.filter { it.categoryId == state.selectedCategoryId }
                    if (state.vegOnlyFilter) list = list.filter { it.isVeg }
                    if (state.searchQuery.isNotBlank()) {
                        val q = state.searchQuery.trim().lowercase()
                        list = list.filter { it.name.lowercase().contains(q) || it.code.lowercase().contains(q) }
                    }
                    list
                }

                Box(modifier = Modifier.weight(1f)) {
                    LazyVerticalGrid(
                        state = mobileGridState,
                        columns = GridCells.Adaptive(minSize = 130.dp),
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        items(filteredProducts, key = { it.id }) { product ->
                            ProductPosCard(product = product, onClick = { posViewModel.onProductClick(product) })
                        }
                    }
                }

                // Compact Bottom Cart Bar
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "${state.totalItemCount} Items | ₹${String.format("%.2f", state.grandTotal)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = AmberPrimary
                            )
                            Text(
                                text = state.orderType.name + if (state.selectedTable != null) " (${state.selectedTable?.tableNumber})" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = { posViewModel.sendToKitchen(currentUser) }) {
                                Icon(Icons.Default.SoupKitchen, contentDescription = "KOT", tint = AmberDark)
                            }
                            Button(
                                onClick = { posViewModel.openCheckout() },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Pay Now", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PosSearchBarAndFilters(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    vegOnlyFilter: Boolean,
    onToggleVeg: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search dishes by name or item code...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .testTag("pos_search_input"),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        FilterChip(
            selected = vegOnlyFilter,
            onClick = onToggleVeg,
            label = { Text("Veg 🟢") },
            leadingIcon = {
                Icon(
                    Icons.Default.Spa,
                    contentDescription = null,
                    tint = if (vegOnlyFilter) Color.White else EmeraldGreen,
                    modifier = Modifier.size(16.dp)
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = EmeraldGreen,
                selectedLabelColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(48.dp)
        )
    }
}

@Composable
fun CategoryFilterChipsRow(
    categories: List<CategoryEntity>,
    selectedCategoryId: Long,
    onSelectCategory: (Long) -> Unit
) {
    val chipListState = rememberLazyListState()

    // Smoothly auto-scroll the selected category into visible range
    LaunchedEffect(selectedCategoryId) {
        val targetIndex = if (selectedCategoryId == 0L) 0 else {
            val foundIdx = categories.indexOfFirst { it.id == selectedCategoryId }
            if (foundIdx >= 0) foundIdx + 1 else 0
        }
        chipListState.animateScrollToItem((targetIndex - 1).coerceAtLeast(0))
    }

    LazyRow(
        state = chipListState,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item(key = "all_dishes_category") {
            FilterChip(
                selected = selectedCategoryId == 0L,
                onClick = { onSelectCategory(0L) },
                label = { Text("All Dishes", fontWeight = if (selectedCategoryId == 0L) FontWeight.Bold else FontWeight.Normal) },
                shape = RoundedCornerShape(10.dp)
            )
        }
        items(categories, key = { it.id }) { cat ->
            FilterChip(
                selected = selectedCategoryId == cat.id,
                onClick = { onSelectCategory(cat.id) },
                label = { Text(cat.name, fontWeight = if (selectedCategoryId == cat.id) FontWeight.Bold else FontWeight.Normal) },
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}

@Composable
fun ProductPosCard(
    product: ProductEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag("product_card_${product.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Veg / Non-Veg Icon
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .border(
                            1.5.dp,
                            if (product.isVeg) EmeraldGreen else CrimsonRed,
                            RoundedCornerShape(3.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (product.isVeg) EmeraldGreen else CrimsonRed)
                    )
                }

                if (product.isBestseller) {
                    Surface(
                        color = AmberLight.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "★ Best",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            ),
                            color = AmberDark,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = product.name,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "₹${String.format("%.0f", product.price)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = AmberPrimary
                    )
                )

                if (product.trackStock) {
                    Text(
                        text = "Stk: ${product.currentStock.toInt()}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = if (product.currentStock <= product.minStockAlert) CrimsonRed else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CartPanelContent(
    state: PosUiState,
    posViewModel: PosViewModel,
    currentUser: UserEntity?,
    onNavigateToTables: () -> Unit
) {
    val cartListState = rememberLazyListState()

    // Smooth auto-scroll to the bottom when a new cart item is added
    LaunchedEffect(state.cartItems.size) {
        if (state.cartItems.isNotEmpty()) {
            cartListState.animateScrollToItem(state.cartItems.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        // Order Type Selector Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OrderType.values().forEach { type ->
                val isSelected = state.orderType == type
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { posViewModel.setOrderType(type) },
                    color = if (isSelected) AmberPrimary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = type.name.replace("_", " "),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 10.sp
                        ),
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 6.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Table Selection / Customer Info Row
        if (state.orderType == OrderType.DINE_IN) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onNavigateToTables() },
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TableBar, contentDescription = null, tint = AmberPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (state.selectedTable != null) "Table: ${state.selectedTable.tableNumber}" else "Select Dine-in Table",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Text(
                        text = "Change",
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberPrimary
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = state.customerName,
                    onValueChange = { posViewModel.onCustomerInfoChange(it, state.customerPhone) },
                    placeholder = { Text("Customer Name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = state.customerPhone,
                    onValueChange = { posViewModel.onCustomerInfoChange(state.customerName, it) },
                    placeholder = { Text("Phone") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        // Cart Items List with Smooth Scrolling
        if (state.cartItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Cart is Empty",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Tap on dishes to add items",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                state = cartListState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                itemsIndexed(
                    items = state.cartItems,
                    key = { index, item -> "${item.product.id}_${item.selectedVariant?.id}_$index" }
                ) { index, cartItem ->
                    CartItemRow(
                        cartItem = cartItem,
                        onIncrease = { posViewModel.increaseQuantity(index) },
                        onDecrease = { posViewModel.decreaseQuantity(index) },
                        onRemove = { posViewModel.removeItem(index) },
                        onToggleComp = { posViewModel.toggleComplimentary(index) }
                    )
                }
            }
        }

        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        Spacer(modifier = Modifier.height(8.dp))

        // Summary Calculations
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Subtotal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("₹${String.format("%.2f", state.subtotal)}", style = MaterialTheme.typography.bodySmall)
            }

            if (state.calculatedDiscount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Discount", style = MaterialTheme.typography.bodySmall, color = EmeraldGreen)
                    Text("-₹${String.format("%.2f", state.calculatedDiscount)}", style = MaterialTheme.typography.bodySmall, color = EmeraldGreen)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("GST (5%)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("₹${String.format("%.2f", state.taxAmount)}", style = MaterialTheme.typography.bodySmall)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Grand Total",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    "₹${String.format("%.2f", state.grandTotal)}",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = AmberPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bottom Action Buttons: KOT & Checkout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { posViewModel.sendToKitchen(currentUser) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("send_kot_btn"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.SoupKitchen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Send KOT", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { posViewModel.openCheckout() },
                modifier = Modifier
                    .weight(1.3f)
                    .height(48.dp)
                    .testTag("checkout_btn"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
            ) {
                Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Checkout ₹${String.format("%.0f", state.grandTotal)}", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CartItemRow(
    cartItem: CartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit,
    onToggleComp: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cartItem.displayName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (cartItem.modifiersSummary.isNotEmpty()) {
                    Text(
                        text = "+ ${cartItem.modifiersSummary}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "₹${String.format("%.2f", cartItem.unitPrice)} each",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Quantity Stepper
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onDecrease,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                }

                Text(
                    text = "${cartItem.quantity}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                IconButton(
                    onClick = onIncrease,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                }

                Text(
                    text = "₹${String.format("%.0f", cartItem.totalPrice)}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}

@Composable
fun VariantModifierDialog(
    product: ProductEntity,
    variants: List<ProductVariantEntity>,
    modifiers: List<ProductModifierEntity>,
    onDismiss: () -> Unit,
    onAddToCart: (ProductVariantEntity?, List<ProductModifierEntity>, Int, String) -> Unit
) {
    var selectedVariant by remember { mutableStateOf(variants.firstOrNull { it.isDefault } ?: variants.firstOrNull()) }
    val selectedModifiers = remember { mutableStateListOf<ProductModifierEntity>() }
    var quantity by remember { mutableIntStateOf(1) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Customize: ${product.name}", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (variants.isNotEmpty()) {
                    Text("Select Size / Portion:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    variants.forEach { v ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedVariant = v }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedVariant?.id == v.id, onClick = { selectedVariant = v })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${v.name} - ₹${String.format("%.0f", v.price)}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (modifiers.isNotEmpty()) {
                    Text("Add Modifiers / Add-ons:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    modifiers.forEach { m ->
                        val isChecked = selectedModifiers.any { it.id == m.id }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (isChecked) selectedModifiers.removeAll { it.id == m.id } else selectedModifiers.add(m)
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked) selectedModifiers.add(m) else selectedModifiers.removeAll { it.id == m.id }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${m.name} (+₹${String.format("%.0f", m.price)})", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Kitchen Cooking Note (e.g. Less Spicy, Jain)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAddToCart(selectedVariant, selectedModifiers.toList(), quantity, notes) },
                colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
            ) {
                Text("Add to Order")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CheckoutDialog(
    posUiState: PosUiState,
    onDismiss: () -> Unit,
    onConfirmPayment: (PaymentMethod, Double, String) -> Unit
) {
    var selectedMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var cashTendered by remember { mutableStateOf("${posUiState.grandTotal.toInt()}") }

    val tendered = cashTendered.toDoubleOrNull() ?: posUiState.grandTotal
    val changeReturn = (tendered - posUiState.grandTotal).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Complete Payment & Settle Bill", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Grand Total Banner
                Surface(
                    color = AmberLight.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Amount Payable", style = MaterialTheme.typography.labelMedium)
                        Text(
                            "₹${String.format("%.2f", posUiState.grandTotal)}",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = AmberPrimary
                            )
                        )
                    }
                }

                Text("Select Payment Method:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaymentMethod.values().forEach { method ->
                        val isSelected = selectedMethod == method
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedMethod = method },
                            color = if (isSelected) AmberPrimary else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = method.name,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(vertical = 10.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                if (selectedMethod == PaymentMethod.CASH) {
                    OutlinedTextField(
                        value = cashTendered,
                        onValueChange = { cashTendered = it },
                        label = { Text("Cash Received from Guest") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Change Return to Guest:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "₹${String.format("%.2f", changeReturn)}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmPayment(selectedMethod, posUiState.grandTotal, "") },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                modifier = Modifier.testTag("confirm_payment_btn")
            ) {
                Text("Confirm & Print Bill", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Back") }
        }
    )
}

@Composable
fun ReceiptPreviewDialog(
    receiptText: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = AmberPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Thermal Bill Receipt Preview", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = receiptText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
            ) {
                Text("Done / Close")
            }
        }
    )
}
