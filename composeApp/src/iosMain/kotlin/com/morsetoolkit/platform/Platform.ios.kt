@file:OptIn(ExperimentalForeignApi::class)

package com.morsetoolkit.platform

import androidx.compose.runtime.Composable
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.cinterop.set
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioFormat
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioPlayerNode
import platform.AVFAudio.AVAudioPlayerNodeBufferLoops
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureTorchModeOff
import platform.AVFoundation.AVCaptureTorchModeOn
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.hasTorch
import platform.AVFoundation.torchMode
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

actual fun createTone(): ToneOutput = IosTone()

actual fun createTorch(): TorchOutput = IosTorch()

/** iOS has no system back button; the back gesture is handled by on-screen buttons. */
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) = Unit

/**
 * Loops a short buffer holding a whole number of sine cycles and switches it on and off
 * with the player's volume, which keeps latency low.
 */
private class IosTone : ToneOutput {
    private val sampleRate = 44100.0
    private val engine = AVAudioEngine()
    private val player = AVAudioPlayerNode()
    private val format = AVAudioFormat(standardFormatWithSampleRate = sampleRate, channels = 1u)
    private var started = false
    private var loadedFrequency = 0

    override fun on(frequency: Int) {
        if (!started) start()
        if (frequency != loadedFrequency) load(frequency)
        player.volume = 1f
    }

    override fun off() {
        player.volume = 0f
    }

    private fun start() {
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryPlayback, error = null)
        session.setActive(true, error = null)
        engine.attachNode(player)
        engine.connect(player, to = engine.mainMixerNode, format = format)
        engine.prepare()
        engine.startAndReturnError(null)
        started = true
    }

    private fun load(frequency: Int) {
        // 100 ms of audio; rounding the cycle count makes the loop seamless.
        val frames = (sampleRate * 0.1).roundToInt()
        val cycles = (frequency * 0.1).roundToInt().coerceAtLeast(1)
        val buffer = AVAudioPCMBuffer(pCMFormat = format, frameCapacity = frames.toUInt()) ?: return
        buffer.frameLength = frames.toUInt()
        val samples = buffer.floatChannelData?.get(0) ?: return
        for (i in 0 until frames) {
            samples[i] = (sin(2 * PI * cycles * i / frames) * 0.6).toFloat()
        }
        player.stop()
        player.volume = 0f
        player.scheduleBuffer(buffer, atTime = null, options = AVAudioPlayerNodeBufferLoops, completionHandler = null)
        player.play()
        loadedFrequency = frequency
    }
}

private class IosTorch : TorchOutput {
    private val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo!!)

    override val isAvailable: Boolean get() = device?.hasTorch == true

    override fun set(on: Boolean) {
        val d = device ?: return
        if (!d.hasTorch) return
        if (d.lockForConfiguration(null)) {
            d.torchMode = if (on) AVCaptureTorchModeOn else AVCaptureTorchModeOff
            d.unlockForConfiguration()
        }
    }
}
