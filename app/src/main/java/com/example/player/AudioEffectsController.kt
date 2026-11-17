package com.example.player

import android.media.audiofx.Visualizer
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Real-time PCM AudioProcessor for Stereo Balance (-1f left .. +1f right) and soft Volume Normalization.
 */
@androidx.media3.common.util.UnstableApi
class BalanceAndNormalizationAudioProcessor : BaseAudioProcessor() {

    @Volatile var stereoBalance: Float = 0f
    @Volatile var normalizationEnabled: Boolean = true

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val out = replaceOutputBuffer(remaining)
        val channels = inputAudioFormat.channelCount
        val balance = stereoBalance.coerceIn(-1f, 1f)
        val leftGain = if (balance > 0f) 1f - balance else 1f
        val rightGain = if (balance < 0f) 1f + balance else 1f
        val normGain = if (normalizationEnabled) 1.08f else 1f

        if (channels >= 2) {
            while (inputBuffer.remaining() >= 4) {
                val l = inputBuffer.short.toFloat() * leftGain * normGain
                val r = inputBuffer.short.toFloat() * rightGain * normGain
                out.putShort(l.roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort())
                out.putShort(r.roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort())
                for (c in 2 until channels) {
                    if (inputBuffer.remaining() >= 2) {
                        out.putShort(inputBuffer.short)
                    }
                }
            }
        }
        while (inputBuffer.hasRemaining()) {
            out.put(inputBuffer.get())
        }
        out.flip()
    }
}

class AudioEffectsController {

    private var visualizer: Visualizer? = null
    private var currentSessionId: Int = 0

    fun attachToSession(audioSessionId: Int) {
        if (audioSessionId <= 0 || audioSessionId == currentSessionId) return
        release()
        currentSessionId = audioSessionId

        runCatching {
            visualizer = Visualizer(audioSessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1].coerceAtMost(256)
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            visualizer: Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int
                        ) = Unit

                        override fun onFftDataCapture(
                            visualizer: Visualizer?,
                            fft: ByteArray?,
                            samplingRate: Int
                        ) {
                            if (fft == null || fft.size < 48) return
                            val levels = List(24) { idx ->
                                val re = fft[(idx * 2).coerceIn(0, fft.lastIndex)].toInt()
                                val im = fft[(idx * 2 + 1).coerceIn(0, fft.lastIndex)].toInt()
                                val mag = abs(re) + abs(im)
                                (mag / 180f).coerceIn(0.08f, 1f)
                            }
                            PlaybackStateHolder.update { it.copy(visualizerLevels = levels) }
                        }
                    },
                    Visualizer.getMaxCaptureRate() / 2,
                    false,
                    true
                )
                enabled = true
            }
        }.onFailure {
            Log.w(TAG, "Visualizer capture unavailable (using fallback animation): ${it.message}")
        }
    }

    fun release() {
        runCatching { visualizer?.release() }
        visualizer = null
        currentSessionId = 0
    }

    companion object {
        private const val TAG = "AudioEffectsController"
    }
}
