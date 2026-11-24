package com.wyrix.cybertrace

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object SoundEngine {
    var isMuted: Boolean = false
    private val scope = CoroutineScope(Dispatchers.Default)

    fun playKeyClick() {
        if (isMuted) return
        playTone(freq = 1150.0, durationMs = 18, volume = 0.25, decay = 45.0)
    }

    fun playSonarPing() {
        if (isMuted) return
        playTone(freq = 880.0, durationMs = 140, volume = 0.35, decay = 14.0)
    }

    fun playErrorBuzz() {
        if (isMuted) return
        playTone(freq = 165.0, durationMs = 220, volume = 0.45, decay = 8.0)
    }

    fun playSuccessChime() {
        if (isMuted) return
        scope.launch {
            playToneSync(freq = 523.25, durationMs = 90, volume = 0.35, decay = 10.0)
            playToneSync(freq = 659.25, durationMs = 90, volume = 0.35, decay = 10.0)
            playToneSync(freq = 783.99, durationMs = 180, volume = 0.40, decay = 8.0)
        }
    }

    private fun playTone(freq: Double, durationMs: Int, volume: Double, decay: Double) {
        scope.launch {
            playToneSync(freq, durationMs, volume, decay)
        }
    }

    private fun playToneSync(freq: Double, durationMs: Int, volume: Double, decay: Double) {
        try {
            val sampleRate = 22050
            val numSamples = (durationMs * sampleRate) / 1000
            val samples = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val env = exp(-decay * t)
                val wave = sin(2.0 * PI * freq * t) * env * volume
                samples[i] = (wave * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minBuf, samples.size * 2))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(samples, 0, samples.size)
            track.play()
            Thread.sleep(durationMs.toLong() + 15)
            track.release()
        } catch (_: Exception) {
        }
    }
}
