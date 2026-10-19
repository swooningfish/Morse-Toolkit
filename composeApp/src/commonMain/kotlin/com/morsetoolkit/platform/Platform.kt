package com.morsetoolkit.platform

/** A sine tone that can be switched on and off with low latency. */
interface ToneOutput {
    fun on(frequency: Int)
    fun off()
}

/** The phone's camera flashlight. */
interface TorchOutput {
    val isAvailable: Boolean
    fun set(on: Boolean)
}

expect fun createTone(): ToneOutput

expect fun createTorch(): TorchOutput
