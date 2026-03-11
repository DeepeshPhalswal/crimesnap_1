package com.example.crimesnap

import kotlinx.coroutines.flow.StateFlow

data class User(
    val id: String,
    val name: String?,
    val email: String?,
    val photoUrl: String?,
    val joinDate: String = "Oct 2023", // Default for now
    var reportsCount: Int = 0
)

interface AuthManager {
    val currentUser: StateFlow<User?>
    suspend fun signIn(): Result<User>
    suspend fun signOut()
    fun updateReportsCount(count: Int)
}

expect fun getAuthManager(): AuthManager
