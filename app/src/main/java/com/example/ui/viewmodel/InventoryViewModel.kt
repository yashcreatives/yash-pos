package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.entity.*
import com.example.data.repository.PosRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class InventoryUiState(
    val products: List<ProductEntity> = emptyList(),
    val ingredients: List<IngredientEntity> = emptyList(),
    val suppliers: List<SupplierEntity> = emptyList(),
    val purchases: List<PurchaseEntity> = emptyList(),
    val selectedTab: Int = 0, // 0: Stock, 1: Recipes & Ingredients, 2: Purchases & Suppliers
    val showAdjustStockDialog: ProductEntity? = null,
    val showAddIngredientDialog: Boolean = false,
    val showAddPurchaseDialog: Boolean = false,
    val showAddSupplierDialog: Boolean = false,
    val toastMessage: String? = null
) {
    val lowStockProducts: List<ProductEntity>
        get() = products.filter { it.trackStock && it.currentStock <= it.minStockAlert }

    val lowStockIngredients: List<IngredientEntity>
        get() = ingredients.filter { it.currentStock <= it.minStockAlert }
}

class InventoryViewModel(private val repository: PosRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            repository.allProducts.collect { prods ->
                _uiState.value = _uiState.value.copy(products = prods)
            }
        }
        viewModelScope.launch {
            repository.allIngredients.collect { ings ->
                _uiState.value = _uiState.value.copy(ingredients = ings)
            }
        }
        viewModelScope.launch {
            repository.allSuppliers.collect { sups ->
                _uiState.value = _uiState.value.copy(suppliers = sups)
            }
        }
        viewModelScope.launch {
            repository.allPurchases.collect { purs ->
                _uiState.value = _uiState.value.copy(purchases = purs)
            }
        }
    }

    fun selectTab(tab: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun openAdjustStock(product: ProductEntity) {
        _uiState.value = _uiState.value.copy(showAdjustStockDialog = product)
    }

    fun dismissAdjustStock() {
        _uiState.value = _uiState.value.copy(showAdjustStockDialog = null)
    }

    fun adjustStock(productId: Long, delta: Double, reason: String, currentUser: UserEntity?) {
        viewModelScope.launch {
            repository.adjustProductStock(productId, delta)
            val prod = repository.getProductById(productId)
            repository.logAction(
                userId = currentUser?.id ?: 0,
                userName = currentUser?.fullName ?: "Staff",
                role = currentUser?.role?.name ?: "MANAGER",
                action = "STOCK_ADJUSTED",
                details = "Adjusted stock for ${prod?.name ?: "Product #$productId"} by $delta. Reason: $reason"
            )
            _uiState.value = _uiState.value.copy(
                showAdjustStockDialog = null,
                toastMessage = "Stock updated for ${prod?.name ?: "Product"}"
            )
        }
    }

    fun addIngredient(name: String, stock: Double, alert: Double, unit: String, cost: Double, supplier: String) {
        viewModelScope.launch {
            repository.insertIngredient(
                IngredientEntity(
                    name = name,
                    currentStock = stock,
                    minStockAlert = alert,
                    unit = unit,
                    costPerUnit = cost,
                    supplierName = supplier
                )
            )
            _uiState.value = _uiState.value.copy(
                showAddIngredientDialog = false,
                toastMessage = "Ingredient $name added"
            )
        }
    }

    fun addSupplier(name: String, phone: String, email: String, address: String) {
        viewModelScope.launch {
            repository.insertSupplier(
                SupplierEntity(name = name, phone = phone, email = email, address = address)
            )
            _uiState.value = _uiState.value.copy(
                showAddSupplierDialog = false,
                toastMessage = "Supplier $name registered"
            )
        }
    }

    fun addPurchase(supplierName: String, invoiceNo: String, total: Double, paid: Double, summary: String) {
        viewModelScope.launch {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            repository.insertPurchase(
                PurchaseEntity(
                    invoiceNumber = invoiceNo.ifEmpty { "PUR-${System.currentTimeMillis() % 10000}" },
                    supplierName = supplierName,
                    purchaseDate = dateStr,
                    totalAmount = total,
                    paidAmount = paid,
                    dueAmount = (total - paid).coerceAtLeast(0.0),
                    itemsSummary = summary
                )
            )
            _uiState.value = _uiState.value.copy(
                showAddPurchaseDialog = false,
                toastMessage = "Purchase invoice recorded for ₹$total"
            )
        }
    }

    fun openAddIngredient() { _uiState.value = _uiState.value.copy(showAddIngredientDialog = true) }
    fun dismissAddIngredient() { _uiState.value = _uiState.value.copy(showAddIngredientDialog = false) }

    fun openAddSupplier() { _uiState.value = _uiState.value.copy(showAddSupplierDialog = true) }
    fun dismissAddSupplier() { _uiState.value = _uiState.value.copy(showAddSupplierDialog = false) }

    fun openAddPurchase() { _uiState.value = _uiState.value.copy(showAddPurchaseDialog = true) }
    fun dismissAddPurchase() { _uiState.value = _uiState.value.copy(showAddPurchaseDialog = false) }

    fun clearToast() { _uiState.value = _uiState.value.copy(toastMessage = null) }
}
