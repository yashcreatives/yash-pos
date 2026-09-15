package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.entity.*
import com.example.data.repository.PosRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

enum class ReportDateFilter(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    ALL_TIME("All Time")
}

data class PaymentDistribution(
    val method: PaymentMethod,
    val totalAmount: Double,
    val count: Int,
    val percentage: Float
)

data class HourlySalesPoint(
    val hourLabel: String,
    val amount: Double
)

data class PrecomputedReportSummary(
    val validBills: List<BillEntity> = emptyList(),
    val grossSales: Double = 0.0,
    val totalDiscounts: Double = 0.0,
    val totalTax: Double = 0.0,
    val netSales: Double = 0.0,
    val totalBillsCount: Int = 0,
    val avgBillValue: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val totalPurchases: Double = 0.0,
    val estimatedFoodCost: Double = 0.0,
    val estimatedProfit: Double = 0.0,
    val profitMarginPercent: Double = 0.0,
    val paymentDistributions: List<PaymentDistribution> = emptyList(),
    val hourlyTrend: List<HourlySalesPoint> = emptyList()
)

data class ReportsUiState(
    val selectedFilter: ReportDateFilter = ReportDateFilter.THIS_MONTH,
    val isLoading: Boolean = false,
    val summary: PrecomputedReportSummary = PrecomputedReportSummary(),
    val showExportDialog: Boolean = false,
    val exportContent: String = "",
    val exportTitle: String = "",
    val toastMessage: String? = null
) {
    // Direct getters from precomputed values
    val validBills: List<BillEntity> get() = summary.validBills
    val grossSales: Double get() = summary.grossSales
    val totalDiscounts: Double get() = summary.totalDiscounts
    val totalTax: Double get() = summary.totalTax
    val netSales: Double get() = summary.netSales
    val totalBillsCount: Int get() = summary.totalBillsCount
    val avgBillValue: Double get() = summary.avgBillValue
    val totalExpenses: Double get() = summary.totalExpenses
    val totalPurchases: Double get() = summary.totalPurchases
    val estimatedFoodCost: Double get() = summary.estimatedFoodCost
    val estimatedProfit: Double get() = summary.estimatedProfit
    val profitMarginPercent: Double get() = summary.profitMarginPercent
    val paymentDistributions: List<PaymentDistribution> get() = summary.paymentDistributions
    val hourlyTrend: List<HourlySalesPoint> get() = summary.hourlyTrend
}

