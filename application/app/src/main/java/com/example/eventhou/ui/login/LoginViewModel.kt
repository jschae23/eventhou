package com.example.eventhou.ui.login

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventhou.R
import com.example.eventhou.data.Result
import com.example.eventhou.data.user.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _loginForm = MutableLiveData<LoginFormState>()
    val loginFormState: LiveData<LoginFormState> = _loginForm

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult

    fun login(username: String, password: String) {

        // Launches the login Handling in a separate asynchronous job.
        viewModelScope.launch(Dispatchers.IO) {
            val result = userRepository.login(username, password)

            if (result is Result.Success) {
                _loginResult.postValue(
                    LoginResult(
                        success = LoggedInUserView(
                            displayName = result.data.displayName ?: result.data.userName
                            ?: result.data.email
                        )
                    )
                )
            } else {
                _loginResult.postValue(LoginResult(error = R.string.login_failed))
            }
        }
    }

    fun loginDataChanged(username: String, password: String) {
        if (!isUserNameValid(username)) {
            _loginForm.value = LoginFormState(usernameError = R.string.invalid_username)
        } else if (!isPasswordValid(password)) {
            _loginForm.value = LoginFormState(passwordError = R.string.invalid_password)
        } else {
            _loginForm.value = LoginFormState(isDataValid = true)
        }
    }

    /**
     * A placeholder username validation check
     */
    private fun isUserNameValid(username: String): Boolean {
        return if (username.contains('@')) {
            Patterns.EMAIL_ADDRESS.matcher(username).matches()
        } else {
            username.isNotBlank()
        }
    }

    /**
     * A placeholder password validation check
     */
    private fun isPasswordValid(password: String): Boolean {
        return password.length > 5
    }
}
