package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.entity.*
import com.example.data.repository.PosRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class BillHistoryUiState(
    val bills: List<BillEntity> = emptyList(),
    val auditLogs: List<AuditLogEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedBillForDetail: BillEntity? = null,
    val showRefundDialog: Boolean = false,
    val refundBill: BillEntity? = null,
    val refundReasonInput: String = "",
    val authPinInput: String = "",
    val showAuditLogs: Boolean = false,
    val toastMessage: String? = null
) {
    val filteredBills: List<BillEntity>
        get() {
            if (searchQuery.isBlank()) return bills
            val q = searchQuery.trim().lowercase()
            return bills.filter {
                it.invoiceNumber.lowercase().contains(q) ||
                        it.customerName.lowercase().contains(q) ||
                        it.customerPhone.contains(q) ||
                        it.tableName.lowercase().contains(q) ||
                        it.cashierName.lowercase().contains(q)
            }
        }
}

class BillHistoryViewModel(private val repository: PosRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(BillHistoryUiState())
    val uiState: StateFlow<BillHistoryUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            repository.allBills.collect { billsList ->
                _uiState.value = _uiState.value.copy(bills = billsList)
            }
        }
        viewModelScope.launch {
            repository.recentAuditLogs.collect { logs ->
                _uiState.value = _uiState.value.copy(auditLogs = logs)
            }
        }
    }

    fun onSearchChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun selectBill(bill: BillEntity) {
        _uiState.value = _uiState.value.copy(selectedBillForDetail = bill)
    }

    fun dismissDetail() {
        _uiState.value = _uiState.value.copy(selectedBillForDetail = null)
    }

    fun openRefundDialog(bill: BillEntity) {
        _uiState.value = _uiState.value.copy(
            showRefundDialog = true,
            refundBill = bill,
            refundReasonInput = "",
            authPinInput = ""
        )
    }

    fun dismissRefundDialog() {
        _uiState.value = _uiState.value.copy(showRefundDialog = false, refundBill = null)
    }

    fun onRefundReasonChange(reason: String) {
        _uiState.value = _uiState.value.copy(refundReasonInput = reason)
    }

    fun onAuthPinChange(pin: String) {
        _uiState.value = _uiState.value.copy(authPinInput = pin)
    }

    fun processRefund(currentUser: UserEntity, isVoid: Boolean = false) {
        val bill = _uiState.value.refundBill ?: return
        val pin = _uiState.value.authPinInput.trim()
        val reason = _uiState.value.refundReasonInput.trim()

        viewModelScope.launch {
            // Check authorization: Owner or Manager can authorize or if cashier entered owner's PIN
            val isAuthorized = (currentUser.role == UserRole.OWNER || currentUser.role == UserRole.ADMIN || currentUser.role == UserRole.MANAGER) ||
                    (pin == "1111" || pin == "4444" || pin == "2222")

            if (!isAuthorized) {
                _uiState.value = _uiState.value.copy(toastMessage = "Authorization Failed: Invalid Owner PIN")
                return@launch
            }

            val action = if (isVoid) BillStatus.VOIDED else BillStatus.REFUNDED
            val authName = if (pin == "1111" || currentUser.role == UserRole.OWNER) "Owner (Yash Demo)" else currentUser.fullName

            repository.refundOrCancelBill(
                billId = bill.id,
                action = action,
                reason = reason.ifEmpty { "Customer request / Return" },
                refundAmount = bill.grandTotal,
                authorizedByName = authName,
                currentUser = currentUser
            )

            _uiState.value = _uiState.value.copy(
                showRefundDialog = false,
                refundBill = null,
                selectedBillForDetail = null,
                toastMessage = "Bill ${bill.invoiceNumber} has been ${action.name} (₹${bill.grandTotal})"
            )
        }
    }

    fun toggleAuditLogsView() {
        _uiState.value = _uiState.value.copy(showAuditLogs = !_uiState.value.showAuditLogs)
    }

    fun clearToast() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }
}
