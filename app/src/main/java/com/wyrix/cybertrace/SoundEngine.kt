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

    fun playShoot() {
        if (isMuted) return
        playSweep(startFreq = 1200.0, endFreq = 320.0, durationMs = 75, volume = 0.35)
    }

    fun playPickup() {
        if (isMuted) return
        scope.launch {
            playToneSync(freq = 740.0, durationMs = 55, volume = 0.35, decay = 15.0)
            playToneSync(freq = 1110.0, durationMs = 85, volume = 0.38, decay = 12.0)
        }
    }

    fun playExplosion() {
        if (isMuted) return
        playSweep(startFreq = 210.0, endFreq = 45.0, durationMs = 260, volume = 0.55)
    }

    fun playLevelWin() {
        if (isMuted) return
        scope.launch {
            playToneSync(523.25, 80, 0.35, 10.0)
            playToneSync(659.25, 80, 0.35, 10.0)
            playToneSync(783.99, 80, 0.35, 10.0)
            playToneSync(1046.50, 200, 0.45, 7.0)
        }
    }

    fun playClick() {
        if (isMuted) return
        playTone(freq = 950.0, durationMs = 22, volume = 0.25, decay = 40.0)
    }

    private fun playTone(freq: Double, durationMs: Int, volume: Double, decay: Double) {
        scope.launch { playToneSync(freq, durationMs, volume, decay) }
    }

    private fun playSweep(startFreq: Double, endFreq: Double, durationMs: Int, volume: Double) {
        scope.launch {
            try {
                val sampleRate = 22050
                val numSamples = (durationMs * sampleRate) / 1000
                val samples = ShortArray(numSamples)
                var phase = 0.0
                for (i in 0 until numSamples) {
                    val progress = i.toDouble() / numSamples
                    val freq = startFreq + (endFreq - startFreq) * progress
                    val env = (1.0 - progress) * volume
                    phase += 2.0 * PI * freq / sampleRate
                    val wave = sin(phase) * env
                    samples[i] = (wave * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
                }
                writeAndPlay(samples, sampleRate, durationMs)
            } catch (_: Exception) {}
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
                samples[i] = (wave * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
            }
            writeAndPlay(samples, sampleRate, durationMs)
        } catch (_: Exception) {}
    }

    private fun writeAndPlay(samples: ShortArray, sampleRate: Int, durationMs: Int) {
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
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
    }
}