class ReportsViewModel(private val repository: PosRepository) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(ReportDateFilter.THIS_MONTH)
    private val _rawBills = MutableStateFlow<List<BillEntity>>(emptyList())
    private val _rawExpenses = MutableStateFlow<List<ExpenseEntity>>(emptyList())
    private val _rawPurchases = MutableStateFlow<List<PurchaseEntity>>(emptyList())

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init {
        observeDataSources()
        setupCalculationPipeline()
    }

    private fun observeDataSources() {
        viewModelScope.launch {
            repository.allBills.collect { bills ->
                _rawBills.value = bills
            }
        }
        viewModelScope.launch {
            repository.allExpenses.collect { exp ->
                _rawExpenses.value = exp
            }
        }
        viewModelScope.launch {
            repository.allPurchases.collect { pur ->
                _rawPurchases.value = pur
            }
        }
    }

    private fun setupCalculationPipeline() {
        viewModelScope.launch {
            combine(
                _selectedFilter,
                _rawBills,
                _rawExpenses,
                _rawPurchases
            ) { filter, bills, expenses, purchases ->
                calculateReportSummary(filter, bills, expenses, purchases)
            }.flowOn(Dispatchers.Default).collect { (filter, summary) ->
                _uiState.value = _uiState.value.copy(
                    selectedFilter = filter,
                    summary = summary,
                    isLoading = false
                )
            }
        }
    }

    private fun calculateReportSummary(
        filter: ReportDateFilter,
        bills: List<BillEntity>,
        expenses: List<ExpenseEntity>,
        purchases: List<PurchaseEntity>
    ): Pair<ReportDateFilter, PrecomputedReportSummary> {
        val cal = Calendar.getInstance()
        val (startTime, endTime) = when (filter) {
            ReportDateFilter.TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                Pair(cal.timeInMillis, Long.MAX_VALUE)
            }
            ReportDateFilter.YESTERDAY -> {
                cal.add(Calendar.DAY_OF_YEAR, -1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                val end = cal.timeInMillis
                Pair(start, end)
            }
            ReportDateFilter.THIS_WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                Pair(cal.timeInMillis, Long.MAX_VALUE)
            }
            ReportDateFilter.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                Pair(cal.timeInMillis, Long.MAX_VALUE)
            }
            ReportDateFilter.ALL_TIME -> Pair(0L, Long.MAX_VALUE)
        }

        val filteredBills = bills.filter { it.createdAt in startTime..endTime }
        val validBills = filteredBills.filter { it.status == BillStatus.COMPLETED }
        val filteredExpenses = expenses.filter { it.createdAt in startTime..endTime }
        val filteredPurchases = purchases.filter { it.createdAt in startTime..endTime }

        val gross = validBills.sumOf { it.subtotal }
        val discounts = validBills.sumOf { it.discountAmount }
        val tax = validBills.sumOf { it.taxAmount }
        val net = validBills.sumOf { it.grandTotal }
        val count = validBills.size
        val avgVal = if (count > 0) net / count else 0.0

        val totalExp = filteredExpenses.sumOf { it.amount }
        val totalPur = filteredPurchases.sumOf { it.totalAmount }
        val foodCost = net * 0.32
        val profit = (net - (foodCost + totalExp + totalPur)).coerceAtLeast(-50000.0)
        val margin = if (net > 0) (profit / net) * 100.0 else 0.0

        // Payment distribution
        val paymentGroups = validBills.groupBy { it.paymentMethod }
        val paymentDist = PaymentMethod.values().map { method ->
            val list = paymentGroups[method] ?: emptyList()
            val totalAmt = list.sumOf { it.grandTotal }
            PaymentDistribution(
                method = method,
                totalAmount = totalAmt,
                count = list.size,
                percentage = if (net > 0) ((totalAmt / net) * 100).toFloat() else 0f
            )
        }

        // Hourly trend
        val hourlyBuckets = mutableMapOf<Int, Double>()
        for (i in 8..23) {
            hourlyBuckets[i] = 0.0
        }
        val calItem = Calendar.getInstance()
        for (bill in validBills) {
            calItem.timeInMillis = bill.createdAt
            val h = calItem.get(Calendar.HOUR_OF_DAY)
            if (h in 8..23) {
                hourlyBuckets[h] = (hourlyBuckets[h] ?: 0.0) + bill.grandTotal
            }
        }
        val hourlyTrend = hourlyBuckets.map { (h, amt) ->
            val label = if (h == 12) "12P" else if (h > 12) "${h - 12}P" else "${h}A"
            HourlySalesPoint(label, amt)
        }

        val summary = PrecomputedReportSummary(
            validBills = validBills,
            grossSales = gross,
            totalDiscounts = discounts,
            totalTax = tax,
            netSales = net,
            totalBillsCount = count,
            avgBillValue = avgVal,
            totalExpenses = totalExp,
            totalPurchases = totalPur,
            estimatedFoodCost = foodCost,
            estimatedProfit = profit,
            profitMarginPercent = margin,
            paymentDistributions = paymentDist,
            hourlyTrend = hourlyTrend
        )

        return Pair(filter, summary)
    }

    fun selectFilter(filter: ReportDateFilter) {
        _selectedFilter.value = filter
    }

    fun exportSalesCsv() {
        viewModelScope.launch {
            val state = _uiState.value
            val csvText = withContext(Dispatchers.Default) {
                val sb = StringBuilder()
                sb.appendLine("Invoice Number,Date,Time,Order Type,Table,Customer,Cashier,Subtotal,Discount,Tax,Grand Total,Payment Method,Status")
                val sdf = SimpleDateFormat("yyyy-MM-dd,HH:mm:ss", Locale.getDefault())
                for (bill in state.validBills) {
                    val dateStr = sdf.format(Date(bill.createdAt))
                    sb.appendLine("${bill.invoiceNumber},$dateStr,${bill.orderType.name},${bill.tableName},\"${bill.customerName}\",\"${bill.cashierName}\",${bill.subtotal},${bill.discountAmount},${bill.taxAmount},${bill.grandTotal},${bill.paymentMethod.name},${bill.status.name}")
                }
                sb.toString()
            }
            _uiState.value = _uiState.value.copy(
                showExportDialog = true,
                exportTitle = "Sales Report CSV Export",
                exportContent = csvText,
                toastMessage = "Sales Report exported (${state.validBills.size} bills)"
            )
        }
    }

    fun exportTallyFormat() {
        viewModelScope.launch {
            val state = _uiState.value
            val xmlText = withContext(Dispatchers.Default) {
                val sb = StringBuilder()
                sb.appendLine("<!-- TALLY ERP 9 / PRIME COMPATIBLE SALES VOUCHER DUMP -->")
                sb.appendLine("<ENVELOPE>")
                sb.appendLine("  <HEADER><TALLYREQUEST>Import Data</TALLYREQUEST></HEADER>")
                sb.appendLine("  <BODY><IMPORTDATA><REQUESTDATA>")
                val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                for (bill in state.validBills) {
                    val dateFormatted = sdf.format(Date(bill.createdAt))
                    sb.appendLine("    <TALLYMESSAGE xmlns:UDF=\"TallyUDF\">")
                    sb.appendLine("      <VOUCHER VCHTYPE=\"Sales\" ACTION=\"Create\">")
                    sb.appendLine("        <DATE>$dateFormatted</DATE>")
                    sb.appendLine("        <VOUCHERNUMBER>${bill.invoiceNumber}</VOUCHERNUMBER>")
                    sb.appendLine("        <PARTYLEDGERNAME>${if (bill.paymentMethod == PaymentMethod.CASH) "Cash-in-Hand" else "Bank Account / UPI"}</PARTYLEDGERNAME>")
                    sb.appendLine("        <AMOUNT>-${bill.grandTotal}</AMOUNT>")
                    sb.appendLine("        <NARRATION>POS Bill for ${bill.customerName} (${bill.orderType})</NARRATION>")
                    sb.appendLine("      </VOUCHER>")
                    sb.appendLine("    </TALLYMESSAGE>")
                }
                sb.appendLine("  </REQUESTDATA></IMPORTDATA></BODY>")
                sb.appendLine("</ENVELOPE>")
                sb.toString()
            }
            _uiState.value = _uiState.value.copy(
                showExportDialog = true,
                exportTitle = "Tally Accounting Export (XML)",
                exportContent = xmlText,
                toastMessage = "Tally XML generated successfully"
            )
        }
    }

    fun dismissExport() {
        _uiState.value = _uiState.value.copy(showExportDialog = false)
    }

    fun clearToast() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }
}
