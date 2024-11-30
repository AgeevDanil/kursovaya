package com.example.lab3part2

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.io.FileOutputStream

class AudioHandler(private val sampleRate: Int, private val bufferSize: Int) {
    private var audioRecord: AudioRecord? = null
    var isRecording = false
        private set

    fun startRecording(audioFilePath: String) {
        Log.d("AudioHandler", "Start recording")
        audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize)
        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            Log.e("AudioHandler", "AudioRecord not initialized")
            return
        }
        Log.d("AudioHandler", "AudioRecord initialized")
        audioRecord?.startRecording()
        Log.d("AudioHandler", "Recording started")
        isRecording = true
        Thread {
            val buffer = ByteArray(bufferSize)
            var totalBytesRecorded = 0
            val maxDuration = 2 * sampleRate * 2 // 2 seconds of audio data

            FileOutputStream(audioFilePath).use { outputStream ->
                while (isRecording && totalBytesRecorded < maxDuration) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        Log.d("AudioHandler", "Read data, number of bytes: $read")
                        outputStream.write(buffer, 0, read)
                        totalBytesRecorded += read
                        Log.d("AudioHandler", "Bytes recorded: $totalBytesRecorded")
                    } else {
                        Log.e("AudioHandler", "Error reading data from AudioRecord, read returned: $read")
                    }
                }
                outputStream.flush()
                Log.d("AudioHandler", "Recording finished, total bytes recorded: $totalBytesRecorded")
            }
        }.start()
    }

    fun stopRecording() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        Log.d("AudioHandler", "Recording stopped")
    }
}
