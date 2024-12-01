package com.example.lab3part2

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class AudioHandler(private val sampleRate: Int, private val bufferSize: Int) {
    private var audioRecord: AudioRecord? = null
    var isRecording = false
        private set

    fun startRecording(audioFilePath: String, maxDurationSeconds: Int = 2, onError: (String) -> Unit = {}) {
        Log.d("AudioHandler", "Start recording")
        audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize)

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            Log.e("AudioHandler", "AudioRecord not initialized")
            onError("AudioRecord not initialized")
            return
        }

        audioRecord?.startRecording()
        isRecording = true
        val maxDurationBytes = maxDurationSeconds * sampleRate * 2 // 2 bytes per sample for PCM 16-bit

        CoroutineScope(Dispatchers.IO).launch {
            val buffer = ByteArray(bufferSize)
            var totalBytesRecorded = 0

            try {
                FileOutputStream(audioFilePath).use { outputStream ->
                    while (isRecording && totalBytesRecorded < maxDurationBytes) {
                        val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                        if (read > 0) {
                            outputStream.write(buffer, 0, read)
                            totalBytesRecorded += read
                        } else {
                            Log.e("AudioHandler", "Error reading data from AudioRecord: $read")
                        }
                    }
                    outputStream.flush()
                }
                Log.d("AudioHandler", "Recording finished. Total bytes recorded: $totalBytesRecorded")
            } catch (e: IOException) {
                Log.e("AudioHandler", "Error writing audio to file: ${e.message}")
                onError("Error writing audio to file")
            }
        }
    }

    fun stopRecording() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        Log.d("AudioHandler", "Recording stopped")
    }

    fun convertRawToWav(rawFilePath: String, wavFilePath: String, sampleRate: Int, channels: Int = 1) {
        val rawFile = File(rawFilePath)
        val wavFile = File(wavFilePath)

        val rawData = rawFile.readBytes()
        val totalDataLength = rawData.size
        val totalFileLength = totalDataLength + 36
        val byteRate = sampleRate * channels * 2 // 16-bit PCM

        wavFile.outputStream().use { outputStream ->
            outputStream.write("RIFF".toByteArray())
            outputStream.write(intToByteArray(totalFileLength))
            outputStream.write("WAVE".toByteArray())
            outputStream.write("fmt ".toByteArray())
            outputStream.write(intToByteArray(16)) // Subchunk1Size
            outputStream.write(shortToByteArray(1)) // AudioFormat (1 = PCM)
            outputStream.write(shortToByteArray(channels.toShort()))
            outputStream.write(intToByteArray(sampleRate))
            outputStream.write(intToByteArray(byteRate))
            outputStream.write(shortToByteArray((channels * 2).toShort())) // BlockAlign
            outputStream.write(shortToByteArray(16)) // BitsPerSample
            outputStream.write("data".toByteArray())
            outputStream.write(intToByteArray(totalDataLength))
            outputStream.write(rawData)
        }
    }

    private fun intToByteArray(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte()
        )
    }

    private fun shortToByteArray(value: Short): ByteArray {
        return byteArrayOf(
            (value.toInt() and 0xFF).toByte(),
            ((value.toInt() shr 8) and 0xFF).toByte()
        )
    }
}
