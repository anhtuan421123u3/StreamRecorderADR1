package com.livevault.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livevault.core.common.Result
import com.livevault.core.network.dto.LoginRequest
import com.livevault.core.network.dto.RegisterRequest
import com.livevault.core.network.dto.UserProfileDto
import com.livevault.core.network.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val user: UserProfileDto? = null
)

sealed interface AuthEvent {
    object NavigateToHome : AuthEvent
    data class ShowToast(val message: String) : AuthEvent
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<AuthEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter email and password")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = authRepository.login(LoginRequest(email.trim(), pass))) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, user = result.data.user)
                    _eventFlow.emit(AuthEvent.NavigateToHome)
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.throwable.message ?: "Login failed"
                    )
                }
                is Result.Loading -> {}
            }
        }
    }

    fun register(email: String, pass: String, displayName: String?) {
        if (email.isBlank() || pass.length < 6) {
            _uiState.value = _uiState.value.copy(errorMessage = "Password must be at least 6 characters")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = authRepository.register(RegisterRequest(email.trim(), pass, displayName?.trim()))) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, user = result.data.user)
                    _eventFlow.emit(AuthEvent.NavigateToHome)
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.throwable.message ?: "Registration failed"
                    )
                }
                is Result.Loading -> {}
            }
        }
    }

    fun googleSignIn(idToken: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = authRepository.googleSignIn(idToken)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, user = result.data.user)
                    _eventFlow.emit(AuthEvent.NavigateToHome)
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.throwable.message ?: "Google sign-in failed"
                    )
                }
                is Result.Loading -> {}
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
