package com.example.macc_app.authentication

import ChatViewModel
import ChatViewModelFactory
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.macc_app.MainActivity
import com.example.macc_app.R
import com.example.macc_app.data.remote.AddUserBody
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.database
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class RegistrationActivity : AppCompatActivity() {
    private lateinit var username: EditText
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var confirmPassword: EditText
    private lateinit var registerButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var languageSpinner: Spinner

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseDatabase

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://ghinoads.pythonanywhere.com")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val viewModel: ChatViewModel by viewModels { ChatViewModelFactory(retrofit) }

    companion object {
        private const val EMPTY_USERNAME_MESSAGE = "Please enter username!"
        private const val EMPTY_EMAIL_MESSAGE = "Please enter email!"
        private const val EMPTY_PASSWORD_MESSAGE = "Please enter password!"
        private const val PASSWORD_NOT_MATCH_MESSAGE = "Password does not match!"
        private const val REGISTRATION_SUCCESSFUL_MESSAGE = "Registration successful!"
        private const val REGISTRATION_FAILED_MESSAGE = "Registration failed!"

        private const val DB_URL = "https://macc-project-bf2f7-default-rtdb.europe-west1.firebasedatabase.app/"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.registration_activity)
        languageSpinner = findViewById<Spinner>(R.id.spinner2)


        val adapter = ArrayAdapter.createFromResource(
            this, R.array.languages, android.R.layout.simple_spinner_item
        )


        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)


        languageSpinner.adapter = adapter
        var index = -1
        for (i in 0 until adapter.count) {
            if (adapter.getItem(i) == "Italian") { // Check for "Italian"
                index = i
                break
            }
        }


// Set the default selection
        if (index != -1) {
            languageSpinner.setSelection(index)
        } else {
            Log.e("Spinner", "Language 'Italian' not found in the adapter.")
        }
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

    private fun getLanguageCodeMap(): Map<String, String> {
        return mapOf(
            "Afrikaans" to "af",
            "Arabic" to "ar",
            "Belarusian" to "be",
            "Bulgarian" to "bg",
            "Bengali" to "bn",
            "Catalan" to "ca",
            "Czech" to "cs",
            "Welsh" to "cy",
            "Danish" to "da",
            "German" to "de",
            "Greek" to "el",
            "English" to "en",
            "Esperanto" to "eo",
            "Spanish" to "es",
            "Estonian" to "et",
            "Persian" to "fa",
            "Finnish" to "fi",
            "French" to "fr",
            "Irish" to "ga",
            "Galician" to "gl",
            "Gujarati" to "gu",
            "Hebrew" to "he",
            "Hindi" to "hi",
            "Croatian" to "hr",
            "Haitian" to "ht",
            "Hungarian" to "hu",
            "Indonesian" to "id",
            "Icelandic" to "is",
            "Italian" to "it",
            "Japanese" to "ja",
            "Georgian" to "ka",
            "Kannada" to "kn",
            "Korean" to "ko",
            "Lithuanian" to "lt",
            "Latvian" to "lv",
            "Macedonian" to "mk",
            "Marathi" to "mr",
            "Malay" to "ms",
            "Maltese" to "mt",
            "Dutch" to "nl",
            "Norwegian" to "no",
            "Polish" to "pl",
            "Portuguese" to "pt",
            "Romanian" to "ro",
            "Russian" to "ru",
            "Slovak" to "sk",
            "Slovenian" to "sl",
            "Albanian" to "sq",
            "Swedish" to "sv",
            "Swahili" to "sw",
            "Tamil" to "ta",
            "Telugu" to "te",
            "Thai" to "th",
            "Tagalog" to "tl",
            "Turkish" to "tr",
            "Ukrainian" to "uk",
            "Urdu" to "ur",
            "Vietnamese" to "vi",
            "Chinese" to "zh"
        )
    }


    private fun registerUserAccount() {
        // Turn progress bar visible for showing the loading progress
        progressBar.visibility = View.VISIBLE

        // Fetching credentials from input texts
        val username = username.getText().toString()
        val email = email.getText().toString()
        val password = password.getText().toString()
        val confirmPassword = confirmPassword.getText().toString()
        val language = languageSpinner.selectedItem.toString()

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

                // Save user data in db
                db = Firebase.database(DB_URL)

                val user = User(username, email)
                val uid = auth.currentUser!!.uid
                val body = AddUserBody(uid = uid, username = username, email = email)
                viewModel.createUser(body)
                val languageMap = getLanguageCodeMap()

                val languageCode = languageMap[language]
                val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.putString("targetLanguage", languageCode)
                editor.apply()

                db.getReference("users/$uid").setValue(user)
                    .addOnSuccessListener {
                        // If sign-up is successful and user data is saved in db, intent to home activity
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                    }
                    .addOnFailureListener {
                        // If save fails
                        Toast.makeText(applicationContext, REGISTRATION_FAILED_MESSAGE,
                            Toast.LENGTH_LONG).show()

                        // Hide the progress bar
                        progressBar.visibility = View.GONE
                    }
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