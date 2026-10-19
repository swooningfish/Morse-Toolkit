package com.morsetoolkit.platform

import androidx.compose.runtime.Composable

/** Handles the system back gesture where the platform has one (Android). */
@Composable
expect fun PlatformBackHandler(enabled: Boolean = true, onBack: () -> Unit)
