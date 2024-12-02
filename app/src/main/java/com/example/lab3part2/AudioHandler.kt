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
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            Log.e("AudioHandler", "AudioRecord not initialized")
            onError("AudioRecord not initialized")
            return
        }

        audioRecord?.startRecording()
        if (audioRecord?.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            Log.e("AudioHandler", "Failed to start recording")
            onError("Failed to start recording")
            return
        }

        isRecording = true

        CoroutineScope(Dispatchers.IO).launch {
            val buffer = ByteArray(bufferSize)
            var totalBytesRecorded = 0

            try {
                FileOutputStream(wavFilePath).use { outputStream ->
                    val byteRate = sampleRate * 2 // 16-bit PCM, mono (1 channel)
                    writeWavHeader(outputStream, sampleRate, 1, 16)

                    // Чтение данных из буфера и запись в файл
                    while (isRecording) {
                        val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                        if (bytesRead > 0) {
                            outputStream.write(buffer, 0, bytesRead)
                            totalBytesRecorded += bytesRead
                        }
                    }
                }

                // Обновляем заголовок WAV с размером данных
                updateWavHeader(wavFilePath, totalBytesRecorded)

                Log.d("AudioHandler", "Recording finished. Total bytes recorded: $totalBytesRecorded")
            } catch (e: IOException) {
                Log.e("AudioHandler", "Error writing audio to file: ${e.message}")
                onError("Error writing audio to file")
            }
        }
    }

    private fun writeWavHeader(outputStream: FileOutputStream, sampleRate: Int, channels: Short, bitsPerSample: Short) {
        val header = ByteArray(44)
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = (channels * bitsPerSample / 8).toShort()

        // RIFF header
        "RIFF".toByteArray().copyInto(header, 0)
        intToByteArray(0).copyInto(header, 4) // Placeholder for chunk size
        "WAVE".toByteArray().copyInto(header, 8)

        // fmt subchunk
        "fmt ".toByteArray().copyInto(header, 12)
        intToByteArray(16).copyInto(header, 16) // Subchunk1Size for PCM
        shortToByteArray(1).copyInto(header, 20) // AudioFormat for PCM
        shortToByteArray(channels).copyInto(header, 22) // NumChannels
        intToByteArray(sampleRate).copyInto(header, 24) // SampleRate
        intToByteArray(byteRate).copyInto(header, 28) // ByteRate
        shortToByteArray(blockAlign).copyInto(header, 32) // BlockAlign
        shortToByteArray(bitsPerSample).copyInto(header, 34) // BitsPerSample

        // data subchunk
        "data".toByteArray().copyInto(header, 36)
        intToByteArray(0).copyInto(header, 40) // Placeholder for subchunk2 size

        outputStream.write(header, 0, 44)
    }

    private fun updateWavHeader(filePath: String, dataLength: Int) {
        try {
            val randomAccessFile = RandomAccessFile(filePath, "rw")
            randomAccessFile.seek(4)
            randomAccessFile.write(intToByteArray(dataLength + 36), 0, 4) // Chunk size
            randomAccessFile.seek(40)
            randomAccessFile.write(intToByteArray(dataLength), 0, 4) // Subchunk2Size
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
