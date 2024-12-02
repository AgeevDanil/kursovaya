package com.example.lab3part2

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.io.File

// Импортируем AudioHandler
import com.example.lab3part2.AudioHandler

class MainActivity : AppCompatActivity() {
    private lateinit var recordButton: Button
    private lateinit var stopButton: Button
    private lateinit var statusTextView: TextView
    private lateinit var audioHandler: AudioHandler
    private lateinit var mfccUploader: MFCCUploader
    private lateinit var audioWavPath: String
    private val sampleRate = 44100
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!hasPermissions()) {
            requestPermissions()
        }

        recordButton = findViewById(R.id.recordButton)
        stopButton = findViewById(R.id.stopButton)
        statusTextView = findViewById(R.id.statusTextView)

        audioWavPath = "${externalCacheDir?.absolutePath}/recording.wav"

        audioHandler = AudioHandler(sampleRate, bufferSize)
        mfccUploader = MFCCUploader("http://89.23.105.181:5248/api/voice/get-mfcc", applicationContext)

        recordButton.setOnClickListener {
            startRecording()
        }

        stopButton.setOnClickListener {
            stopRecordingAndUpload()
        }
    }

    private fun startRecording() {
        try {
            audioHandler.startRecording(audioWavPath, { error ->
                statusTextView.text = "Failed to start recording: $error"
                Log.e("MainActivity", "Error starting recording", Throwable(error))
            })
            recordButton.visibility = Button.GONE
            stopButton.visibility = Button.VISIBLE
            statusTextView.text = "Recording..."
            Log.d("MainActivity", "Recording started at $audioWavPath")
        } catch (e: Exception) {
            statusTextView.text = "Failed to start recording: ${e.message}"
            Log.e("MainActivity", "Error starting recording", e)
        }
    }

    private fun stopRecordingAndUpload() {
        try {
            audioHandler.stopRecording()
            Log.d("MainActivity", "Recording stopped, file saved at $audioWavPath")

            recordButton.visibility = Button.VISIBLE
            stopButton.visibility = Button.GONE
            statusTextView.text = "Uploading audio file..."

            CoroutineScope(Dispatchers.Main).launch {
                withContext(Dispatchers.IO) {
                    mfccUploader.uploadAudioFile(audioWavPath) { success, errorCode, errorMessage ->
                        runOnUiThread {
                            if (success) {
                                statusTextView.text = "Upload successful, navigating to next screen..."
                                clearCache()
                            } else {
                                statusTextView.text = "Upload failed: Server error $errorCode, $errorMessage"
                            }
                            stopButton.isEnabled = true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            statusTextView.text = "Failed to stop recording: ${e.message}"
            Log.e("MainActivity", "Error stopping recording", e)
        }
    }

    private fun hasPermissions(): Boolean {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ),
            200
        )
    }

    private fun clearCache() {
        try {
            val cacheDir = externalCacheDir
            if (cacheDir != null && cacheDir.isDirectory) {
                cacheDir.listFiles()?.forEach { file ->
                    if (!file.delete()) {
                        Log.e("MainActivity", "Failed to delete ${file.absolutePath}")
                    }
                }
                Log.d("MainActivity", "Cache cleared successfully")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to clear cache: ${e.message}")
        }
    }
}
