package com.example.crimesnap

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AuthViewModel(private val authManager: AuthManager) {
    val currentUser: StateFlow<User?> = authManager.currentUser

    fun signOut() {
        // Use a basic CoroutineScope for demonstration. 
        // In a real app, use a proper viewModelScope if available.
        CoroutineScope(Dispatchers.Main).launch {
            authManager.signOut()
        }
    }
}
