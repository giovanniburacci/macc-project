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
import androidx.lifecycle.lifecycleScope
import com.example.macc_app.MainActivity
import com.example.macc_app.R
import com.example.macc_app.data.remote.PythonAnywhereClient
import com.example.macc_app.data.remote.PythonAnywhereFactorAPI
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LoginActivity : AppCompatActivity() {
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button
    private lateinit var progressBar: ProgressBar

    private lateinit var auth: FirebaseAuth

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(PythonAnywhereClient.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val myApiService: PythonAnywhereFactorAPI = retrofit.create(PythonAnywhereFactorAPI::class.java)

    companion object {
        private const val EMPTY_EMAIL_MESSAGE = "Please enter email!"
        private const val EMPTY_PASSWORD_MESSAGE = "Please enter password!"
        private const val LOGIN_SUCCESSFUL_MESSAGE = "Login successful!"
        private const val LOGIN_FAILED_MESSAGE = "Login failed!"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        // Check if user is already logged in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        email = findViewById(R.id.email)
        password = findViewById(R.id.password)
        loginButton = findViewById(R.id.login)
        registerButton = findViewById(R.id.register)
        progressBar = findViewById(R.id.progressBar)

        // Set on Click Listener on Sign-in button
        loginButton.setOnClickListener { loginUserAccount() }

        // Set on Click Listener on Register button
        registerButton.setOnClickListener {
            val intent = Intent(this, RegistrationActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loginUserAccount() {
        // Turn progress bar visible for showing the loading progress
        progressBar.visibility = View.VISIBLE

        // Fetching email and password from input texts
        val email = email.getText().toString()
        val password = password.getText().toString()

        // Input validation
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(applicationContext, EMPTY_EMAIL_MESSAGE, Toast.LENGTH_LONG).show()
            return
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(applicationContext, EMPTY_PASSWORD_MESSAGE, Toast.LENGTH_LONG).show()
            return
        }

        // Sign in with email and password
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                Toast.makeText(applicationContext, LOGIN_SUCCESSFUL_MESSAGE,
                    Toast.LENGTH_LONG).show()

                // Hide the progress bar
                progressBar.visibility = View.GONE

                setTargetLanguageIfNotExists(it.user!!.uid)

                // If sign-in is successful intent to home activity
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
            .addOnFailureListener {
                // If sign-in fails
                Toast.makeText(applicationContext, LOGIN_FAILED_MESSAGE,
                    Toast.LENGTH_LONG).show()

                // Hide the progress bar
                progressBar.visibility = View.GONE
            }
    }

    private fun setTargetLanguageIfNotExists(uid: String) {
        val sharedPreferences = this.getSharedPreferences("MyPrefs", MODE_PRIVATE)
        var targetLanguage = sharedPreferences.getString("targetLanguage", null)

        if (targetLanguage == null) {

            // Set target language if it doesn't exist, using coroutine
            lifecycleScope.launch(Dispatchers.IO) {
                val user = myApiService.getUser(uid)
                targetLanguage = user.target_language
                val editor = sharedPreferences.edit()
                editor.putString("targetLanguage", targetLanguage)
                editor.apply()
            }
        }
    }
}