package com.example.lab3part2

import android.content.Context
import android.content.Intent
import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class MFCCUploader(private val url: String, private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    fun uploadAudioFile(filePath: String, callback: (Boolean, Int, String?) -> Unit) {
        val file = File(filePath)
        if (!file.exists()) {
            Log.e("MFCCUploader", "Файл не найден: $filePath")
            callback(false, -1, "Файл не найден")
            return
        }

        val mediaType = "audio/wav".toMediaTypeOrNull()
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("voiceFile", file.name, file.asRequestBody(mediaType))
            .build()

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        Log.d("MFCCUploader", "Отправка файла на сервер: ${file.name}")
        Log.d("MFCCUploader", "Запрос URL: ${request.url}")
        Log.d("MFCCUploader", "Запрос Headers: ${request.headers}")
        Log.d("MFCCUploader", "Запрос Body: ${requestBody.contentType()}")

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("MFCCUploader", "Ошибка при загрузке файла: ${e.message}")
                callback(false, -1, e.message)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val responseBody = response.body?.string()
                Log.d("MFCCUploader", "Ответ сервера: ${response.code}, тело: $responseBody")
                navigateToSecondForm()
                if (response.isSuccessful) {
                    callback(true, response.code, responseBody)
                } else {
                    callback(false, response.code, responseBody)
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
            .url("https://xuy.com/login")
            .post(requestBody)
            .build()

        Log.d("MFCCUploader", "Отправка запроса на авторизацию")
        Log.d("MFCCUploader", "Запрос URL: ${request.url}")
        Log.d("MFCCUploader", "Запрос Headers: ${request.headers}")
        Log.d("MFCCUploader", "Запрос Body: ${requestBody.contentType()}")

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("MFCCUploader", "Ошибка при авторизации: ${e.message}")
                onResult(false, e.message ?: "Login failed")
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                Log.d("MFCCUploader", "Ответ сервера получен при авторизации")
                val responseBody = response.body?.string()
                Log.d("MFCCUploader", "Ответ сервера: ${response.code}, тело: $responseBody")

                if (response.isSuccessful) {
                    Log.d("MFCCUploader", "Авторизация успешна")
                    onResult(true, "Login successful")
                } else {
                    Log.e("MFCCUploader", "Ошибка авторизации: ${response.code}")
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
