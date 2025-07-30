package com.example.eventhou.data.model

import android.net.Uri

/**
 * Data class that captures user information for logged in users retrieved from LoginRepository
 */
data class AppUser(
    val userId: String,
    val email: String,
    val displayName: String?,
    val userName: String?,
    val phoneNumber: String?,
    val photoUrl: Uri?,
    val providerId: String?
)
