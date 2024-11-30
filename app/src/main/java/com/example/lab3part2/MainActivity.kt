package com.example.lab3part2

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
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {
    private lateinit var recordButton: Button
    private lateinit var stopButton: Button
    private lateinit var statusTextView: TextView
    private lateinit var audioHandler: AudioHandler
    private lateinit var mfccExtractor: MFCCExtractor
    private lateinit var audioFilePath: String
    private val sampleRate = 16000  // Set sample rate to 16000 Hz
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

        audioFilePath = "${externalCacheDir?.absolutePath}/recording.raw"

        // Load URL from the assets file
        val url = loadURLFromFile()

        val uploader = MFCCUploader(url, this)  // Use URL from file
        audioHandler = AudioHandler(sampleRate, bufferSize)
        mfccExtractor = MFCCExtractor(
            samplingRate = sampleRate,
            frameDuration = 0.001,
            hopDuration = 0.0005,
            featureCount = 300,
            filterBankSize = 500,
            preEmphasis = 0.87,
            uploader = uploader
        )

        recordButton.setOnClickListener {
            startRecording()
        }

        stopButton.setOnClickListener {
            stopRecording()
        }
    }

    private fun loadURLFromFile(): String {
        val inputStream = assets.open("url.txt")
        val reader = BufferedReader(InputStreamReader(inputStream))
        return reader.readLine()
    }

    private fun startRecording() {
        audioHandler.startRecording(audioFilePath)
        recordButton.visibility = Button.GONE
        stopButton.visibility = Button.VISIBLE
        statusTextView.text = ""
    }

    private fun stopRecording() {
        audioHandler.stopRecording()
        recordButton.visibility = Button.VISIBLE
        stopButton.visibility = Button.GONE

        statusTextView.text = "Processing..."
        stopButton.isEnabled = false

        CoroutineScope(Dispatchers.Main).launch {
            val result = withContext(Dispatchers.IO) {
                mfccExtractor.extractMFCC(audioFilePath) { success, errorCode ->
                    runOnUiThread {
                        if (success) {
                            statusTextView.text = "Processing completed successfully"
                        } else {
                            statusTextView.text = "Error occurred during processing: Server error $errorCode"
                        }
                    }
                }
            }

            stopButton.isEnabled = true
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
