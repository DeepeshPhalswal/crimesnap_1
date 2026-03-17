package com.example.crimesnap

import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

actual class AuthManager {
    private val auth = Firebase.auth
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val _currentUser = MutableStateFlow<User?>(null)
    actual val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        scope.launch {
            auth.authStateChanged.collect { firebaseUser ->
                _currentUser.value = firebaseUser?.let {
                    User(
                        id = it.uid,
                        name = it.displayName,
                        email = it.email,
                        photoUrl = it.photoURL
                    )
                }
            }
        }
    }

    actual suspend fun signIn(): Result<User> {
        val activity = AndroidPlatform.currentActivity ?: return Result.failure(Exception("Activity not found"))
        val credentialManager = CredentialManager.create(activity)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId("705077492406-d5tj48pocttsdmabf7bsmeqo5eb08lns.apps.googleusercontent.com")
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential
            
            if (credential is androidx.credentials.CustomCredential && 
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                
                val firebaseCredential = GoogleAuthProvider.credential(idToken, null)
                
                val authResult = auth.signInWithCredential(firebaseCredential)
                val user = authResult.user?.let {
                    User(
                        id = it.uid,
                        name = it.displayName,
                        email = it.email,
                        photoUrl = it.photoURL
                    )
                } ?: throw Exception("Sign in failed: No user returned")
                
                Result.success(user)
            } else {
                Result.failure(Exception("Unexpected credential type: ${credential.type}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual suspend fun signOut() {
        auth.signOut()
        AndroidPlatform.currentActivity?.let { activity ->
            try {
                CredentialManager.create(activity).clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {}
        }
    }
}

private var authManagerInstance: AuthManager? = null

actual fun getAuthManager(): AuthManager {
    if (authManagerInstance == null) {
        authManagerInstance = AuthManager()
    }
    return authManagerInstance!!
}
