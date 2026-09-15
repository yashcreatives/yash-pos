package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.entity.*
import com.example.data.repository.PosRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class ExpenseTallyUiState(
    val expenses: List<ExpenseEntity> = emptyList(),
    val todayCashSales: Double = 0.0,
    val todayUpiSales: Double = 0.0,
    val todayCardSales: Double = 0.0,
    val todayCreditSales: Double = 0.0,
    val openingCashInput: String = "2000.0",
    val otherCashIncomeInput: String = "0.0",
    val actualClosingCashInput: String = "",
    val showAddExpenseDialog: Boolean = false,
    val showShiftDialog: Boolean = false,
    val activeShift: ShiftEntity? = null,
    val toastMessage: String? = null
) {
    val totalExpenses: Double
        get() = expenses.sumOf { it.amount }

    val totalCashExpenses: Double
        get() = expenses.filter { it.paymentMethod == PaymentMethod.CASH }.sumOf { it.amount }

    val openingCash: Double
        get() = openingCashInput.toDoubleOrNull() ?: 0.0

    val otherIncome: Double
        get() = otherCashIncomeInput.toDoubleOrNull() ?: 0.0

    val expectedClosingCash: Double
        get() = (openingCash + todayCashSales + otherIncome - totalCashExpenses).coerceAtLeast(0.0)

    val actualClosingCash: Double?
        get() = actualClosingCashInput.toDoubleOrNull()

    val cashDifference: Double?
        get() = actualClosingCash?.let { it - expectedClosingCash }
}

class ExpenseTallyViewModel(private val repository: PosRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpenseTallyUiState())
    val uiState: StateFlow<ExpenseTallyUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        viewModelScope.launch {
            repository.allExpenses.collect { expList ->
                _uiState.value = _uiState.value.copy(expenses = expList)
            }
        }
        viewModelScope.launch {
            repository.allBills.collect { bills ->
                val valid = bills.filter { it.status == BillStatus.COMPLETED }
                val cash = valid.filter { it.paymentMethod == PaymentMethod.CASH }.sumOf { it.grandTotal }
                val upi = valid.filter { it.paymentMethod == PaymentMethod.UPI }.sumOf { it.grandTotal }
                val card = valid.filter { it.paymentMethod == PaymentMethod.CARD }.sumOf { it.grandTotal }
                val credit = valid.filter { it.paymentMethod == PaymentMethod.CREDIT }.sumOf { it.grandTotal }

                _uiState.value = _uiState.value.copy(
                    todayCashSales = cash,
                    todayUpiSales = upi,
                    todayCardSales = card,
                    todayCreditSales = credit
                )
            }
        }
    }

    fun onOpeningCashChange(value: String) {
        _uiState.value = _uiState.value.copy(openingCashInput = value)
    }

    fun onOtherIncomeChange(value: String) {
        _uiState.value = _uiState.value.copy(otherCashIncomeInput = value)
    }

    fun onActualCashChange(value: String) {
        _uiState.value = _uiState.value.copy(actualClosingCashInput = value)
    }

    fun openAddExpenseDialog() {
        _uiState.value = _uiState.value.copy(showAddExpenseDialog = true)
    }

    fun dismissAddExpenseDialog() {
        _uiState.value = _uiState.value.copy(showAddExpenseDialog = false)
    }

    fun addExpense(
        category: String,
        amount: Double,
        paymentMethod: PaymentMethod,
        description: String,
        currentUser: UserEntity?
    ) {
        viewModelScope.launch {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val exp = ExpenseEntity(
                expenseDate = todayStr,
                category = category,
                amount = amount,
                paymentMethod = paymentMethod,
                description = description,
                employeeName = currentUser?.fullName ?: "Staff",
                referenceNumber = "EXP-${System.currentTimeMillis() % 10000}"
            )
            repository.insertExpense(exp, currentUser)
            _uiState.value = _uiState.value.copy(
                showAddExpenseDialog = false,
                toastMessage = "Recorded expense of ₹$amount under $category"
            )
        }
    }

    fun deleteExpense(id: Long) {
        viewModelScope.launch {
            repository.deleteExpense(id)
            _uiState.value = _uiState.value.copy(toastMessage = "Expense removed")
        }
    }

    fun performDayClosing(currentUser: UserEntity?) {
        val state = _uiState.value
        val actual = state.actualClosingCash ?: 0.0
        val diff = state.cashDifference ?: 0.0

        viewModelScope.launch {
            repository.logAction(
                userId = currentUser?.id ?: 0,
                userName = currentUser?.fullName ?: "Manager",
                role = currentUser?.role?.name ?: "MANAGER",
                action = "DAY_CLOSING_PERFORMED",
                details = "Day closing completed: Expected Cash: ₹${state.expectedClosingCash}, Actual Cash: ₹$actual, Diff: ₹$diff"
            )

            val statusMsg = if (diff == 0.0) {
                "Perfect Match! Zero cash mismatch."
            } else if (diff > 0) {
                "Day closed with Cash Excess of +₹${String.format("%.2f", diff)}"
            } else {
                "Day closed with Cash Shortage of -₹${String.format("%.2f", -diff)}"
            }

            _uiState.value = _uiState.value.copy(
                toastMessage = "Day Closing Saved! $statusMsg"
            )
        }
    }

    fun clearToast() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }
}
