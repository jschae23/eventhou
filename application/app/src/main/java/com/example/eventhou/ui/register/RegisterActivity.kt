package com.example.eventhou.ui.register

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.eventhou.EventhouApplication
import com.example.eventhou.R
import com.example.eventhou.ui.login.LoginActivity
import com.example.eventhou.ui.login.afterTextChanged
import com.example.eventhou.ui.tutorial.TutorialActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class RegisterActivity : AppCompatActivity() {
    lateinit var toolbar: ActionBar
    private var mAuth: FirebaseAuth? = null
    var toast: Toast? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mAuth = FirebaseAuth.getInstance();
        setContentView(R.layout.activity_register)
        toolbar = supportActionBar!!
        toolbar.hide()

        val username: EditText = findViewById(R.id.username)
        val pwInput: EditText = findViewById(R.id.password)
        val pwVerInput: EditText = findViewById(R.id.verify_password)

        val backToLogin: Button = findViewById(R.id.backtologin)

        var email = ""
        var password = ""
        var passwordRetype = ""

        val appContainer = (this.application as EventhouApplication).appContainer

        fun isValidEmail(target: String): Boolean {
            return !TextUtils.isEmpty(target) && android.util.Patterns.EMAIL_ADDRESS.matcher(target)
                .matches();
        }

        username.afterTextChanged {
            email = username.text.toString()
            password = pwInput.text.toString()
        }

        // Password Checks
        pwInput.afterTextChanged {
            if (!isValidEmail(email)) {
                updateUi("Invalid Email")
            }
            email = username.text.toString()
            password = pwInput.text.toString()
        }
        pwVerInput.afterTextChanged {
            if (!isPasswordValid(password)) {
                updateUi("The password must have more than 5 characters.")
            }
            passwordRetype = pwVerInput.text.toString()
        }

        val registerButton: Button = findViewById(R.id.register)

        registerButton.setOnClickListener {
            if (passwordRetype != password) {
                updateUi("The passwords are not identical.")
            } else if (!isPasswordValid(password)) {
                updateUi("The password must have more than 5 characters.")
            } else if (!isValidEmail(email)) {
                updateUi("Invalid Email")
            } else {
                mAuth!!.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(
                        this
                    ) { task ->
                        if (task.isSuccessful) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("CreateUser", "createUserWithEmail:success")
                            updateUi("Registered")
                            setResult(Activity.RESULT_OK)

                            // Launches the login Handling in a separate asynchronous job.
                            lifecycleScope.launch(Dispatchers.IO) {
                                appContainer.userRepository.login(email, password)
                            }

                            val intent = Intent(this, TutorialActivity::class.java)
                            startActivity(intent)

                            finish()
                        } else {
                            Log.w(
                                "CreateUserFailure",
                                "createUserWithEmail:failure",
                                task.exception
                            )
                            Toast.makeText(
                                applicationContext,
                                "Register failed",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                    }
            }

        }
        backToLogin.setOnClickListener {
            setResult(Activity.RESULT_OK)

            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)

            finish()
        }
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.length > 5
    }

    /**
     *  Displays a text, that contains input responses for the user
     */
    private fun updateUi(displayText: String) {
        if (toast != null) {
            toast?.cancel()
        }
        toast = Toast.makeText(
            applicationContext,
            displayText,
            Toast.LENGTH_LONG
        )
        toast?.show()
    }
}