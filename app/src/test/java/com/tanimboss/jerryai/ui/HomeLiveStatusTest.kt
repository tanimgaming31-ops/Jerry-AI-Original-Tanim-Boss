// Copyright (c) 2026 Tanim Boss. See LICENSE.
package com.tanimboss.jerryai.ui

import com.tanimboss.jerryai.voice.VoicePhase
import org.junit.Assert.*
import org.junit.Test

class HomeLiveStatusTest {
    @Test fun idleMustNotPretendConnected() {
        val status = homeLiveStatus(VoicePhase.IDLE)
        assertEquals("Not connected", status.label)
        assertFalse(status.active)
        assertFalse(status.pending)
    }

    @Test fun connectingMustNotPretendConnected() {
        val status = homeLiveStatus(VoicePhase.CONNECTING)
        assertEquals("Connecting", status.label)
        assertFalse(status.active)
        assertTrue(status.pending)
    }

    @Test fun reconnectingRemainsPending() {
        val status = homeLiveStatus(VoicePhase.RECONNECTING)
        assertEquals("Reconnecting", status.label)
        assertFalse(status.active)
        assertTrue(status.pending)
    }

    @Test fun errorMustNotPretendConnected() {
        val status = homeLiveStatus(VoicePhase.ERROR)
        assertEquals("Connection error", status.label)
        assertFalse(status.active)
    }

    @Test fun activeSessionStatesAreReflectedAccurately() {
        for (phase in listOf(VoicePhase.LISTENING, VoicePhase.THINKING, VoicePhase.SPEAKING, VoicePhase.MUTED)) {
            val status = homeLiveStatus(phase)
            assertTrue("Expected live status for $phase", status.active)
            assertFalse(status.pending)
            assertFalse(status.label.equals("Running", ignoreCase = true))
        }
    }

    @Test fun allVoicePhasesHaveOneHonestBadge() {
        assertEquals(VoicePhase.entries.size, VoicePhase.entries.map(::homeLiveStatus).size)
        assertTrue(VoicePhase.entries.map(::homeLiveStatus).all { it.label.isNotBlank() })
    }
}
