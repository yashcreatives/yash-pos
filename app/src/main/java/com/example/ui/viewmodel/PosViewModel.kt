package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.entity.*
import com.example.data.repository.PosRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CartItem(
    val product: ProductEntity,
    val selectedVariant: ProductVariantEntity? = null,
    val selectedModifiers: List<ProductModifierEntity> = emptyList(),
    val quantity: Int = 1,
    val isComplimentary: Boolean = false,
    val notes: String = ""
) {
    val unitPrice: Double
        get() {
            if (isComplimentary) return 0.0
            val base = selectedVariant?.price ?: product.price
            val mods = selectedModifiers.sumOf { it.price }
            return base + mods
        }

    val totalPrice: Double
        get() = unitPrice * quantity

    val displayName: String
        get() {
            val variantText = if (selectedVariant != null) " (${selectedVariant.name})" else ""
            val compText = if (isComplimentary) " [COMPLIMENTARY]" else ""
            return "${product.name}$variantText$compText"
        }

    val modifiersSummary: String
        get() = selectedModifiers.joinToString(", ") { it.name }
}

data class PosUiState(
    val categories: List<CategoryEntity> = emptyList(),
    val products: List<ProductEntity> = emptyList(),
    val selectedCategoryId: Long = 0L, // 0 = All
    val searchQuery: String = "",
    val vegOnlyFilter: Boolean = false,
    val cartItems: List<CartItem> = emptyList(),
    val orderType: OrderType = OrderType.DINE_IN,
    val selectedTable: RestaurantTableEntity? = null,
    val customerName: String = "",
    val customerPhone: String = "",
    val deliveryAddress: String = "",
    val deliveryCharge: Double = 0.0,
    val orderNotes: String = "",
    val discountPercent: Double = 0.0,
    val discountAmount: Double = 0.0,
    val isFlatDiscount: Boolean = false,
    val discountReason: String = "",
    val showCheckoutDialog: Boolean = false,
    val showVariantDialogForProduct: ProductEntity? = null,
    val availableVariants: List<ProductVariantEntity> = emptyList(),
    val availableModifiers: List<ProductModifierEntity> = emptyList(),
    val showHoldOrdersDialog: Boolean = false,
    val heldOrders: List<OrderEntity> = emptyList(),
    val generatedBill: BillEntity? = null,
    val showReceiptDialog: Boolean = false,
    val receiptContent: String = "",
    val activeShift: ShiftEntity? = null,
    val toastMessage: String? = null
) {
    val subtotal: Double
        get() = cartItems.sumOf { it.totalPrice }

    val calculatedDiscount: Double
        get() {
            return if (isFlatDiscount) {
                discountAmount.coerceAtMost(subtotal)
            } else {
                (subtotal * (discountPercent / 100.0)).coerceAtMost(subtotal)
            }
        }

    val taxableAmount: Double
        get() = (subtotal - calculatedDiscount).coerceAtLeast(0.0)

    val taxAmount: Double
        get() = taxableAmount * 0.05 // 5% GST

    val grandTotal: Double
        get() = (taxableAmount + taxAmount + deliveryCharge).coerceAtLeast(0.0)

    val totalItemCount: Int
        get() = cartItems.sumOf { it.quantity }
}

