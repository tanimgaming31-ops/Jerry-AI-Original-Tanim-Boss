// Copyright (c) 2026 Tanim Boss. See LICENSE.
package com.tanimboss.jerryai.voice

import com.google.gson.*
import java.util.Base64

/** Native Gemini Developer API v1beta Live wire format; no tool execution is configured. */
object LiveProtocol {
    private val gson = GsonBuilder().setStrictness(Strictness.STRICT).create()
    private fun obj(vararg fields: Pair<String, JsonElement>) = JsonObject().apply { fields.forEach { add(it.first, it.second) } }
    private fun str(value: String) = JsonPrimitive(value)
    fun setup(config: VoiceConfig, resume: String?): String {
        require(config.valid())
        val resumption = JsonObject().apply { if (!resume.isNullOrBlank()) addProperty("handle", resume) }
        val speech = obj("voiceConfig" to obj("prebuiltVoiceConfig" to obj("voiceName" to str(config.voice))))
        val generation = obj("responseModalities" to JsonArray().apply { add("AUDIO") }, "speechConfig" to speech)
        val setup = obj(
            "model" to str(config.modelResource), "generationConfig" to generation,
            "systemInstruction" to obj("parts" to JsonArray().apply { add(obj("text" to str(config.instruction))) }),
            "realtimeInputConfig" to obj("automaticActivityDetection" to obj("disabled" to JsonPrimitive(true)), "activityHandling" to str("START_OF_ACTIVITY_INTERRUPTS")),
            "inputAudioTranscription" to JsonObject(), "outputAudioTranscription" to JsonObject(), "sessionResumption" to resumption,
        )
        return gson.toJson(obj("setup" to setup))
    }
    fun audio(pcm: ShortArray): String {
        require(pcm.isNotEmpty() && pcm.size <= 16000)
        return gson.toJson(obj("realtimeInput" to obj("audio" to obj("mimeType" to str("audio/pcm;rate=16000"), "data" to str(Base64.getEncoder().encodeToString(Pcm16.encode(pcm)))))))
    }
    fun activity(start: Boolean): String = gson.toJson(obj("realtimeInput" to obj((if (start) "activityStart" else "activityEnd") to JsonObject())))
    fun audioEnd(): String = "{\"realtimeInput\":{\"audioStreamEnd\":true}}"
    fun decode(message: String): List<LiveEvent> {
        require(message.length <= 2_000_000) { "Oversized Live frame" }
        val root = gson.fromJson(message, JsonObject::class.java) ?: error("Empty Live frame")
        val events = mutableListOf<LiveEvent>()
        if (root.has("error")) return listOf(LiveEvent.Failure(false, "Gemini rejected the session. Check the model, key, quota and account availability."))
        if (root.has("setupComplete")) events += LiveEvent.Ready
        root.getAsJsonObject("sessionResumptionUpdate")?.let {
            if (it.get("resumable")?.asBoolean == true) it.get("newHandle")?.asString?.takeIf { h -> h.length <= 4096 }?.let { h -> events += LiveEvent.ResumeHandle(h) }
            else if(it.get("resumable")?.asBoolean == false)events += LiveEvent.ResumeHandle(null)
        }
        if (root.has("goAway")) events += LiveEvent.GoAway
        root.getAsJsonObject("serverContent")?.let { content ->
            // Interruption invalidates co-delivered old audio before it can be played.
            if (content.get("interrupted")?.asBoolean == true) {
                events += LiveEvent.Interrupted
            } else {
                content.getAsJsonObject("modelTurn")?.getAsJsonArray("parts")?.forEach { part ->
                    val item = part.asJsonObject
                    if(item.get("thought")?.asBoolean == true)return@forEach
                    if(!content.has("outputTranscription"))item.get("text")?.asString?.let {events += LiveEvent.OutputText(it.take(4000))}
                    item.getAsJsonObject("inlineData")?.let { inline ->
                        val mime = inline.get("mimeType")?.asString.orEmpty()
                        val attributes=mime.split(';').drop(1).associate { val p=it.trim().split('=',limit=2);require(p.size==2);p[0] to p[1] }
                        require(mime.substringBefore(';') == "audio/pcm" && attributes["rate"]=="24000" && attributes["channels"] in setOf(null,"1")) { "Unsupported response audio format" }
                        val pcm = Base64.getDecoder().decode(inline.get("data").asString)
                        require(pcm.isNotEmpty() && pcm.size % 2 == 0 && pcm.size <= 1_000_000)
                        events += LiveEvent.Audio(pcm)
                    }
                }
            }
            content.getAsJsonObject("inputTranscription")?.let { events += LiveEvent.InputText(it.get("text")?.asString.orEmpty().take(4000), it.get("finished")?.asBoolean == true) }
            content.getAsJsonObject("outputTranscription")?.get("text")?.asString?.let { events += LiveEvent.OutputText(it.take(4000)) }
            if (content.get("turnComplete")?.asBoolean == true) events += LiveEvent.TurnComplete
        }
        return events
    }
}

