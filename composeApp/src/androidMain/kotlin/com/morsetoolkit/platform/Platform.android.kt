package com.morsetoolkit.platform

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import kotlin.math.PI
import kotlin.math.sin

object AndroidPlatform {
    @SuppressLint("StaticFieldLeak") // Application context only.
    lateinit var context: Context
}

actual fun createTone(): ToneOutput = AndroidTone()

actual fun createTorch(): TorchOutput = AndroidTorch(AndroidPlatform.context)

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) = BackHandler(enabled, onBack)

/**
 * Streams a sine wave on a background thread. The amplitude ramps over a few milliseconds
 * so the tone starts and stops without clicks. The thread stops after a few idle seconds.
 */
private class AndroidTone : ToneOutput {
    private val sampleRate = 44100
    @Volatile private var target = 0f
    @Volatile private var frequency = 600
    private var thread: Thread? = null
    private val lock = Any()

    override fun on(frequency: Int) {
        this.frequency = frequency
        target = 1f
        ensureRunning()
    }

    override fun off() {
        target = 0f
    }

    private fun ensureRunning() {
        synchronized(lock) {
            if (thread?.isAlive == true) return
            thread = Thread({ streamLoop() }, "morse-tone").apply {
                priority = Thread.MAX_PRIORITY
                start()
            }
        }
    }

    private fun streamLoop() {
        val minBuffer = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(minBuffer)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()
        val chunk = ShortArray(128)
        val rampStep = 1f / (sampleRate * 0.004f) // 4 ms ramp
        var amplitude = 0f
        var phase = 0.0
        var idleSamples = 0
        try {
            track.play()
            while (true) {
                val step = 2 * PI * frequency / sampleRate
                val goal = target
                for (i in chunk.indices) {
                    amplitude = when {
                        amplitude < goal -> (amplitude + rampStep).coerceAtMost(goal)
                        amplitude > goal -> (amplitude - rampStep).coerceAtLeast(goal)
                        else -> amplitude
                    }
                    chunk[i] = (sin(phase) * amplitude * 0.6 * Short.MAX_VALUE).toInt().toShort()
                    phase += step
                    if (phase > 2 * PI) phase -= 2 * PI
                }
                track.write(chunk, 0, chunk.size)
                if (goal == 0f && amplitude == 0f) {
                    idleSamples += chunk.size
                    if (idleSamples > sampleRate * 3) {
                        synchronized(lock) {
                            if (target == 0f) {
                                thread = null
                                return
                            }
                        }
                    }
                } else {
                    idleSamples = 0
                }
            }
        } finally {
            track.stop()
            track.release()
        }
    }
}

private class AndroidTorch(context: Context) : TorchOutput {
    private val manager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val cameraId: String? = try {
        manager?.cameraIdList?.firstOrNull { id ->
            manager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    } catch (e: Exception) {
        null
    }

    override val isAvailable: Boolean get() = cameraId != null

    override fun set(on: Boolean) {
        val id = cameraId ?: return
        try {
            manager?.setTorchMode(id, on)
        } catch (e: Exception) {
            // Camera in use by another app; ignore.
        }
    }
}
