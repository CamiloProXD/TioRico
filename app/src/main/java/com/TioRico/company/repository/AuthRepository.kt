package com.TioRico.company.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

class AuthRepository(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) {

    private val _currentUser = MutableStateFlow(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser

    init {
        auth.addAuthStateListener {
            _currentUser.value = it.currentUser
        }
    }

    fun signOut() {
        auth.signOut()
    }

    suspend fun signInWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    suspend fun signUpWithEmail(email: String, password: String, name: String) {
        auth.createUserWithEmailAndPassword(email, password).await()
        updateProfile(name)
    }

    private suspend fun updateProfile(name: String) {
        val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
            displayName = name
        }
        auth.currentUser?.updateProfile(profileUpdates)?.await()
    }

    suspend fun signInWithGoogle(idToken: String) {
        val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).await()
    }

    val isUserLoggedIn: Boolean
        get() = auth.currentUser != null

    fun getCurrentUserId(): String? = auth.currentUser?.uid
    fun getCurrentUserName(): String = auth.currentUser?.displayName ?: "Jugador"
    fun getCurrentUserEmail(): String? = auth.currentUser?.email
}
