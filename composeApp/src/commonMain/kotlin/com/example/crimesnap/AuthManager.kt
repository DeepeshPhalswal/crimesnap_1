package com.example.crimesnap

import kotlinx.coroutines.flow.StateFlow

data class User(
    val id: String,
    val name: String?,
    val email: String?,
    val photoUrl: String?,
    val joinDate: String = "Oct 2023",
    val reportsCount: Int = 0
)

expect class AuthManager {
    val currentUser: StateFlow<User?>
    suspend fun signIn(): Result<User>
    suspend fun signOut()
}

expect fun getAuthManager(): AuthManager
