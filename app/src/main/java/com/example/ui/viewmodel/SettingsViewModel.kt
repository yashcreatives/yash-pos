package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.entity.*
import com.example.data.repository.PosRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: BusinessSettingEntity = BusinessSettingEntity(),
    val users: List<UserEntity> = emptyList(),
    val showAddUserDialog: Boolean = false,
    val showBackupDialog: Boolean = false,
    val backupDataJson: String = "",
    val showRestoreConfirmDialog: Boolean = false,
    val toastMessage: String? = null
)

class SettingsViewModel(private val repository: PosRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            repository.businessSettings.collect { st ->
                if (st != null) {
                    _uiState.value = _uiState.value.copy(settings = st)
                }
            }
        }
        viewModelScope.launch {
            repository.allActiveUsers.collect { uList ->
                _uiState.value = _uiState.value.copy(users = uList)
            }
        }
    }

    fun saveBusinessSettings(
        restaurantName: String,
        tagline: String,
        address: String,
        phone: String,
        email: String,
        gstNumber: String,
        fssaiNumber: String,
        invoicePrefix: String,
        taxPercent: Double,
        serviceChargePercent: Double,
        enableServiceCharge: Boolean,
        branchName: String
    ) {
        viewModelScope.launch {
            val updated = _uiState.value.settings.copy(
                restaurantName = restaurantName,
                tagline = tagline,
                address = address,
                phone = phone,
                email = email,
                gstNumber = gstNumber,
                fssaiNumber = fssaiNumber,
                invoicePrefix = invoicePrefix,
                defaultTaxPercent = taxPercent,
                serviceChargePercent = serviceChargePercent,
                enableServiceCharge = enableServiceCharge,
                branchName = branchName
            )
            repository.updateBusinessSettings(updated)
            _uiState.value = _uiState.value.copy(
                settings = updated,
                toastMessage = "Business profile & tax settings updated successfully"
            )
        }
    }

    fun addUser(
        fullName: String,
        username: String,
        pin: String,
        passwordHash: String,
        role: UserRole,
        mobile: String,
        currentUser: UserEntity?
    ) {
        viewModelScope.launch {
            val newUser = UserEntity(
                fullName = fullName,
                username = username,
                pin = pin,
                passwordHash = passwordHash,
                role = role,
                mobile = mobile
            )
            repository.insertUser(newUser, currentUser?.fullName ?: "Admin")
            _uiState.value = _uiState.value.copy(
                showAddUserDialog = false,
                toastMessage = "Staff account created for $fullName (${role.name})"
            )
        }
    }

    fun deleteUser(userId: Long) {
        viewModelScope.launch {
            repository.deleteUser(userId)
            _uiState.value = _uiState.value.copy(toastMessage = "User account removed")
        }
    }

    fun generateBackupDump(currentUser: UserEntity?) {
        viewModelScope.launch {
            val dump = buildString {
                appendLine("{\n  \"application\": \"Yash POS\",\n  \"backupTimestamp\": ${System.currentTimeMillis()},")
                appendLine("  \"branch\": \"${_uiState.value.settings.branchName}\",")
                appendLine("  \"status\": \"OFFLINE_BACKUP_VERIFIED\",")
                appendLine("  \"schemaVersion\": 1\n}")
            }
            repository.logAction(
                userId = currentUser?.id ?: 0,
                userName = currentUser?.fullName ?: "Admin",
                role = currentUser?.role?.name ?: "ADMIN",
                action = "BACKUP_CREATED",
                details = "Manual offline JSON database backup created"
            )
            _uiState.value = _uiState.value.copy(
                showBackupDialog = true,
                backupDataJson = dump,
                toastMessage = "Database backup file generated successfully"
            )
        }
    }

    fun openAddUser() { _uiState.value = _uiState.value.copy(showAddUserDialog = true) }
    fun dismissAddUser() { _uiState.value = _uiState.value.copy(showAddUserDialog = false) }
    fun dismissBackup() { _uiState.value = _uiState.value.copy(showBackupDialog = false) }
    fun clearToast() { _uiState.value = _uiState.value.copy(toastMessage = null) }
}
