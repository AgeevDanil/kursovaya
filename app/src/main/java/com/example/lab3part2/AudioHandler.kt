package com.example.lab3part2

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.*
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile

class AudioHandler(private val sampleRate: Int, private val bufferSize: Int) {
    private var audioRecord: AudioRecord? = null
    var isRecording = false
        private set

    fun startRecording(wavFilePath: String, onError: (String) -> Unit = {}) {
        Log.d("AudioHandler", "Start recording")
        audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize)

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            Log.e("AudioHandler", "AudioRecord not initialized")
            onError("AudioRecord not initialized")
            return
        }

        audioRecord?.startRecording()
        isRecording = true

        CoroutineScope(Dispatchers.IO).launch {
            val buffer = ByteArray(bufferSize)
            var totalBytesRecorded = 0

            try {
                FileOutputStream(wavFilePath).use { outputStream ->
                    // Write the initial header with placeholder values
                    val byteRate = sampleRate * 2 // 16-bit PCM, mono (1 channel)
                    writeWavHeader(outputStream, byteRate, 0)

                    // Write audio data
                    while (isRecording) {
                        val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                        Log.d("AudioHandler", "Bytes read from AudioRecord: $read")

                        // Проверка данных на адекватность уровня громкости
                        val dataSnippet = buffer.take(10).joinToString(" ") { it.toString() }
                        Log.d("AudioHandler", "Audio data snippet: $dataSnippet")

                        if (buffer.none { it.toInt() != 0 }) {
                            Log.w("AudioHandler", "All zero bytes detected in buffer, check microphone input.")
                        }

                        if (read > 0) {
                            outputStream.write(buffer, 0, read)
                            totalBytesRecorded += read
                        } else {
                            Log.e("AudioHandler", "No data read from AudioRecord: $read")
                            delay(10)  // небольшой таймаут перед повторной попыткой
                        }
                    }
                }

                // Update the header with the correct file size and data length
                val byteRate = sampleRate * 2
                updateWavHeader(wavFilePath, byteRate, totalBytesRecorded)

                Log.d("AudioHandler", "Recording finished. Total bytes recorded: $totalBytesRecorded")
            } catch (e: IOException) {
                Log.e("AudioHandler", "Error writing audio to file: ${e.message}")
                onError("Error writing audio to file")
            }
        }
    }

    private fun writeWavHeader(outputStream: FileOutputStream, byteRate: Int, dataLength: Int) {
        val header = ByteArray(44)
        // RIFF header
        "RIFF".toByteArray().copyInto(header, 0)
        intToByteArray(dataLength + 36).copyInto(header, 4) // Total file length
        "WAVE".toByteArray().copyInto(header, 8)
        // fmt sub-chunk
        "fmt ".toByteArray().copyInto(header, 12)
        intToByteArray(16).copyInto(header, 16) // Subchunk1Size (16 for PCM)
        shortToByteArray(1).copyInto(header, 20) // AudioFormat (1 for PCM)
        shortToByteArray(1).copyInto(header, 22) // Number of channels (1 for mono)
        intToByteArray(sampleRate).copyInto(header, 24) // Sample rate
        intToByteArray(byteRate).copyInto(header, 28) // Byte rate
        shortToByteArray(2).copyInto(header, 32) // Block align (bytes per sample)
        shortToByteArray(16).copyInto(header, 34) // Bits per sample
        // data sub-chunk
        "data".toByteArray().copyInto(header, 36)
        intToByteArray(dataLength).copyInto(header, 40) // Data length
        outputStream.write(header, 0, 44)
    }

    private fun updateWavHeader(filePath: String, byteRate: Int, dataLength: Int) {
        try {
            val randomAccessFile = RandomAccessFile(filePath, "rw")
            randomAccessFile.seek(4)
            randomAccessFile.write(intToByteArray(dataLength + 36), 0, 4)
            randomAccessFile.seek(40)
            randomAccessFile.write(intToByteArray(dataLength), 0, 4)
            randomAccessFile.close()
        } catch (e: IOException) {
            Log.e("AudioHandler", "Error updating WAV header: ${e.message}")
        }
    }

    fun stopRecording() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        Log.d("AudioHandler", "Recording stopped")
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
