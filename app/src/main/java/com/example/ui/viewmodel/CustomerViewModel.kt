package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.entity.CustomerEntity
import com.example.data.repository.PosRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CustomerUiState(
    val customers: List<CustomerEntity> = emptyList(),
    val searchQuery: String = "",
    val showAddEditDialog: Boolean = false,
    val editingCustomer: CustomerEntity? = null,
    val selectedCustomerDetail: CustomerEntity? = null,
    val toastMessage: String? = null
) {
    val filteredCustomers: List<CustomerEntity>
        get() {
            if (searchQuery.isBlank()) return customers
            val q = searchQuery.trim().lowercase()
            return customers.filter {
                it.name.lowercase().contains(q) || it.phone.contains(q) || it.address.lowercase().contains(q)
            }
        }
}

class CustomerViewModel(private val repository: PosRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerUiState())
    val uiState: StateFlow<CustomerUiState> = _uiState.asStateFlow()

    init {
        loadCustomers()
    }

    private fun loadCustomers() {
        viewModelScope.launch {
            repository.allCustomers.collect { custList ->
                _uiState.value = _uiState.value.copy(customers = custList)
            }
        }
    }

    fun onSearchChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun openAddCustomer() {
        _uiState.value = _uiState.value.copy(showAddEditDialog = true, editingCustomer = null)
    }

    fun openEditCustomer(customer: CustomerEntity) {
        _uiState.value = _uiState.value.copy(showAddEditDialog = true, editingCustomer = customer)
    }

    fun dismissAddEdit() {
        _uiState.value = _uiState.value.copy(showAddEditDialog = false, editingCustomer = null)
    }

    fun selectCustomer(customer: CustomerEntity) {
        _uiState.value = _uiState.value.copy(selectedCustomerDetail = customer)
    }

    fun dismissDetail() {
        _uiState.value = _uiState.value.copy(selectedCustomerDetail = null)
    }

    fun saveCustomer(
        name: String,
        phone: String,
        address: String,
        birthday: String,
        notes: String,
        creditBalance: Double
    ) {
        viewModelScope.launch {
            val editing = _uiState.value.editingCustomer
            if (editing != null) {
                repository.updateCustomer(
                    editing.copy(
                        name = name,
                        phone = phone,
                        address = address,
                        birthday = birthday,
                        notes = notes,
                        creditBalance = creditBalance
                    )
                )
            } else {
                repository.insertCustomer(
                    CustomerEntity(
                        name = name,
                        phone = phone,
                        address = address,
                        birthday = birthday,
                        notes = notes,
                        creditBalance = creditBalance
                    )
                )
            }
            _uiState.value = _uiState.value.copy(
                showAddEditDialog = false,
                editingCustomer = null,
                toastMessage = "Customer $name saved"
            )
        }
    }

    fun adjustCredit(customer: CustomerEntity, amountPaid: Double) {
        viewModelScope.launch {
            val updated = customer.copy(
                creditBalance = (customer.creditBalance - amountPaid).coerceAtLeast(0.0)
            )
            repository.updateCustomer(updated)
            _uiState.value = _uiState.value.copy(
                selectedCustomerDetail = updated,
                toastMessage = "Received ₹$amountPaid credit settlement from ${customer.name}"
            )
        }
    }

    fun clearToast() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }
}
