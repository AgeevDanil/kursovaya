package com.example.lab3part2

import AudioHandler
import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*

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

        // Указываем путь для записи в WAV
        audioWavPath = "${externalCacheDir?.absolutePath}/recording.wav"

        audioHandler = AudioHandler(sampleRate, bufferSize)
        mfccUploader = MFCCUploader("http://89.23.105.181:5248/api/voice/auth-mfcc", applicationContext) // Когда серверная часть доработается, заменить урл

        recordButton.setOnClickListener {
            startRecording()
        }

        stopButton.setOnClickListener {
            stopRecordingAndUpload()
        }
    }

    private fun startRecording() {
        // Теперь сразу записываем в WAV
        audioHandler.startRecording(audioWavPath)
        recordButton.visibility = Button.GONE
        stopButton.visibility = Button.VISIBLE
        statusTextView.text = "Recording..."
    }

    private fun stopRecordingAndUpload() {
        audioHandler.stopRecording()
        recordButton.visibility = Button.VISIBLE
        stopButton.visibility = Button.GONE

        statusTextView.text = "Uploading audio file..."
        stopButton.isEnabled = false

        CoroutineScope(Dispatchers.Main).launch {
            // Загружаем аудиофайл в формате WAV
            withContext(Dispatchers.IO) {
                mfccUploader.uploadAudioFile(audioWavPath) { success, errorCode ->
                    runOnUiThread {
                        if (success) {
                            statusTextView.text = "Upload successful, navigating to next screen..."
                        } else {
                            statusTextView.text = "Upload failed: Server error $errorCode"
                        }
                        stopButton.isEnabled = true
                    }
                }
            }
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
}
