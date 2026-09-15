package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.entity.*
import com.example.data.repository.PosRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MenuManagementUiState(
    val products: List<ProductEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val selectedTab: Int = 0, // 0: Products, 1: Categories, 2: Menu Availability Toggles
    val selectedCategoryId: Long = 0L,
    val searchQuery: String = "",
    val showAddEditProductDialog: Boolean = false,
    val editingProduct: ProductEntity? = null,
    val showAddEditCategoryDialog: Boolean = false,
    val editingCategory: CategoryEntity? = null,
    val toastMessage: String? = null
) {
    val filteredProducts: List<ProductEntity>
        get() {
            var list = products
            if (selectedCategoryId > 0L) {
                list = list.filter { it.categoryId == selectedCategoryId }
            }
            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase()
                list = list.filter { it.name.lowercase().contains(q) || it.code.lowercase().contains(q) }
            }
            return list
        }
}

class MenuManagementViewModel(private val repository: PosRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(MenuManagementUiState())
    val uiState: StateFlow<MenuManagementUiState> = _uiState.asStateFlow()

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
            repository.allCategories.collect { cats ->
                _uiState.value = _uiState.value.copy(categories = cats)
            }
        }
    }

    fun selectTab(tab: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun selectCategory(catId: Long) {
        _uiState.value = _uiState.value.copy(selectedCategoryId = catId)
    }

    fun onSearchQueryChange(q: String) {
        _uiState.value = _uiState.value.copy(searchQuery = q)
    }

    fun toggleProductAvailability(product: ProductEntity) {
        viewModelScope.launch {
            val newStatus = !product.isAvailable
            repository.updateProductAvailability(product.id, newStatus)
            _uiState.value = _uiState.value.copy(
                toastMessage = "${product.name} is now ${if (newStatus) "AVAILABLE (In Stock)" else "OUT OF STOCK"}"
            )
        }
    }

    fun toggleTodaySpecial(product: ProductEntity) {
        viewModelScope.launch {
            repository.updateProduct(product.copy(isTodaySpecial = !product.isTodaySpecial), null)
        }
    }

    fun openAddProduct() {
        _uiState.value = _uiState.value.copy(showAddEditProductDialog = true, editingProduct = null)
    }

    fun openEditProduct(product: ProductEntity) {
        _uiState.value = _uiState.value.copy(showAddEditProductDialog = true, editingProduct = product)
    }

    fun dismissAddEditProduct() {
        _uiState.value = _uiState.value.copy(showAddEditProductDialog = false, editingProduct = null)
    }

    fun saveProduct(
        name: String,
        shortName: String,
        code: String,
        categoryId: Long,
        price: Double,
        purchasePrice: Double,
        taxPercent: Double,
        isVeg: Boolean,
        isBestseller: Boolean,
        trackStock: Boolean,
        stock: Double,
        minAlert: Double,
        prepTime: Int,
        kitchenStation: String,
        currentUser: UserEntity?
    ) {
        viewModelScope.launch {
            val editing = _uiState.value.editingProduct
            if (editing != null) {
                val updated = editing.copy(
                    name = name,
                    shortName = shortName,
                    code = code,
                    categoryId = categoryId,
                    price = price,
                    purchasePrice = purchasePrice,
                    taxPercent = taxPercent,
                    isVeg = isVeg,
                    isBestseller = isBestseller,
                    trackStock = trackStock,
                    currentStock = stock,
                    minStockAlert = minAlert,
                    prepTimeMinutes = prepTime,
                    kitchenStation = kitchenStation
                )
                repository.updateProduct(updated, currentUser)
                _uiState.value = _uiState.value.copy(
                    showAddEditProductDialog = false,
                    editingProduct = null,
                    toastMessage = "Updated product $name"
                )
            } else {
                val newProduct = ProductEntity(
                    name = name,
                    shortName = shortName,
                    code = code,
                    categoryId = categoryId,
                    price = price,
                    purchasePrice = purchasePrice,
                    taxPercent = taxPercent,
                    isVeg = isVeg,
                    isBestseller = isBestseller,
                    trackStock = trackStock,
                    currentStock = stock,
                    minStockAlert = minAlert,
                    prepTimeMinutes = prepTime,
                    kitchenStation = kitchenStation
                )
                repository.insertProduct(newProduct, currentUser)
                _uiState.value = _uiState.value.copy(
                    showAddEditProductDialog = false,
                    editingProduct = null,
                    toastMessage = "Added product $name"
                )
            }
        }
    }

    fun deleteProduct(id: Long) {
        viewModelScope.launch {
            repository.deleteProduct(id)
            _uiState.value = _uiState.value.copy(toastMessage = "Product deleted")
        }
    }

    fun openAddCategory() {
        _uiState.value = _uiState.value.copy(showAddEditCategoryDialog = true, editingCategory = null)
    }

    fun openEditCategory(category: CategoryEntity) {
        _uiState.value = _uiState.value.copy(showAddEditCategoryDialog = true, editingCategory = category)
    }

    fun dismissAddEditCategory() {
        _uiState.value = _uiState.value.copy(showAddEditCategoryDialog = false, editingCategory = null)
    }

    fun saveCategory(name: String, iconName: String, colorHex: String) {
        viewModelScope.launch {
            val editing = _uiState.value.editingCategory
            if (editing != null) {
                repository.updateCategory(editing.copy(name = name, iconName = iconName, colorHex = colorHex))
            } else {
                repository.insertCategory(CategoryEntity(name = name, iconName = iconName, colorHex = colorHex))
            }
            _uiState.value = _uiState.value.copy(
                showAddEditCategoryDialog = false,
                editingCategory = null,
                toastMessage = "Category $name saved"
            )
        }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            repository.deleteCategory(id)
            _uiState.value = _uiState.value.copy(toastMessage = "Category deleted")
        }
    }

    fun clearToast() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }
}