object Pcm16 {
    fun encode(input: ShortArray) = ByteArray(input.size * 2).also { out -> input.forEachIndexed { i, v -> out[i*2] = v.toByte(); out[i*2+1] = (v.toInt() shr 8).toByte() } }
}

/** Streaming area-average resampling. Carries fractional input across frames. */
class PcmResampler(inputRate: Int, outputRate: Int = 16000) {
    private val width = inputRate.toDouble() / outputRate
    private var filled = 0.0
    private var sum = 0.0
    init { require(inputRate > 0 && outputRate > 0) }
    fun convert(input: ShortArray): ShortArray {
        val output = ArrayList<Short>()
        input.forEach { sample ->
            var left = 1.0
            while (left > 1e-9) {
                val amount = minOf(left, width - filled)
                sum += sample * amount; filled += amount; left -= amount
                if (filled >= width - 1e-9) { output += (sum / width).toInt().coerceIn(-32768,32767).toShort(); filled = 0.0; sum = 0.0 }
            }
        }
        return output.toShortArray()
    }
}

/** Local manual activity detection, not a wake-word or speaker-recognition engine. */
class SpeechGate(private val threshold: Double = .025, private val attack: Int = 3, private val releaseSamples: Int = 9600) {
    var active = false; private set
    private var voiced = 0; private var silence = 0
    fun reset() { active = false; voiced = 0; silence = 0 }
    fun forceStart() { active = true; silence = 0; voiced = 0 }
    fun accept(pcm: ShortArray): Boolean? {
        if (pcm.isEmpty()) return null
        val rms = kotlin.math.sqrt(pcm.sumOf { val s=it.toDouble()/32768; s*s } / pcm.size)
        if (rms >= threshold) {
            silence = 0; voiced++
            if (!active && voiced >= attack) { active = true; return true }
        } else {
            voiced = 0
            if (active) { silence += pcm.size; if (silence >= releaseSamples) { reset(); return false } }
        }
        return null
    }
}

class PlaybackQueue(private val maxBytes: Int = 384000) {
    data class Chunk(val epoch: Long, val bytes: ByteArray)
    private val chunks = ArrayDeque<Chunk>()
    var epoch: Long = 0; private set
    private var bytes = 0
    @Synchronized fun offer(pcm: ByteArray): Boolean {
        if (pcm.isEmpty() || pcm.size % 2 != 0 || bytes + pcm.size > maxBytes) return false
        chunks.addLast(Chunk(epoch, pcm)); bytes += pcm.size; return true
    }
    @Synchronized fun poll(): Chunk? = chunks.removeFirstOrNull()?.also { bytes -= it.bytes.size }
    @Synchronized fun clear() { chunks.clear(); bytes = 0; epoch++ }
    @Synchronized fun current(value: Long): Boolean = epoch == value
    @Synchronized fun empty(): Boolean = chunks.isEmpty()
}
