package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.entity.KotEntity
import com.example.data.entity.KotPriority
import com.example.data.entity.KotStatus
import com.example.data.repository.PosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class KdsUiState(
    val activeKots: List<KotEntity> = emptyList(),
    val filterPriority: KotPriority? = null,
    val toastMessage: String? = null
) {
    val newKots: List<KotEntity>
        get() = activeKots.filter { it.status == KotStatus.NEW }

    val preparingKots: List<KotEntity>
        get() = activeKots.filter { it.status == KotStatus.PREPARING || it.status == KotStatus.ACCEPTED }

    val readyKots: List<KotEntity>
        get() = activeKots.filter { it.status == KotStatus.READY }
}

class KdsViewModel(private val repository: PosRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(KdsUiState())
    val uiState: StateFlow<KdsUiState> = _uiState.asStateFlow()

    init {
        loadKots()
    }

    private fun loadKots() {
        viewModelScope.launch {
            repository.activeKots.collect { kots ->
                _uiState.value = _uiState.value.copy(activeKots = kots)
            }
        }
    }

    fun advanceKotStatus(kot: KotEntity) {
        viewModelScope.launch {
            val nextStatus = when (kot.status) {
                KotStatus.NEW -> KotStatus.PREPARING
                KotStatus.ACCEPTED -> KotStatus.PREPARING
                KotStatus.PREPARING -> KotStatus.READY
                KotStatus.READY -> KotStatus.SERVED
                KotStatus.SERVED -> KotStatus.SERVED
            }

            val updated = when (nextStatus) {
                KotStatus.PREPARING -> kot.copy(status = nextStatus, acceptedAt = System.currentTimeMillis())
                KotStatus.READY -> kot.copy(status = nextStatus, readyAt = System.currentTimeMillis())
                else -> kot.copy(status = nextStatus)
            }

            repository.updateKot(updated)
            _uiState.value = _uiState.value.copy(
                toastMessage = "KOT ${kot.kotNumber} moved to ${nextStatus.name}"
            )
        }
    }

    fun recallKot(kot: KotEntity) {
        viewModelScope.launch {
            val prevStatus = when (kot.status) {
                KotStatus.READY -> KotStatus.PREPARING
                KotStatus.PREPARING -> KotStatus.NEW
                else -> KotStatus.NEW
            }
            repository.updateKotStatus(kot.id, prevStatus)
        }
    }

    fun clearToast() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }
}