class PosViewModel(private val repository: PosRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(PosUiState())
    val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            repository.allCategories.collect { cats ->
                _uiState.value = _uiState.value.copy(categories = cats)
            }
        }
        viewModelScope.launch {
            repository.allProducts.collect { prods ->
                _uiState.value = _uiState.value.copy(products = prods)
            }
        }
    }

    fun selectCategory(categoryId: Long) {
        _uiState.value = _uiState.value.copy(selectedCategoryId = categoryId)
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun toggleVegFilter() {
        _uiState.value = _uiState.value.copy(vegOnlyFilter = !_uiState.value.vegOnlyFilter)
    }

    fun setOrderType(type: OrderType) {
        _uiState.value = _uiState.value.copy(orderType = type)
    }

    fun setSelectedTable(table: RestaurantTableEntity?) {
        _uiState.value = _uiState.value.copy(
            selectedTable = table,
            orderType = if (table != null) OrderType.DINE_IN else _uiState.value.orderType
        )
    }

    fun onCustomerInfoChange(name: String, phone: String) {
        _uiState.value = _uiState.value.copy(customerName = name, customerPhone = phone)
    }

    fun onDeliveryInfoChange(address: String, charge: Double) {
        _uiState.value = _uiState.value.copy(deliveryAddress = address, deliveryCharge = charge)
    }

    fun onOrderNotesChange(notes: String) {
        _uiState.value = _uiState.value.copy(orderNotes = notes)
    }

    fun applyPercentageDiscount(percent: Double, reason: String = "") {
        _uiState.value = _uiState.value.copy(
            discountPercent = percent,
            isFlatDiscount = false,
            discountReason = reason
        )
    }

    fun applyFlatDiscount(amount: Double, reason: String = "") {
        _uiState.value = _uiState.value.copy(
            discountAmount = amount,
            isFlatDiscount = true,
            discountReason = reason
        )
    }

    fun clearDiscount() {
        _uiState.value = _uiState.value.copy(
            discountPercent = 0.0,
            discountAmount = 0.0,
            isFlatDiscount = false,
            discountReason = ""
        )
    }

    fun onProductClick(product: ProductEntity) {
        if (!product.isAvailable) {
            _uiState.value = _uiState.value.copy(toastMessage = "${product.name} is currently OUT OF STOCK")
            return
        }

        viewModelScope.launch {
            val variants = repository.getVariantsForProductSync(product.id)
            val modifiers = repository.getModifiersForProductSync(product.id)

            if (variants.isNotEmpty() || modifiers.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(
                    showVariantDialogForProduct = product,
                    availableVariants = variants,
                    availableModifiers = modifiers
                )
            } else {
                addToCart(CartItem(product = product))
            }
        }
    }

    fun addToCartWithCustomization(
        product: ProductEntity,
        variant: ProductVariantEntity?,
        modifiers: List<ProductModifierEntity>,
        quantity: Int = 1,
        notes: String = ""
    ) {
        addToCart(
            CartItem(
                product = product,
                selectedVariant = variant,
                selectedModifiers = modifiers,
                quantity = quantity,
                notes = notes
            )
        )
        _uiState.value = _uiState.value.copy(showVariantDialogForProduct = null)
    }

    fun dismissVariantDialog() {
        _uiState.value = _uiState.value.copy(showVariantDialogForProduct = null)
    }

    fun addToCart(cartItem: CartItem) {
        val current = _uiState.value.cartItems.toMutableList()
        val index = current.indexOfFirst {
            it.product.id == cartItem.product.id &&
                    it.selectedVariant?.id == cartItem.selectedVariant?.id &&
                    it.modifiersSummary == cartItem.modifiersSummary &&
                    it.isComplimentary == cartItem.isComplimentary &&
                    it.notes == cartItem.notes
        }

        if (index >= 0) {
            val existing = current[index]
            current[index] = existing.copy(quantity = existing.quantity + cartItem.quantity)
        } else {
            current.add(cartItem)
        }
        _uiState.value = _uiState.value.copy(cartItems = current)
    }

    fun increaseQuantity(index: Int) {
        val current = _uiState.value.cartItems.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(quantity = current[index].quantity + 1)
            _uiState.value = _uiState.value.copy(cartItems = current)
        }
    }

    fun decreaseQuantity(index: Int) {
        val current = _uiState.value.cartItems.toMutableList()
        if (index in current.indices) {
            if (current[index].quantity > 1) {
                current[index] = current[index].copy(quantity = current[index].quantity - 1)
            } else {
                current.removeAt(index)
            }
            _uiState.value = _uiState.value.copy(cartItems = current)
        }
    }

    fun removeItem(index: Int) {
        val current = _uiState.value.cartItems.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _uiState.value = _uiState.value.copy(cartItems = current)
        }
    }

    fun toggleComplimentary(index: Int) {
        val current = _uiState.value.cartItems.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(isComplimentary = !current[index].isComplimentary)
            _uiState.value = _uiState.value.copy(cartItems = current)
        }
    }

    fun updateItemNotes(index: Int, notes: String) {
        val current = _uiState.value.cartItems.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(notes = notes)
            _uiState.value = _uiState.value.copy(cartItems = current)
        }
    }

    fun clearCart() {
        _uiState.value = _uiState.value.copy(
            cartItems = emptyList(),
            selectedTable = null,
            customerName = "",
            customerPhone = "",
            deliveryAddress = "",
            deliveryCharge = 0.0,
            orderNotes = "",
            discountPercent = 0.0,
            discountAmount = 0.0
        )
    }

    fun sendToKitchen(currentUser: UserEntity?) {
        if (_uiState.value.cartItems.isEmpty()) {
            _uiState.value = _uiState.value.copy(toastMessage = "Cart is empty")
            return
        }

        viewModelScope.launch {
            val kotNumber = "KOT-${(100..999).random()}"
            val tableName = _uiState.value.selectedTable?.tableNumber ?: "Counter"
            val itemsSummary = _uiState.value.cartItems.joinToString("\n") {
                val mod = if (it.modifiersSummary.isNotEmpty()) " (${it.modifiersSummary})" else ""
                val note = if (it.notes.isNotEmpty()) " [Note: ${it.notes}]" else ""
                "${it.displayName}$mod x ${it.quantity}$note"
            }

            // Create Order if dine-in
            val order = OrderEntity(
                orderNumber = "ORD-${System.currentTimeMillis() % 100000}",
                orderType = _uiState.value.orderType,
                tableId = _uiState.value.selectedTable?.id ?: 0L,
                tableName = tableName,
                customerName = _uiState.value.customerName,
                customerPhone = _uiState.value.customerPhone,
                deliveryAddress = _uiState.value.deliveryAddress,
                deliveryCharge = _uiState.value.deliveryCharge,
                status = OrderStatus.SENT_TO_KITCHEN,
                subtotal = _uiState.value.subtotal,
                discountPercent = _uiState.value.discountPercent,
                discountAmount = _uiState.value.calculatedDiscount,
                taxAmount = _uiState.value.taxAmount,
                grandTotal = _uiState.value.grandTotal,
                notes = _uiState.value.orderNotes,
                cashierId = currentUser?.id ?: 0L,
                cashierName = currentUser?.fullName ?: "Staff"
            )
            val orderId = repository.saveOrder(order)

            // Save order items
            for (cart in _uiState.value.cartItems) {
                repository.insertOrderItem(
                    OrderItemEntity(
                        orderId = orderId,
                        productId = cart.product.id,
                        productName = cart.product.name,
                        variantName = cart.selectedVariant?.name ?: "",
                        modifiersJson = cart.modifiersSummary,
                        quantity = cart.quantity,
                        unitPrice = cart.unitPrice,
                        rate = cart.unitPrice,
                        totalPrice = cart.totalPrice,
                        isComplimentary = cart.isComplimentary,
                        notes = cart.notes,
                        isKotSent = true
                    )
                )
            }

            // Create KOT
            repository.createKot(
                KotEntity(
                    kotNumber = kotNumber,
                    orderId = orderId,
                    tableName = tableName,
                    orderType = _uiState.value.orderType,
                    itemsSummary = itemsSummary,
                    notes = _uiState.value.orderNotes,
                    priority = KotPriority.NORMAL,
                    status = KotStatus.NEW
                )
            )

            // Update Table if occupied
            _uiState.value.selectedTable?.let { table ->
                repository.updateTableStatus(
                    tableId = table.id,
                    status = TableStatus.OCCUPIED,
                    orderId = orderId,
                    amount = _uiState.value.grandTotal,
                    occupiedSince = if (table.occupiedSinceTimestamp > 0) table.occupiedSinceTimestamp else System.currentTimeMillis()
                )
            }

            _uiState.value = _uiState.value.copy(
                toastMessage = "KOT #$kotNumber generated & sent to Kitchen Display!"
            )
        }
    }

    fun openCheckout() {
        if (_uiState.value.cartItems.isEmpty()) {
            _uiState.value = _uiState.value.copy(toastMessage = "Please add items to cart before billing")
            return
        }
        _uiState.value = _uiState.value.copy(showCheckoutDialog = true)
    }

    fun dismissCheckout() {
        _uiState.value = _uiState.value.copy(showCheckoutDialog = false)
    }

    fun completeCheckout(
        paymentMethod: PaymentMethod,
        paidAmount: Double,
        splitDetails: String,
        currentUser: UserEntity?
    ) {
        viewModelScope.launch {
            val invoiceNumber = repository.generateInvoiceNumber()
            val state = _uiState.value
            val tableName = state.selectedTable?.tableNumber ?: ""

            val itemsSummary = state.cartItems.joinToString("\n") {
                val mod = if (it.modifiersSummary.isNotEmpty()) " (+${it.modifiersSummary})" else ""
                "${it.displayName}$mod x ${it.quantity} = ₹${String.format("%.2f", it.totalPrice)}"
            }

            // Save order first
            val order = OrderEntity(
                orderNumber = "ORD-${System.currentTimeMillis() % 100000}",
                orderType = state.orderType,
                tableId = state.selectedTable?.id ?: 0L,
                tableName = tableName,
                customerName = state.customerName,
                customerPhone = state.customerPhone,
                deliveryAddress = state.deliveryAddress,
                deliveryCharge = state.deliveryCharge,
                status = OrderStatus.BILLED,
                subtotal = state.subtotal,
                discountPercent = state.discountPercent,
                discountAmount = state.calculatedDiscount,
                taxAmount = state.taxAmount,
                grandTotal = state.grandTotal,
                notes = state.orderNotes,
                cashierId = currentUser?.id ?: 0L,
                cashierName = currentUser?.fullName ?: "Staff"
            )
            val orderId = repository.saveOrder(order)

            val orderItemsList = mutableListOf<OrderItemEntity>()
            for (cart in state.cartItems) {
                val itemEntity = OrderItemEntity(
                    orderId = orderId,
                    productId = cart.product.id,
                    productName = cart.product.name,
                    variantName = cart.selectedVariant?.name ?: "",
                    modifiersJson = cart.modifiersSummary,
                    quantity = cart.quantity,
                    unitPrice = cart.unitPrice,
                    rate = cart.unitPrice,
                    totalPrice = cart.totalPrice,
                    isComplimentary = cart.isComplimentary,
                    notes = cart.notes,
                    isKotSent = true
                )
                repository.insertOrderItem(itemEntity)
                orderItemsList.add(itemEntity)
            }

            val bill = BillEntity(
                invoiceNumber = invoiceNumber,
                orderId = orderId,
                orderType = state.orderType,
                tableName = tableName,
                customerName = state.customerName.ifEmpty { "Walk-in Guest" },
                customerPhone = state.customerPhone,
                cashierId = currentUser?.id ?: 0L,
                cashierName = currentUser?.fullName ?: "Staff",
                itemsSummary = itemsSummary,
                subtotal = state.subtotal,
                discountPercent = state.discountPercent,
                discountAmount = state.calculatedDiscount,
                discountReason = state.discountReason,
                taxAmount = state.taxAmount,
                serviceCharge = state.deliveryCharge,
                grandTotal = state.grandTotal,
                paidAmount = paidAmount,
                dueAmount = (state.grandTotal - paidAmount).coerceAtLeast(0.0),
                paymentMethod = paymentMethod,
                splitDetails = splitDetails,
                status = BillStatus.COMPLETED
            )

            val billId = repository.completeBilling(
                bill = bill,
                orderId = orderId,
                tableId = state.selectedTable?.id,
                orderItems = orderItemsList,
                currentUser = currentUser
            )

            val savedBill = bill.copy(id = billId)
            val receipt = generateThermalReceiptText(savedBill, state.cartItems, state.deliveryCharge)

            _uiState.value = _uiState.value.copy(
                showCheckoutDialog = false,
                generatedBill = savedBill,
                receiptContent = receipt,
                showReceiptDialog = true,
                toastMessage = "Bill $invoiceNumber generated successfully! Paid via ${paymentMethod.name}"
            )

            // Clear active cart after successful billing
            clearCart()
        }
    }

    private fun generateThermalReceiptText(
        bill: BillEntity,
        items: List<CartItem>,
        deliveryCharge: Double
    ): String {
        val sb = StringBuilder()
        sb.appendLine("================================")
        sb.appendLine("      YASH FOOD PALACE & BAR    ")
        sb.appendLine(" 102 Gourmet Blvd, Food City    ")
        sb.appendLine(" GSTIN: 27ABCDE1234F1Z5         ")
        sb.appendLine(" Phone: +91 98765 43210         ")
        sb.appendLine("================================")
        sb.appendLine("Invoice No : ${bill.invoiceNumber}")
        sb.appendLine("Date/Time  : ${java.text.SimpleDateFormat("dd-MM-yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(bill.createdAt))}")
        sb.appendLine("Order Type : ${bill.orderType.name} ${if (bill.tableName.isNotEmpty()) "(${bill.tableName})" else ""}")
        sb.appendLine("Cashier    : ${bill.cashierName}")
        if (bill.customerName.isNotEmpty()) sb.appendLine("Customer   : ${bill.customerName}")
        sb.appendLine("--------------------------------")
        sb.appendLine(String.format("%-16s %3s %9s", "ITEM", "QTY", "AMOUNT"))
        sb.appendLine("--------------------------------")
        for (item in items) {
            val name = if (item.displayName.length > 16) item.displayName.take(15) + "…" else item.displayName
            sb.appendLine(String.format("%-16s %3d %9.2f", name, item.quantity, item.totalPrice))
        }
        sb.appendLine("--------------------------------")
        sb.appendLine(String.format("%-20s %10.2f", "Subtotal:", bill.subtotal))
        if (bill.discountAmount > 0) {
            sb.appendLine(String.format("%-20s -%9.2f", "Discount:", bill.discountAmount))
        }
        sb.appendLine(String.format("%-20s %10.2f", "GST (5%):", bill.taxAmount))
        if (deliveryCharge > 0) {
            sb.appendLine(String.format("%-20s %10.2f", "Delivery Charge:", deliveryCharge))
        }
        sb.appendLine("================================")
        sb.appendLine(String.format("%-20s ₹%9.2f", "GRAND TOTAL:", bill.grandTotal))
        sb.appendLine(String.format("%-20s %10s", "Payment Method:", bill.paymentMethod.name))
        sb.appendLine(String.format("%-20s %10.2f", "Paid Amount:", bill.paidAmount))
        if (bill.dueAmount > 0) {
            sb.appendLine(String.format("%-20s %10.2f", "Balance Due:", bill.dueAmount))
        }
        sb.appendLine("================================")
        sb.appendLine("     THANK YOU! VISIT AGAIN     ")
        sb.appendLine("   Software by Yash POS Suite   ")
        sb.appendLine("================================")
        return sb.toString()
    }

    fun dismissReceipt() {
        _uiState.value = _uiState.value.copy(showReceiptDialog = false)
    }

    fun clearToast() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }
}
