// Copyright (c) 2026 Tanim Boss. See LICENSE.
package com.tanimboss.jerryai.catalog

enum class ImplementationState(val label: String) { PLANNED("Planned / unavailable"), PARTIAL("Partial / device unverified") }

data class Feature(
    val id: Int,
    val title: String,
    val phase: Int,
    val implementation: ImplementationState = ImplementationState.PLANNED,
    val limitation: String = "Not implemented. Requires its own build, tests, and device verification.",
    val deviceEvidence: String? = null,
)

/** Initial owner-supplied baseline, not a complete historical inventory. */
object FeatureCatalog {
    val features: List<Feature> = listOf(
        Feature(1, "Gemini Live Voice Assistant", 1),
        Feature(2, "Real-time Audio Streaming", 1),
        Feature(3, "Voice Interruption and Recovery", 1),
        Feature(4, "Offline Wake Word Detection", 2),
        Feature(5, "Owner Voice Recognition", 2),
        Feature(6, "Background Voice Service", 2),
        Feature(7, "Multilingual Voice and Personas", 1),
        Feature(8, "Text Chat and Conversation History", 1),
        Feature(9, "WhatsApp Contact and Messaging Assistant", 3),
        Feature(10, "YouTube Search and Playback Controls", 3),
        Feature(11, "Contact Search and Calling", 3, ImplementationState.PARTIAL, "Confirmed Android dialer intent only; no contact lookup, direct call, or call-completion verification."),
        Feature(12, "Android Phone Controls", 3, ImplementationState.PARTIAL, "Confirmed Home intent and optional accessibility Back; broader phone controls are not implemented."),
        Feature(13, "Application Launcher", 3, ImplementationState.PARTIAL, "Launch selected installed apps using Android launcher intents; foreground verification requires opted-in accessibility. Device testing pending."),
        Feature(14, "Web Search and Weather", 3),
        Feature(15, "Accessibility Voice Automation", 4, ImplementationState.PARTIAL, "Foreground Android speech command input, confirmation gates, and optional selected-app tap/swipe/scroll/text automation. Search fallback fills a search field only. Device testing pending."),
        Feature(16, "Screen Observation and Screenshot", 4, ImplementationState.PARTIAL, "Ephemeral accessible node inspection for confirmed commands only. Screenshots and general screen observation are not implemented."),
        Feature(17, "Multi-Step Task Automation", 4),
        Feature(18, "Spotify and Instagram Integration", 3),
        Feature(19, "Smart Memory and Personal Learning", 5, ImplementationState.PARTIAL, "Opt-in local save and ranked text search only; no AI-based learning, semantic retrieval or voice integration. Android test pending."),
        Feature(20, "Memory Backup and Restore", 5, ImplementationState.PARTIAL, "UTF-8 export and merge-only import through Android documents; no cloud backup or restore-overwrite. Device test pending."),
        Feature(21, "Settings and UI Customization", 0, ImplementationState.PARTIAL, "Theme preferences and original UI foundation only; broader customization is planned."),
        Feature(22, "Android Permission Management", 0, ImplementationState.PARTIAL, "Read-only permission diagnostics and settings links only; feature-specific permission flows are planned."),
        Feature(23, "Security and Privacy Controls", 0, ImplementationState.PARTIAL, "Confirmation gates, opt-in service/session, protected-screen and password checks; no network or sensitive runtime permissions. Device testing and key storage remain outstanding."),
        Feature(24, "Diagnostics and Error Recovery", 0, ImplementationState.PARTIAL, "Action rejection/timeout/cancellation and observed-vs-unverified outcomes; voice-session recovery is not implemented."),
    )
}
