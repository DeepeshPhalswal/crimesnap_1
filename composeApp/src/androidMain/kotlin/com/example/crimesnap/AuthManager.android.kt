package com.example.crimesnap

import android.app.Activity
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await

class AndroidAuthManager : AuthManager {
    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private fun getGoogleSignInClient() = GoogleSignIn.getClient(
        AndroidPlatform.currentActivity!!,
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .build()
    )

    init {
        // Check for existing user
        AndroidPlatform.appContext?.let { context ->
            GoogleSignIn.getLastSignedInAccount(context)?.let { account ->
                _currentUser.value = account.toUser()
            }
        }
    }

    override suspend fun signIn(): Result<User> {
        val activity = AndroidPlatform.currentActivity ?: return Result.failure(Exception("Activity not found"))
        val client = getGoogleSignInClient()
        
        return try {
            val intent = client.signInIntent
            activity.startActivityForResult(intent, RC_SIGN_IN)
            Result.failure(Exception("SIGN_IN_PENDING")) 
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        getGoogleSignInClient().signOut().await()
        _currentUser.value = null
    }

    override fun updateReportsCount(count: Int) {
        _currentUser.update { user ->
            user?.copy(reportsCount = count)
        }
    }

    fun handleSignInResult(data: Intent?) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            _currentUser.value = account.toUser()
        } catch (e: ApiException) {
            _currentUser.value = null
        }
    }

    private fun GoogleSignInAccount.toUser() = User(
        id = id ?: "",
        name = displayName,
        email = email,
        photoUrl = photoUrl?.toString(),
        joinDate = "Oct 2023" // In real app, fetch from backend
    )

    companion object {
        const val RC_SIGN_IN = 9001
    }
}

private val androidAuthManager = AndroidAuthManager()
actual fun getAuthManager(): AuthManager = androidAuthManager
fun getAndroidAuthManager(): AndroidAuthManager = androidAuthManager
