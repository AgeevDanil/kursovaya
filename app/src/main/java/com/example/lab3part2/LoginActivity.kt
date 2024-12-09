package com.example.lab3part2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var loginButton: Button
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var errorTextView: TextView
    private lateinit var mfccUploader: MFCCUploader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        loginButton = findViewById(R.id.loginButton)
        usernameEditText = findViewById(R.id.usernameEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        errorTextView = findViewById(R.id.errorTextView)

        mfccUploader = MFCCUploader("http://89.23.105.181:5428/api2/auth/auth", this)  // Когда серверная часть доработается

        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()

            mfccUploader.login(username, password) { success, message ->
                runOnUiThread {
                    if (success) {
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    } else {
                        errorTextView.text = message
                    }
                }
            }
        }
    }
}
