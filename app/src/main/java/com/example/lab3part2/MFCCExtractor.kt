package com.example.lab3part2

import android.util.Log
import be.tarsos.dsp.*
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.io.UniversalAudioInputStream
import be.tarsos.dsp.mfcc.MFCC
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File

class MFCCExtractor(
    private val samplingRate: Int = 16000,
    private val frameDuration: Double = 0.001,
    private val hopDuration: Double = 0.0005,
    private val featureCount: Int = 300,
    private val filterBankSize: Int = 500,
    private val preEmphasis: Double = 0.87,
    private val uploader: MFCCUploader
) {

    suspend fun extractMFCC(audioFilePath: String, callback: (Boolean, Int) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                val audioFile = File(audioFilePath)
                if (!audioFile.exists()) {
                    Log.d("MFCCExtractor", "File not found: $audioFilePath")
                    callback(false, -1)  // Indicate error with a special error code
                    return@withContext
                }

                Log.d("MFCCExtractor", "File found: ${audioFile.length()} bytes")

                val audioData = audioFile.readBytes()
                val audioStream = ByteArrayInputStream(audioData)
                val audioFormat = TarsosDSPAudioFormat(samplingRate.toFloat(), 16, 1, true, false)
                val audioInputStream = UniversalAudioInputStream(audioStream, audioFormat)

                val bufferSize = (frameDuration * samplingRate).toInt()
                val overlap = (hopDuration * samplingRate).toInt()

                val dispatcher = AudioDispatcher(audioInputStream, bufferSize, overlap)

                val lowerFilterFreq = 0.0f // Lower filter frequency
                val upperFilterFreq = (samplingRate / 2).toFloat() // Upper filter frequency

                val mfccProcessor = MFCC(
                    bufferSize,
                    samplingRate.toFloat(),
                    featureCount,
                    filterBankSize,
                    lowerFilterFreq,
                    upperFilterFreq
                )

                val uniqueSegments = mutableListOf<List<Float>>()

                dispatcher.addAudioProcessor(object : AudioProcessor {
                    override fun process(audioEvent: AudioEvent): Boolean {
                        mfccProcessor.process(audioEvent)
                        val mfcc = mfccProcessor.mfcc?.toList()

                        if (mfcc != null && mfcc.isNotEmpty() && mfcc[0] != -25000.0f) {
                            if (!uniqueSegments.contains(mfcc)) {
                                uniqueSegments.add(mfcc)
                            }
                        }
                        return true
                    }

                    override fun processingFinished() {
                        if (uniqueSegments.isNotEmpty()) {
                            val firstUniqueSegment = uniqueSegments[0].joinToString(", ")
                            Log.d("MFCC", firstUniqueSegment)
                            uploader.uploadMFCC(listOf(firstUniqueSegment), callback)  // Sending first unique segment
                        } else {
                            callback(false, -1)
                        }
                        Log.d("MFCC", "Processing completed")
                    }
                })

                dispatcher.run()

            } catch (e: Exception) {
                Log.e("MFCC", "Error: ${e.message}")
                callback(false, -1)
            }
        }
    }
}
