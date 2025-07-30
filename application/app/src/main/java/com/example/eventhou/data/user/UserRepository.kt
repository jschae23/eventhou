package com.example.eventhou.data.user

import com.example.eventhou.data.Result
import com.example.eventhou.data.model.AppUser

/**
 * Class that requests authentication and user information from the remote data source and
 * maintains an in-memory cache of login status and user credentials information.
 */

class UserRepository(val dataSource: UserDataSource) {

    // in-memory cache of the loggedInUser object
    var user: AppUser? = null
        private set

    val isLoggedIn: Boolean
        get() = user != null

    init {
        // If user credentials will be cached in local storage, it is recommended it be encrypted
        // @see https://developer.android.com/training/articles/keystore
        user = dataSource.getUser()
    }

    /**
     * Log in the user in data source.
     * @param username the users username (most likely the email)
     * @param password the password
     */
    suspend fun login(username: String, password: String): Result<AppUser> {
        // handle login
        val result = dataSource.login(username, password)

        if (result is Result.Success) {
            setLoggedInUser(result.data)
        }

        return result
    }

    /**
     * Log out the user from data source and clear user.
     */
    fun logout() {
        user = null
        dataSource.logout()
    }

    private fun setLoggedInUser(appUser: AppUser) {
        this.user = appUser
        // If user credentials will be cached in local storage, it is recommended it be encrypted
        // @see https://developer.android.com/training/articles/keystore
    }

    /**
     * Get the current logged in user.
     */
    fun getLoggedInUser(): AppUser? {
        return user
    }
}
