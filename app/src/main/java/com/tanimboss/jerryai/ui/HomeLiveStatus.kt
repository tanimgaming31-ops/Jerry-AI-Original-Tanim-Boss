// Copyright (c) 2026 Tanim Boss. See LICENSE.
package com.tanimboss.jerryai.ui

import com.tanimboss.jerryai.voice.VoicePhase

/** Display-only status driven by the real voice session, never a demo "connected" badge. */
internal data class HomeLiveStatus(val label: String, val active: Boolean, val pending: Boolean = false)

internal fun homeLiveStatus(phase: VoicePhase): HomeLiveStatus = when (phase) {
    VoicePhase.LISTENING -> HomeLiveStatus("Listening", true)
    VoicePhase.THINKING -> HomeLiveStatus("Thinking", true)
    VoicePhase.SPEAKING -> HomeLiveStatus("Speaking", true)
    VoicePhase.MUTED -> HomeLiveStatus("Muted", true)
    VoicePhase.CONNECTING -> HomeLiveStatus("Connecting", false, true)
    VoicePhase.RECONNECTING -> HomeLiveStatus("Reconnecting", false, true)
    VoicePhase.ERROR -> HomeLiveStatus("Connection error", false)
    VoicePhase.IDLE -> HomeLiveStatus("Not connected", false)
}
