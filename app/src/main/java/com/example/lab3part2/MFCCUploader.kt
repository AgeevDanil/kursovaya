package com.example.lab3part2

import android.content.Context
import android.content.Intent
import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException

class MFCCUploader(private val url: String, private val context: Context) {
    private val client = OkHttpClient()

    fun uploadAudioFile(filePath: String, callback: (Boolean, Int) -> Unit) {
        val file = File(filePath)
        val mediaType = "audio/wav".toMediaTypeOrNull()
        val requestBody = file.asRequestBody(mediaType)
        val request = Request.Builder()
            .url(url) // Используется ваш API URL
            .post(requestBody)
            .build()

        Log.d("MFCCUploader", "Uploading file: ${file.absolutePath}")

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("MFCCUploader", "Error uploading audio file: ${e.message}")
                callback(false, -1)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (response.isSuccessful) {
                    Log.d("MFCCUploader", "Audio file upload successful")
                    callback(true, response.code)
                    navigateToSecondForm()
                } else {
                    Log.e("MFCCUploader", "Server error: ${response.code}")
                    Log.e("MFCCUploader", "Response Body: ${response.body?.string()}")
                    callback(false, response.code)
                }
            }
        })
    }

    fun login(username: String, password: String, onResult: (Boolean, String) -> Unit) {
        val json = JSONObject()
        json.put("username", username)
        json.put("password", password)

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val requestBody: RequestBody = json.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url("https://example.com/login")  // Replace with your URL
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("MFCCUploader", "Login error: ${e.message}")
                onResult(false, e.message ?: "Login failed")
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (response.isSuccessful) {
                    Log.d("MFCCUploader", "Login successful")
                    onResult(true, "Login successful")
                } else {
                    Log.e("MFCCUploader", "Login error: ${response.code}")
                    onResult(false, "Login failed: ${response.code}")
                }
            }
        })
    }

    private fun showError(errorMessage: String) {
        Log.e("MFCCUploader", errorMessage)
    }

    private fun navigateToSecondForm() {
        val intent = Intent(context, SecondActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

}
