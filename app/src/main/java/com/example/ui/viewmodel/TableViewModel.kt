package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.entity.*
import com.example.data.repository.PosRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TableUiState(
    val floors: List<FloorSectionEntity> = emptyList(),
    val selectedFloorId: Long = 1L,
    val tables: List<RestaurantTableEntity> = emptyList(),
    val selectedTableForAction: RestaurantTableEntity? = null,
    val showAddEditTableDialog: Boolean = false,
    val editingTable: RestaurantTableEntity? = null,
    val showTransferTableDialog: Boolean = false,
    val showReserveTableDialog: Boolean = false,
    val toastMessage: String? = null
)

class TableViewModel(private val repository: PosRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(TableUiState())
    val uiState: StateFlow<TableUiState> = _uiState.asStateFlow()

    init {
        loadFloorsAndTables()
    }

    private fun loadFloorsAndTables() {
        viewModelScope.launch {
            repository.allFloors.collect { floorsList ->
                _uiState.value = _uiState.value.copy(floors = floorsList)
                if (floorsList.isNotEmpty() && _uiState.value.selectedFloorId == 0L) {
                    _uiState.value = _uiState.value.copy(selectedFloorId = floorsList.first().id)
                }
            }
        }
        viewModelScope.launch {
            repository.allTables.collect { tablesList ->
                _uiState.value = _uiState.value.copy(tables = tablesList)
            }
        }
    }

    fun selectFloor(floorId: Long) {
        _uiState.value = _uiState.value.copy(selectedFloorId = floorId)
    }

    fun openTableAction(table: RestaurantTableEntity) {
        _uiState.value = _uiState.value.copy(selectedTableForAction = table)
    }

    fun dismissTableAction() {
        _uiState.value = _uiState.value.copy(selectedTableForAction = null)
    }

    fun setTableStatus(tableId: Long, status: TableStatus) {
        viewModelScope.launch {
            repository.updateTableStatus(tableId, status)
            _uiState.value = _uiState.value.copy(
                selectedTableForAction = null,
                toastMessage = "Table status updated to ${status.name}"
            )
        }
    }

    fun markTableAvailable(tableId: Long) {
        viewModelScope.launch {
            repository.updateTableStatus(tableId, TableStatus.AVAILABLE, 0L, 0.0, 0L)
            _uiState.value = _uiState.value.copy(
                selectedTableForAction = null,
                toastMessage = "Table marked Available and ready for guests"
            )
        }
    }

    fun markTableCleaning(tableId: Long) {
        viewModelScope.launch {
            repository.updateTableStatus(tableId, TableStatus.CLEANING)
            _uiState.value = _uiState.value.copy(
                selectedTableForAction = null,
                toastMessage = "Table marked for Cleaning"
            )
        }
    }

    fun reserveTable(tableId: Long, customerName: String, customerPhone: String, time: String) {
        viewModelScope.launch {
            val table = repository.getTableById(tableId)
            if (table != null) {
                repository.updateTable(
                    table.copy(
                        status = TableStatus.RESERVED,
                        reservedCustomerName = customerName,
                        reservedCustomerPhone = customerPhone,
                        reservedTime = time
                    )
                )
                _uiState.value = _uiState.value.copy(
                    showReserveTableDialog = false,
                    selectedTableForAction = null,
                    toastMessage = "Table ${table.tableNumber} reserved for $customerName at $time"
                )
            }
        }
    }

    fun transferTable(fromTableId: Long, toTableId: Long) {
        viewModelScope.launch {
            val fromTable = repository.getTableById(fromTableId)
            val toTable = repository.getTableById(toTableId)
            if (fromTable != null && toTable != null) {
                // Move order & status to new table
                repository.updateTable(
                    toTable.copy(
                        status = fromTable.status,
                        currentOrderId = fromTable.currentOrderId,
                        currentOrderAmount = fromTable.currentOrderAmount,
                        occupiedSinceTimestamp = fromTable.occupiedSinceTimestamp,
                        guestCount = fromTable.guestCount
                    )
                )
                // Clear old table
                repository.updateTable(
                    fromTable.copy(
                        status = TableStatus.AVAILABLE,
                        currentOrderId = 0L,
                        currentOrderAmount = 0.0,
                        occupiedSinceTimestamp = 0L,
                        guestCount = 0
                    )
                )
                _uiState.value = _uiState.value.copy(
                    showTransferTableDialog = false,
                    selectedTableForAction = null,
                    toastMessage = "Transferred Order from ${fromTable.tableNumber} to ${toTable.tableNumber}"
                )
            }
        }
    }

    fun saveTable(tableNumber: String, capacity: Int, floorId: Long, editingId: Long = 0L) {
        viewModelScope.launch {
            if (editingId > 0L) {
                val existing = repository.getTableById(editingId)
                if (existing != null) {
                    repository.updateTable(
                        existing.copy(
                            tableNumber = tableNumber,
                            capacity = capacity,
                            floorId = floorId
                        )
                    )
                }
            } else {
                repository.insertTable(
                    RestaurantTableEntity(
                        tableNumber = tableNumber,
                        capacity = capacity,
                        floorId = floorId,
                        status = TableStatus.AVAILABLE
                    )
                )
            }
            _uiState.value = _uiState.value.copy(
                showAddEditTableDialog = false,
                editingTable = null,
                toastMessage = "Table $tableNumber saved successfully"
            )
        }
    }

    fun deleteTable(tableId: Long) {
        viewModelScope.launch {
            repository.deleteTable(tableId)
            _uiState.value = _uiState.value.copy(
                showAddEditTableDialog = false,
                editingTable = null,
                toastMessage = "Table deleted"
            )
        }
    }

    fun openAddTable() {
        _uiState.value = _uiState.value.copy(showAddEditTableDialog = true, editingTable = null)
    }

    fun openEditTable(table: RestaurantTableEntity) {
        _uiState.value = _uiState.value.copy(showAddEditTableDialog = true, editingTable = table)
    }

    fun dismissAddEditTable() {
        _uiState.value = _uiState.value.copy(showAddEditTableDialog = false, editingTable = null)
    }

    fun openReserveDialog(table: RestaurantTableEntity) {
        _uiState.value = _uiState.value.copy(showReserveTableDialog = true, selectedTableForAction = table)
    }

    fun dismissReserveDialog() {
        _uiState.value = _uiState.value.copy(showReserveTableDialog = false)
    }

    fun openTransferDialog(table: RestaurantTableEntity) {
        _uiState.value = _uiState.value.copy(showTransferTableDialog = true, selectedTableForAction = table)
    }

    fun dismissTransferDialog() {
        _uiState.value = _uiState.value.copy(showTransferTableDialog = false)
    }

    fun clearToast() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }
}
