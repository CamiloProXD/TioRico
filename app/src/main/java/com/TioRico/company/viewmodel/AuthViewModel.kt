package com.TioRico.company.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.TioRico.company.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: AuthRepository = AuthRepository()) : ViewModel() {

    val currentUser: StateFlow<FirebaseUser?> = repository.currentUser

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun signIn(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                repository.signInWithEmail(email, password)
                onSuccess()
            } catch (e: Exception) {
                _error.value = "Error al iniciar sesión: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun signUp(email: String, password: String, name: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                repository.signUpWithEmail(email, password, name)
                onSuccess()
            } catch (e: Exception) {
                _error.value = "Error al registrarse: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun signInWithGoogle(idToken: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                repository.signInWithGoogle(idToken)
                onSuccess()
            } catch (e: Exception) {
                _error.value = "Error con Google: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
        }
    }

    fun getCurrentUserId(): String? = repository.getCurrentUserId()
    fun getCurrentUserName(): String = repository.getCurrentUserName()
}