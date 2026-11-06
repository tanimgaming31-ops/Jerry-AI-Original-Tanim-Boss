// Copyright (c) 2026 Tanim Boss. See LICENSE.
package com.tanimboss.jerryai.catalog

enum class ImplementationState(val label:String) {
    WORKING_DEVICE_VERIFIED("Working and device-verified"), IMPLEMENTED_DEVICE_UNVERIFIED("Implemented; provider/device unverified"),
    PARTIAL("Partially implemented"), PLANNED("Planned"), UNSUPPORTED("Unsupported"), FAILED("Failed")
}
data class Feature(val id:Int,val title:String,val phase:Int,val implementation:ImplementationState=ImplementationState.PLANNED,
    val limitation:String="Not implemented.",val deviceEvidence:String?=null) {
    init { require(implementation!=ImplementationState.WORKING_DEVICE_VERIFIED || !deviceEvidence.isNullOrBlank()) }
}
/** Canonical master inventory. Earlier 24-group IDs were not persisted; archived inventory remains in docs. */
object FeatureCatalog {
    val features:List<Feature> = listOf(
        Feature(1, "Gemini Live Voice Assistant", 1, ImplementationState.IMPLEMENTED_DEVICE_UNVERIFIED, "Original authenticated v1beta Live WebSocket, model discovery, setup, sanitized failures and memory-only resumption. Provider/key/model and physical-phone acceptance remain unverified."),
        Feature(2, "Real-Time Bidirectional Audio", 1, ImplementationState.IMPLEMENTED_DEVICE_UNVERIFIED, "Foreground PCM16 microphone capture, 16 kHz stream/resampling, 24 kHz playback, bounded buffers, focus and optional AEC. Hardware/network audio quality remains unverified."),
        Feature(3, "Voice Interruptions and Barge-In", 1, ImplementationState.IMPLEMENTED_DEVICE_UNVERIFIED, "Local activity detection, immediate playback invalidation, manual activity/barge-in signals and stale callback guards. Real Gemini cancellation/echo behavior needs phone testing."),
        Feature(4, "Offline Wake Word", 2, ImplementationState.PLANNED, "Not implemented. Retained in the structured backlog; no working or device-verification claim."),
        Feature(5, "Foreground Voice Service", 2, ImplementationState.PLANNED, "Not implemented. Retained in the structured backlog; no working or device-verification claim."),
        Feature(6, "Background Voice Assistant", 2, ImplementationState.PLANNED, "Not implemented. Retained in the structured backlog; no working or device-verification claim."),
        Feature(7, "Offline Voice Commands", 2, ImplementationState.PLANNED, "Not implemented. Retained in the structured backlog; no working or device-verification claim."),
        Feature(8, "Advanced Voice Session Management", 1, ImplementationState.IMPLEMENTED_DEVICE_UNVERIFIED, "Serialized session states, mute/stop, bounded retries, setup timeout, focus-loss shutdown and foreground lifecycle cleanup. OS behavior remains unverified."),
        Feature(9, "Multilingual Voice Understanding", 1, ImplementationState.PARTIAL, "Persistent language requests for the listed language families and provider input/output captions. Full language recognition/output coverage is model-dependent and has not been verified."),
        Feature(10, "AI Voice and Personality Modes", 1, ImplementationState.IMPLEMENTED_DEVICE_UNVERIFIED, "Five provider voice names, six persona modes, saved preferences and exact English/Bengali input-transcript mode switching. Provider voice availability and actual persona output remain unverified."),
        Feature(11, "Advanced AI Text Chat", 3, ImplementationState.PLANNED, "Not implemented. Retained in the structured backlog; no working or device-verification claim."),
        Feature(12, "Persistent Chat History", 3, ImplementationState.PLANNED, "Not implemented. Retained in the structured backlog; no working or device-verification claim."),
        Feature(13, "Smart Memory Bank", 3, ImplementationState.PLANNED, "Not implemented. Retained in the structured backlog; no working or device-verification claim."),
        Feature(14, "Personal Learning Engine", 3, ImplementationState.PLANNED, "Not implemented. Retained in the structured backlog; no working or device-verification claim."),
        Feature(15, "Memory Backup and Restore", 3, ImplementationState.PLANNED, "Not implemented. Retained in the structured backlog; no working or device-verification claim."),
        Feature(16, "Conversation Summaries", 3, ImplementationState.PLANNED, "Not implemented. Retained in the structured backlog; no working or device-verification claim."),
        Feature(17, "Personal Knowledge Search", 3, ImplementationState.PLANNED, "Not implemented. Retained in the structured backlog; no working or device-verification claim."),
        Feature(18, "AI Planning and Decision Assistant", 3, ImplementationState.PLANNED, "Not implemented. Retained in the structured backlog; no working or device-verification claim."),
        Feature(19, "Universal App Launcher", 4, ImplementationState.PARTIAL, "Selected launchable app intents and legacy foreground command input retained. Universal spoken-name resolution and all app/profile/clone cases remain pending."),
        Feature(20, "Advanced Phone Controls", 4, ImplementationState.PARTIAL, "Confirmed Home intent and optional accessibility Back retained. Flashlight, brightness, media/volume and broader controls remain pending."),
        Feature(21, "Contact Search and Calling", 4, ImplementationState.PARTIAL, "Confirmed dialer intent retained; contact lookup, ambiguity resolution and actual calling are not implemented."),
        Feature(22, "WhatsApp Voice Assistant", 4, ImplementationState.PLANNED, "Not implemented. Retained in the structured backlog; no working or device-verification claim."),
        Feature(23, "IMO and Messenger Assistant", 4, ImplementationState.PLANNED, "Not implemented. Retained in the structured backlog; no working or device-verification claim."),
        Feature(24, "YouTube Search and Playback", 4, ImplementationState.PLANNED, "Not implemented. Retained in the structured backlog; no working or device-verification claim."),
        Feature(25, "Spotify and Media Assistant", 4, ImplementationState.PLANNED, "Not implemented. Retained in the structured backlog; no working or device-verification claim."),
        Feature(26, "Notification Reader and Reply Manager", 4, ImplementationState.PLANNED, "Not implemented. Retained in the structured backlog; no working or device-verification claim."),
        Feature(27, "Screenshot and Screen Capture", 4, ImplementationState.PLANNED, "Not implemented. Retained in the structured backlog; no working or device-verification claim."),
        Feature(28, "Camera and Smart Vision", 4, ImplementationState.PLANNED, "Not implemented. Retained in the structured backlog; no working or device-verification claim."),
        Feature(29, "Accessibility Voice Control", 5, ImplementationState.PARTIAL, "Existing optional user-enabled selected-app tap/swipe/scroll/text and navigation retained, with confirmation and observed/unverified outcomes. Layout compatibility remains unverified."),
        Feature(30, "Multi-Step App Macros", 5, ImplementationState.PLANNED, "Not implemented. Retained in the structured backlog; no working or device-verification claim."),
        Feature(31, "Smart Task Execution Engine", 5, ImplementationState.PLANNED, "Not implemented. Retained in the structured backlog; no working or device-verification claim."),
        Feature(32, "Voice Command Learning", 5, ImplementationState.PLANNED, "Not implemented. Retained in the structured backlog; no working or device-verification claim."),
        Feature(33, "Smart Clipboard Assistant", 5, ImplementationState.PLANNED, "Not implemented. Retained in the structured backlog; no working or device-verification claim."),
        Feature(34, "Advanced File Manager", 5, ImplementationState.PLANNED, "Not implemented. Retained in the structured backlog; no working or device-verification claim."),
        Feature(35, "Smart Routines", 5, ImplementationState.PLANNED, "Not implemented. Retained in the structured backlog; no working or device-verification claim."),
        Feature(36, "Alarms and Reminders", 5, ImplementationState.PLANNED, "Not implemented. Retained in the structured backlog; no working or device-verification claim."),
        Feature(37, "Screen Understanding Assistant", 6, ImplementationState.PARTIAL, "Only bounded transient accessibility-node checks for confirmed actions. No general screen summarizer or screenshot pipeline."),
        Feature(38, "PDF and Document Reader", 6, ImplementationState.PLANNED, "Not implemented. Retained in the structured backlog; no working or device-verification claim."),
        Feature(39, "QR and Barcode Scanner", 6, ImplementationState.PLANNED, "Not implemented. Retained in the structured backlog; no working or device-verification claim."),
        Feature(40, "Receipt Scanner and Expense Tracker", 6, ImplementationState.PLANNED, "Not implemented. Retained in the structured backlog; no working or device-verification claim."),
        Feature(41, "Real-Time Voice Captions", 6, ImplementationState.PARTIAL, "Live input/output transcription captions in the foreground session only. No continuous independent caption utility; recognition needs provider/phone tests."),
        Feature(42, "Live Translator and Interpreter", 6, ImplementationState.PLANNED, "Not implemented. Retained in the structured backlog; no working or device-verification claim."),
        Feature(43, "Weather, News and Information", 6, ImplementationState.PLANNED, "Not implemented. Retained in the structured backlog; no working or device-verification claim."),
        Feature(44, "Interactive Learning Assistant", 6, ImplementationState.PLANNED, "Not implemented. Retained in the structured backlog; no working or device-verification claim."),
        Feature(45, "Original Animated Home Interface", 7, ImplementationState.PARTIAL, "Original existing Compose home preserved and Voice screen added. Futuristic orb/waveform/transitions are planned."),
        Feature(46, "Avatar Customization", 7, ImplementationState.PLANNED, "Not implemented. Retained in the structured backlog; no working or device-verification claim."),
        Feature(47, "Floating Jerry Shortcut", 7, ImplementationState.PLANNED, "Not implemented. Retained in the structured backlog; no working or device-verification claim."),
        Feature(48, "Advanced Settings Dashboard", 7, ImplementationState.PARTIAL, "Existing themes/settings plus Voice model/key/language/persona configuration. Memory, notifications, security and backup dashboards expand in later phases."),
        Feature(49, "Permission Diagnostics Dashboard", 7, ImplementationState.PARTIAL, "Real declared/runtime microphone state, other permission states and accessibility-session diagnostics. Camera/capture/notification feature flows remain pending."),
        Feature(50, "AI Self-Diagnostics and Recovery", 7, ImplementationState.PARTIAL, "Sanitized Live network/auth/model/quota failures, retry states, microphone/focus handling and existing command diagnostics. Structured diagnostic export/self-repair remains pending."),
        Feature(51, "Security and Owner Protection", 7, ImplementationState.PARTIAL, "Keystore-backed API-key encryption, explicit microphone start, scoped confirmations/password checks and no automatic AI device tools. Owner biometrics and broader privacy alerts remain pending."),
        Feature(52, "Backup, Restore and Data Migration", 7, ImplementationState.PLANNED, "Not implemented. Retained in the structured backlog; no working or device-verification claim."),
        Feature(53, "About Me, Ownership and Legal Notices", 7, ImplementationState.IMPLEMENTED_DEVICE_UNVERIFIED, "Original ownership/About UI, correct app identity, proprietary original-material notice and preserved third-party licenses. Phone rendering still unverified."),
        Feature(54, "Complete Feature Status, Testing and Updates", 7, ImplementationState.PARTIAL, "All 54 groups and six truthful status categories, structured backlog, version/change documentation, local tests/build evidence and workflow. Device verification and updater are pending."),
    )
}
