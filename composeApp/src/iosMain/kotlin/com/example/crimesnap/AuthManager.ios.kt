package com.example.crimesnap

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class IosAuthManager : AuthManager {
    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    override suspend fun signIn(): Result<User> {
        val mockUser = User(
            id = "google_ios_123",
            name = "iOS Demo User",
            email = "ios@example.com",
            photoUrl = null,
            joinDate = "Oct 2023"
        )
        _currentUser.value = mockUser
        return Result.success(mockUser)
    }

    override suspend fun signOut() {
        _currentUser.value = null
    }

    override fun updateReportsCount(count: Int) {
        _currentUser.update { user ->
            user?.copy(reportsCount = count)
        }
    }
}

actual fun getAuthManager(): AuthManager = IosAuthManager()
