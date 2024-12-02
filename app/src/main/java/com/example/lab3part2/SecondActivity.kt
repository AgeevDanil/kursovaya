package com.example.lab3part2

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SecondActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)

        val responseCode = intent.getIntExtra("response_code", -1)
        val responseMessage = intent.getStringExtra("response_message")

        val responseTextView: TextView = findViewById(R.id.responseTextView)
        responseTextView.text = "Response Code: $responseCode\nResponse Message: $responseMessage"
    }
}
