package com.example.macc_app.authentication

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.macc_app.MainActivity
import com.example.macc_app.R
import com.google.firebase.auth.FirebaseAuth

class RegistrationActivity : AppCompatActivity() {
    private lateinit var username: EditText
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var confirmPassword: EditText
    private lateinit var registerButton: Button
    private lateinit var progressBar: ProgressBar

    private lateinit var auth: FirebaseAuth

    companion object {
        private const val EMPTY_USERNAME_MESSAGE = "Please enter username!"
        private const val EMPTY_EMAIL_MESSAGE = "Please enter email!"
        private const val EMPTY_PASSWORD_MESSAGE = "Please enter password!"
        private const val PASSWORD_NOT_MATCH_MESSAGE = "Password does not match!"
        private const val REGISTRATION_SUCCESSFUL_MESSAGE = "Registration successful!"
        private const val REGISTRATION_FAILED_MESSAGE = "Registration failed!"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.registration_activity)

        auth = FirebaseAuth.getInstance()

        username = findViewById(R.id.username)
        email = findViewById(R.id.email)
        password = findViewById(R.id.password)
        confirmPassword = findViewById(R.id.confirmPassword)
        registerButton = findViewById(R.id.register)
        progressBar = findViewById(R.id.progressBar)

        // Set on Click Listener on Register button
        registerButton.setOnClickListener { registerUserAccount() }
    }

    private fun registerUserAccount() {
        // Turn progress bar visible for showing the loading progress
        progressBar.visibility = View.VISIBLE

        // Fetching credentials from input texts
        val username = username.getText().toString()
        val email = email.getText().toString()
        val password = password.getText().toString()
        val confirmPassword = confirmPassword.getText().toString()

        // Input validation
        if (TextUtils.isEmpty(username)) {
            Toast.makeText(applicationContext, EMPTY_USERNAME_MESSAGE, Toast.LENGTH_LONG).show()
            return
        }

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(applicationContext, EMPTY_EMAIL_MESSAGE, Toast.LENGTH_LONG).show()
            return
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(applicationContext, EMPTY_PASSWORD_MESSAGE, Toast.LENGTH_LONG).show()
            return
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(applicationContext, EMPTY_PASSWORD_MESSAGE, Toast.LENGTH_LONG).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(applicationContext, PASSWORD_NOT_MATCH_MESSAGE, Toast.LENGTH_LONG).show()
            return
        }

        // Sign up with email and password
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                Toast.makeText(applicationContext, REGISTRATION_SUCCESSFUL_MESSAGE,
                    Toast.LENGTH_LONG).show()

                // Hide the progress bar
                progressBar.visibility = View.GONE

                // If sign-up is successful intent to home activity
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
            .addOnFailureListener {
                // If sign-up fails
                Toast.makeText(applicationContext, REGISTRATION_FAILED_MESSAGE,
                    Toast.LENGTH_LONG).show()

                // Hide the progress bar
                progressBar.visibility = View.GONE
            }
    }
}