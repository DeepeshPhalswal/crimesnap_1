package com.example.crimesnap

import cocoapods.FirebaseAuth.FIRAuth
import cocoapods.FirebaseAuth.FIRGoogleAuthProvider
import cocoapods.GoogleSignIn.GIDConfiguration
import cocoapods.GoogleSignIn.GIDSignIn
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class IosAuthManager : AuthManager {
    private val auth = FIRAuth.auth()
    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        auth.addAuthStateDidChangeListener { _, firUser ->
            _currentUser.value = firUser?.let {
                User(
                    id = it.uid,
                    name = it.displayName,
                    email = it.email,
                    photoUrl = it.photoURL?.absoluteString
                )
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun signIn(): Result<User> = suspendCoroutine { continuation ->
        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
            ?: return@suspendCoroutine continuation.resume(Result.failure(Exception("Root view controller not found")))

        GIDSignIn.sharedInstance.signInWithPresentingViewController(rootViewController) { result, error ->
            if (error != null) {
                continuation.resume(Result.failure(Exception(error.localizedDescription)))
                return@signInWithPresentingViewController
            }

            val idToken = result?.user?.idToken?.tokenString
            if (idToken == null) {
                continuation.resume(Result.failure(Exception("Google ID Token not found")))
                return@signInWithPresentingViewController
            }

            val credential = FIRGoogleAuthProvider.credentialWithIDToken(idToken, "")
            auth.signInWithCredential(credential) { authResult, authError ->
                if (authError != null) {
                    continuation.resume(Result.failure(Exception(authError.localizedDescription)))
                } else {
                    val user = authResult?.user?.let {
                        User(
                            id = it.uid,
                            name = it.displayName,
                            email = it.email,
                            photoUrl = it.photoURL?.absoluteString
                        )
                    }
                    if (user != null) {
                        continuation.resume(Result.success(user))
                    } else {
                        continuation.resume(Result.failure(Exception("Firebase user creation failed")))
                    }
                }
            }
        }
    }

    override suspend fun signOut() {
        auth.signOut(null)
        GIDSignIn.sharedInstance.signOut()
        _currentUser.value = null
    }

    override fun updateReportsCount(count: Int) {
        _currentUser.update { user ->
            user?.copy(reportsCount = count)
        }
    }
}

actual fun getAuthManager(): AuthManager = IosAuthManager()
