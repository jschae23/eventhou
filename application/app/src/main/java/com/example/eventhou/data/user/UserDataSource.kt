package com.example.eventhou.data.user

import android.util.Log
import com.example.eventhou.data.Result
import com.example.eventhou.data.model.AppUser
import com.google.firebase.auth.FirebaseAuth
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
class UserDataSource {

    val mAuth = FirebaseAuth.getInstance()

    /**
     * Log in the user in firebase auth.
     * @param username the users username (most likely the email)
     * @param password the password
     */
    suspend fun login(username: String, password: String): Result<AppUser> {
        try {
            val loggedInUser = suspendCoroutine<AppUser> { continuation ->
                mAuth.signInWithEmailAndPassword(username, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val currentUser = getUser()
                            currentUser?.let {
                                continuation.resume(currentUser)
                            }
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(
                                "loginFailure",
                                "signInWithEmail:failure",
                                task.exception
                            )
                            continuation.resumeWithException(
                                task.exception ?: Exception("loginFailure: signInWithEmail:failure")
                            )
                        }
                    }
            }

            return Result.Success(loggedInUser)
        } catch (e: Throwable) {
            return Result.Error(
                IOException(
                    "Error logging in",
                    e
                )
            )
        }
    }

    /**
     * Log out the user from firebase auth.
     */
    fun logout() {
        mAuth.signOut()
    }

    /**
     * Get the current firebase user.
     */
    fun getUser(): AppUser? {
        val fbUser = mAuth.currentUser
        fbUser?.let {
            return AppUser(
                fbUser.uid,
                fbUser.email!!,
                fbUser.displayName,
                fbUser.email,
                fbUser.phoneNumber,
                fbUser.photoUrl,
                fbUser.providerId
            )
        }
        return null
    }
}
