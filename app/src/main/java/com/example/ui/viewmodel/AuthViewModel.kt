package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.entity.UserEntity
import com.example.data.entity.UserRole
import com.example.data.repository.PosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val currentUser: UserEntity? = null,
    val isLoggedIn: Boolean = false,
    val isLocked: Boolean = false,
    val identifierInput: String = "",
    val pinOrPasswordInput: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showLogoutDialog: Boolean = false
)

class AuthViewModel(private val repository: PosRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onIdentifierChange(value: String) {
        _uiState.value = _uiState.value.copy(identifierInput = value, errorMessage = null)
    }

    fun onPinOrPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(pinOrPasswordInput = value, errorMessage = null)
    }

    fun appendPinDigit(digit: String) {
        val current = _uiState.value.pinOrPasswordInput
        if (current.length < 8) {
            val updated = current + digit
            _uiState.value = _uiState.value.copy(pinOrPasswordInput = updated, errorMessage = null)
            // If 4 digits entered, try instant PIN login if no custom username entered
            if (updated.length == 4 && _uiState.value.identifierInput.isEmpty()) {
                loginWithPin(updated)
            }
        }
    }

    fun clearPinDigit() {
        val current = _uiState.value.pinOrPasswordInput
        if (current.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(pinOrPasswordInput = current.dropLast(1), errorMessage = null)
        }
    }

    fun clearAllPin() {
        _uiState.value = _uiState.value.copy(pinOrPasswordInput = "", errorMessage = null)
    }

    fun login() {
        val id = _uiState.value.identifierInput.trim()
        val secret = _uiState.value.pinOrPasswordInput.trim()

        if (secret.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter your PIN or Password")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val user = if (id.isEmpty()) {
                repository.authenticate("owner", secret) ?: repository.authenticate("manager", secret) ?: repository.authenticate("cashier", secret) ?: repository.authenticate("admin", secret)
            } else {
                repository.authenticate(id, secret)
            }

            if (user != null) {
                _uiState.value = _uiState.value.copy(
                    currentUser = user,
                    isLoggedIn = true,
                    isLocked = false,
                    isLoading = false,
                    identifierInput = "",
                    pinOrPasswordInput = "",
                    errorMessage = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Invalid Mobile/Username or PIN/Password"
                )
            }
        }
    }

    fun loginWithPin(pin: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val user = repository.authenticate("", pin)
            if (user != null) {
                _uiState.value = _uiState.value.copy(
                    currentUser = user,
                    isLoggedIn = true,
                    isLocked = false,
                    isLoading = false,
                    pinOrPasswordInput = "",
                    identifierInput = "",
                    errorMessage = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Incorrect PIN code"
                )
            }
        }
    }

    fun quickDemoLogin(role: UserRole) {
        val (username, pin) = when (role) {
            UserRole.OWNER -> "owner" to "1111"
            UserRole.MANAGER -> "manager" to "2222"
            UserRole.CASHIER -> "cashier" to "3333"
            UserRole.ADMIN -> "admin" to "4444"
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val user = repository.authenticate(username, pin)
            if (user != null) {
                _uiState.value = _uiState.value.copy(
                    currentUser = user,
                    isLoggedIn = true,
                    isLocked = false,
                    isLoading = false,
                    identifierInput = "",
                    pinOrPasswordInput = "",
                    errorMessage = null
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Demo user not found")
            }
        }
    }

    fun promptLogout() {
        _uiState.value = _uiState.value.copy(showLogoutDialog = true)
    }

    fun dismissLogout() {
        _uiState.value = _uiState.value.copy(showLogoutDialog = false)
    }

    fun confirmLogout() {
        val user = _uiState.value.currentUser
        viewModelScope.launch {
            if (user != null) {
                repository.logAction(user.id, user.fullName, user.role.name, "LOGOUT", "User logged out")
            }
            _uiState.value = AuthUiState()
        }
    }

    fun lockScreen() {
        _uiState.value = _uiState.value.copy(isLocked = true, pinOrPasswordInput = "")
    }

    fun unlockScreen(pin: String): Boolean {
        val current = _uiState.value.currentUser
        return if (current != null && (current.pin == pin || current.passwordHash == pin)) {
            _uiState.value = _uiState.value.copy(isLocked = false, pinOrPasswordInput = "")
            true
        } else {
            false
        }
    }
}
