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

    fun startRecording(wavFilePath: String, maxDurationSeconds: Int = 2, onError: (String) -> Unit = {}) {
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
                FileOutputStream(wavFilePath).use { outputStream ->
                    // Write WAV header first
                    val byteRate = sampleRate * 1 * 2 // 16-bit PCM, mono (1 channel)
                    val header = ByteArray(44) // Standard WAV header size
                    writeWavHeader(outputStream, header, byteRate, 0) // 0 for now, we will update later

                    while (isRecording && totalBytesRecorded < maxDurationBytes) {
                        val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                        if (read > 0) {
                            outputStream.write(buffer, 0, read)
                            totalBytesRecorded += read
                        } else {
                            Log.e("AudioHandler", "Error reading data from AudioRecord: $read")
                        }
                    }

                    // Now, update the header with the correct file size and data length
                    val fileLength = totalBytesRecorded + 36
                    val dataLength = totalBytesRecorded
                    writeWavHeader(outputStream, header, byteRate, dataLength)
                }
                Log.d("AudioHandler", "Recording finished. Total bytes recorded: $totalBytesRecorded")
            } catch (e: IOException) {
                Log.e("AudioHandler", "Error writing audio to file: ${e.message}")
                onError("Error writing audio to file")
            }
        }
    }

    private fun writeWavHeader(outputStream: FileOutputStream, header: ByteArray, byteRate: Int, dataLength: Int) {
        // RIFF header
        "RIFF".toByteArray().copyInto(header, 0)
        // Total file length (dataLength + 36 bytes for header)
        intToByteArray(dataLength + 36).copyInto(header, 4)
        // WAVE format
        "WAVE".toByteArray().copyInto(header, 8)
        // fmt sub-chunk
        "fmt ".toByteArray().copyInto(header, 12)
        intToByteArray(16).copyInto(header, 16) // Subchunk1Size
        shortToByteArray(1).copyInto(header, 20) // AudioFormat (1 = PCM)
        shortToByteArray(1).copyInto(header, 22) // Number of channels (1 = mono)
        intToByteArray(sampleRate).copyInto(header, 24) // Sample rate
        intToByteArray(byteRate).copyInto(header, 28) // Byte rate
        shortToByteArray(2).copyInto(header, 32) // Block align (channels * bits per sample / 8)
        shortToByteArray(16).copyInto(header, 34) // Bits per sample
        // data sub-chunk
        "data".toByteArray().copyInto(header, 36)
        // Data length (number of bytes)
        intToByteArray(dataLength).copyInto(header, 40)

        outputStream.write(header)
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
