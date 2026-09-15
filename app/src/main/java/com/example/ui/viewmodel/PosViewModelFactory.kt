package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.repository.PosRepository

@Suppress("UNCHECKED_CAST")
class PosViewModelFactory(private val repository: PosRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> AuthViewModel(repository) as T
            modelClass.isAssignableFrom(PosViewModel::class.java) -> PosViewModel(repository) as T
            modelClass.isAssignableFrom(TableViewModel::class.java) -> TableViewModel(repository) as T
            modelClass.isAssignableFrom(KdsViewModel::class.java) -> KdsViewModel(repository) as T
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> DashboardViewModel(repository) as T
            modelClass.isAssignableFrom(ExpenseTallyViewModel::class.java) -> ExpenseTallyViewModel(repository) as T
            modelClass.isAssignableFrom(InventoryViewModel::class.java) -> InventoryViewModel(repository) as T
            modelClass.isAssignableFrom(ReportsViewModel::class.java) -> ReportsViewModel(repository) as T
            modelClass.isAssignableFrom(BillHistoryViewModel::class.java) -> BillHistoryViewModel(repository) as T
            modelClass.isAssignableFrom(CustomerViewModel::class.java) -> CustomerViewModel(repository) as T
            modelClass.isAssignableFrom(MenuManagementViewModel::class.java) -> MenuManagementViewModel(repository) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(repository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
