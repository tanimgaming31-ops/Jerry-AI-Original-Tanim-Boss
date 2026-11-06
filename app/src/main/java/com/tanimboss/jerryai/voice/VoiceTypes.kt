// Copyright (c) 2026 Tanim Boss. See LICENSE.
package com.tanimboss.jerryai.voice

enum class VoicePhase { IDLE, CONNECTING, LISTENING, THINKING, SPEAKING, MUTED, RECONNECTING, ERROR }
enum class Persona(val label: String, val instruction: String) {
    ASSISTANT("Assistant", "Be a clear, helpful assistant."),
    FRIENDLY("Friendly", "Be warm and conversational."),
    COMPANION("Companion", "Be supportive while respecting personal boundaries."),
    PROFESSIONAL("Professional", "Be concise, accurate and professional."),
    FOCUS("Focus", "Keep answers short and focused on the current task."),
    TANIM("Tanim Boss Personal Mode", "Use a respectful personal tone for Tanim Boss. Never infer security authorization from this mode.");
    companion object {
        fun fromCommand(text: String): Persona? {
            val clean = text.trim().lowercase().replace(Regex("[.!?।]+$"), "").removePrefix("hey ").removePrefix("জেরি, ").removePrefix("jerry, ").removePrefix("jerry ")
            return entries.firstOrNull { clean == "switch to ${it.label.lowercase()} mode" || clean == "switch to ${it.label.lowercase()}" }
                ?: when (clean) {
                    "বন্ধুত্বপূর্ণ মোড চালু করো" -> FRIENDLY
                    "পেশাদার মোড চালু করো" -> PROFESSIONAL
                    "ফোকাস মোড চালু করো" -> FOCUS
                    "সহকারী মোড চালু করো" -> ASSISTANT
                    "তানিম বস মোড চালু করো" -> TANIM
                    else -> null
                }
        }
    }
}
data class VoiceConfig(val model: String = "", val voice: String = "Kore", val language: String = "en-US", val persona: Persona = Persona.ASSISTANT) {
    fun valid(): Boolean = model.removePrefix("models/").matches(Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,119}")) && voice in voices && language in languages.keys
    val modelResource: String get() = "models/" + model.removePrefix("models/")
    val instruction: String get() = "You are Jerry, an AI assistant in Jerry AI Original, owned and published by Tanim Boss. ${persona.instruction} " +
        "Respond in ${languages[language]} ($language) when the model supports it; understand multilingual input when supported. " +
        "You have no device tools. Never claim to have opened an app, sent a message, placed a call or changed a phone setting. Explain that device actions require Jerry's separate confirmed Actions screen. " +
        "Do not claim unsupported features or invent sources."
    companion object {
        val voices = listOf("Kore", "Puck", "Aoede", "Charon", "Fenrir")
        val languages = linkedMapOf("en-US" to "English", "bn-BD" to "Bengali", "hi-IN" to "Hindi / Hinglish", "ur-PK" to "Urdu", "ar-SA" to "Arabic", "fil-PH" to "Tagalog", "id-ID" to "Indonesian", "ta-IN" to "Tamil", "te-IN" to "Telugu", "kn-IN" to "Kannada", "ml-IN" to "Malayalam", "mr-IN" to "Marathi", "gu-IN" to "Gujarati", "pa-IN" to "Punjabi", "or-IN" to "Odia")
    }
}
data class VoiceState(val phase: VoicePhase = VoicePhase.IDLE, val detail: String = "Foreground voice is stopped.", val inputCaption: String = "", val outputCaption: String = "", val attempt: Int = 0)
sealed interface LiveEvent {
    data object Ready : LiveEvent
    data class Audio(val pcm: ByteArray) : LiveEvent
    data class InputText(val text: String, val finished: Boolean) : LiveEvent
    data class OutputText(val text: String) : LiveEvent
    data object Interrupted : LiveEvent
    data object TurnComplete : LiveEvent
    data class ResumeHandle(val handle: String?) : LiveEvent
    data object GoAway : LiveEvent
    data class Failure(val retryable: Boolean, val detail: String) : LiveEvent
}
interface LiveConnection {
    fun audio(pcm: ShortArray): Boolean
    fun activity(start: Boolean): Boolean
    fun audioEnd(): Boolean
    fun close()
}
fun interface LiveConnector { fun connect(key: String, config: VoiceConfig, resume: String?, onEvent: (LiveEvent) -> Unit): LiveConnection }
interface VoiceAudio {
    suspend fun open(onFocusLost: () -> Unit, onDrained: () -> Unit)
    suspend fun startMic(onPcm: (ShortArray) -> Unit, onError: () -> Unit)
    suspend fun stopMic()
    fun play(pcm: ByteArray): Boolean
    fun interrupt()
    fun busy(): Boolean
    suspend fun close()
}

/** Only harmless session controls are accepted from provider input transcripts. */
object VoiceControls {
    private fun clean(text:String)=text.trim().lowercase().replace(Regex("[.!?।]+$"),"").removePrefix("hey ").removePrefix("jerry, ").removePrefix("jerry ").removePrefix("জেরি, ").removePrefix("জেরি ")
    fun stop(text:String)=clean(text) in setOf("stop listening","stop voice","শোনা বন্ধ করো","ভয়েস বন্ধ করো")
    fun mute(text:String)=clean(text) in setOf("mute microphone","mute voice","মাইক্রোফোন বন্ধ করো","মিউট করো")
}
