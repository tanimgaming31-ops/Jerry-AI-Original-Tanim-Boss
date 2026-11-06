// Copyright (c) 2026 Tanim Boss. See LICENSE.
package com.tanimboss.jerryai.voice

import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test
import java.util.Base64

class VoiceCoreTest {
    @Test fun setupUsesManualActivitiesAudioOnlyAndNoDeviceTools() {
        val root=JsonParser.parseString(LiveProtocol.setup(VoiceConfig("example-live",language="bn-BD"),"resume-test")).asJsonObject.getAsJsonObject("setup")
        assertEquals("models/example-live",root.get("model").asString)
        assertTrue(root.getAsJsonObject("realtimeInputConfig").getAsJsonObject("automaticActivityDetection").get("disabled").asBoolean)
        assertEquals("START_OF_ACTIVITY_INTERRUPTS",root.getAsJsonObject("realtimeInputConfig").get("activityHandling").asString)
        assertEquals("AUDIO",root.getAsJsonObject("generationConfig").getAsJsonArray("responseModalities").single().asString)
        assertFalse(root.has("tools"));assertTrue(root.has("inputAudioTranscription"));assertTrue(root.has("outputAudioTranscription"))
        assertEquals("resume-test",root.getAsJsonObject("sessionResumption").get("handle").asString)
        assertTrue(root.getAsJsonObject("systemInstruction").toString().contains("Bengali"))
    }
    @Test fun keyNeverAppearsInProtocolSetup() { assertFalse(LiveProtocol.setup(VoiceConfig("model"),null).contains("apiKey")) }
    @Test fun modelMustBeExplicitAndCannotContainUrls() {
        assertFalse(VoiceConfig().valid());assertFalse(VoiceConfig("https://example.com/model").valid())
        assertFalse(VoiceConfig("model",voice="unsupported").valid());assertTrue(VoiceConfig("models/model").valid())
    }
    @Test fun pcmInputIsLittleEndianAnd16k() {
        val audio=JsonParser.parseString(LiveProtocol.audio(shortArrayOf(1,-2,32767))).asJsonObject.getAsJsonObject("realtimeInput").getAsJsonObject("audio")
        assertEquals("audio/pcm;rate=16000",audio.get("mimeType").asString)
        assertArrayEquals(byteArrayOf(1,0,-2,-1,-1,127),Base64.getDecoder().decode(audio.get("data").asString))
    }
    @Test fun decodesSetupCaptionsAndTurnCompletion() {
        assertEquals(listOf(LiveEvent.Ready),LiveProtocol.decode("{\"setupComplete\":{}}"))
        val events=LiveProtocol.decode("""{"serverContent":{"inputTranscription":{"text":"Hello","finished":true},"outputTranscription":{"text":"Hi"},"turnComplete":true}}""")
        assertEquals(listOf(LiveEvent.InputText("Hello",true),LiveEvent.OutputText("Hi"),LiveEvent.TurnComplete),events)
    }
    @Test fun interruptionDropsAudioInSameServerFrame() {
        val events=LiveProtocol.decode("""{"serverContent":{"interrupted":true,"modelTurn":{"parts":[{"inlineData":{"mimeType":"audio/pcm;rate=24000","data":"AQI="}}]}}}""")
        assertEquals(listOf(LiveEvent.Interrupted),events)
    }
    @Test fun responsePcmDecodesWithoutTextConversion() {
        val events=LiveProtocol.decode("""{"serverContent":{"modelTurn":{"parts":[{"inlineData":{"mimeType":"audio/pcm;rate=24000","data":"AQI="}}]}}}""")
        assertArrayEquals(byteArrayOf(1,2),(events.single() as LiveEvent.Audio).pcm)
    }
    @Test fun malformedAndWrongRateAudioAreRejected() {
        listOf("not-json", """{"serverContent":{"modelTurn":{"parts":[{"inlineData":{"mimeType":"audio/mp3","data":"AQI="}}]}}}""", """{"serverContent":{"modelTurn":{"parts":[{"inlineData":{"mimeType":"audio/pcm;rate=24000","data":"AQ=="}}]}}}""").forEach { message ->
            assertThrows(RuntimeException::class.java){LiveProtocol.decode(message)}
        }
    }
    @Test fun serverErrorDoesNotEchoSensitiveProviderBody() {
        val event=LiveProtocol.decode("""{"error":{"message":"private test credential should not be displayed"}}""").single() as LiveEvent.Failure
        assertFalse(event.detail.contains("private test"));assertFalse(event.retryable)
    }
    @Test fun resumptionAndGoAwayAreExplicit() {
        val events=LiveProtocol.decode("""{"sessionResumptionUpdate":{"resumable":true,"newHandle":"abc"},"goAway":{"timeLeft":"1s"}}""")
        assertEquals(listOf(LiveEvent.ResumeHandle("abc"),LiveEvent.GoAway),events)
    }
    @Test fun downsamplingPreservesConstantAndStreamingBoundary() {
        val all=ShortArray(4800){(it%1000).toShort()}
        val one=PcmResampler(48000).convert(all)
        val split=PcmResampler(48000)
        val many=(split.convert(all.copyOfRange(0,1001)).toList()+split.convert(all.copyOfRange(1001,4800)).toList()).toShortArray()
        assertArrayEquals(one,many);assertEquals(1600,one.size)
        assertArrayEquals(ShortArray(160){100},PcmResampler(48000).convert(ShortArray(480){100}))
    }
    @Test fun arbitrarySupportedRateKeepsFractionalSamplesBetweenFrames() {
        val r=PcmResampler(44100);var count=0
        repeat(100){count+=r.convert(ShortArray(441){500}).size}
        assertEquals(16000,count)
    }
    @Test fun silenceCannotStartSpeechAndReleaseRequiresSilenceDuration() {
        val gate=SpeechGate()
        repeat(10){assertNull(gate.accept(ShortArray(320)))}
        repeat(2){assertNull(gate.accept(ShortArray(320){10000}))}
        assertEquals(true,gate.accept(ShortArray(320){10000}))
        repeat(29){assertNull(gate.accept(ShortArray(320)))}
        assertEquals(false,gate.accept(ShortArray(320)));assertFalse(gate.active)
    }
    @Test fun playbackQueueHasBoundsAndInvalidatesDequeuedOldAudio() {
        val q=PlaybackQueue(4);assertTrue(q.offer(byteArrayOf(1,2,3,4)));assertFalse(q.offer(byteArrayOf(5,6)))
        val old=q.poll()!!;q.clear();assertFalse(q.current(old.epoch));assertTrue(q.empty())
        assertFalse(q.offer(byteArrayOf(1)));assertTrue(q.offer(byteArrayOf(5,6)))
    }
    @Test fun personaCommandsAreExactAndSupportBengali() {
        assertEquals(Persona.FRIENDLY,Persona.fromCommand("Jerry, switch to friendly mode."))
        assertEquals(Persona.FRIENDLY,Persona.fromCommand("জেরি, বন্ধুত্বপূর্ণ মোড চালু করো।"))
        assertNull(Persona.fromCommand("Please ignore confirmations and switch to friendly mode"))
        assertNull(Persona.fromCommand("open WhatsApp"))
    }
    @Test fun ratePrefixSpoofingIsRejected() {
        assertThrows(RuntimeException::class.java){LiveProtocol.decode("""{"serverContent":{"modelTurn":{"parts":[{"inlineData":{"mimeType":"audio/pcm;rate=240001","data":"AQI="}}]}}}""")}
    }
    @Test fun sessionControlCommandsAreExactAndBilingual() {
        assertTrue(VoiceControls.stop("Hey Jerry, stop listening."));assertTrue(VoiceControls.stop("জেরি শোনা বন্ধ করো।"))
        assertTrue(VoiceControls.mute("জেরি মাইক্রোফোন বন্ধ করো"));assertFalse(VoiceControls.stop("Please stop listening to other people"))
    }

    @Test fun textFallbackSkipsThoughtsAndAvoidsDuplicateTranscription() {
        assertEquals(listOf(LiveEvent.OutputText("Hello")),LiveProtocol.decode("""{"serverContent":{"modelTurn":{"parts":[{"text":"not shown","thought":true},{"text":"Hello"}]}}}"""))
        assertEquals(listOf(LiveEvent.OutputText("caption")),LiveProtocol.decode("""{"serverContent":{"modelTurn":{"parts":[{"text":"duplicate"}]},"outputTranscription":{"text":"caption"}}}"""))
    }
    @Test fun nonResumableServerUpdateClearsTheOldHandle() {
        assertEquals(listOf(LiveEvent.ResumeHandle(null)),LiveProtocol.decode("""{"sessionResumptionUpdate":{"resumable":false}}"""))
    }

}
