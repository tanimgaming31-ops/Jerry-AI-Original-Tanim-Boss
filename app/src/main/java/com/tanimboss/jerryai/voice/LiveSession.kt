// Copyright (c) 2026 Tanim Boss. See LICENSE.
package com.tanimboss.jerryai.voice

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Serialized session owner: one connection, one microphone, bounded retries, no replay of captured audio. */
class LiveSession(private val scope: CoroutineScope, private val connector: LiveConnector, private val audio: VoiceAudio,
    private val onPersona: (Persona) -> Unit = {}) {
    private val lock=Mutex()
    private val mutableState=MutableStateFlow(VoiceState())
    val state=mutableState.asStateFlow()
    private var connection: LiveConnection?=null
    private var retryJob: Job?=null; private var setupJob: Job?=null; private var responseJob:Job?=null
    private var active=false; private var ready=false; private var muted=false
    private var generation=0L; private var retries=0
    private var lifetime=0L
    private var awaitingInput=false
    private var key=""; private var config=VoiceConfig(); private var handle: String?=null
    private val gate=SpeechGate();private val preRoll=ArrayDeque<ShortArray>()
    private var suppress=false;private var responseDone=true;private var input=""

    private data class MicFrame(val connection:Long,val capture:Long,val pcm:ShortArray)
    private var micGeneration=0L
    private val micFrames=Channel<MicFrame>(8)
    private val events=Channel<Pair<Long,LiveEvent>>(32)
    init {
        scope.launch { for(frame in micFrames) lock.withLock { if(active && ready && !muted && frame.connection==generation && frame.capture==micGeneration) safely { microphoneLocked(frame.pcm) } } }
        scope.launch { for((g,event) in events) lock.withLock { if(active && g==generation) safely { eventLocked(event,g) } } }
    }
    private suspend fun safely(block:suspend ()->Unit) {
        try {block()}catch(cancelled:CancellationException){throw cancelled}
        catch(_:Exception){failLocked("Voice processing or audio control failed. Session stopped; no private provider details were logged.")}
    }
    private suspend fun cancelOther(job:Job?) { if(job!=null && job!==currentCoroutineContext()[Job])job.cancel() }
    suspend fun start(apiKey: String, settings: VoiceConfig) = lock.withLock {
        stopLocked()
        if (apiKey.isBlank() || !settings.valid()) { mutableState.value=VoiceState(VoicePhase.ERROR,"Save a Gemini key and choose a valid Live model first.");return@withLock }
        key=apiKey;config=settings;active=true;muted=false;retries=0;handle=null
        val owner=lifetime
        try {
            audio.open({ scope.launch { lock.withLock { if (active && owner==lifetime) failLocked("Audio focus or output became unavailable. Session stopped; restart when other audio ends.") } } },
                { val g=generation;scope.launch { lock.withLock { if(active && owner==lifetime && g==generation && responseDone && !gate.active && !muted) update(VoicePhase.LISTENING,"Listening; response playback drained.") } } })
            connectLocked(false)
        } catch (cancelled:CancellationException) { stopLocked();throw cancelled }
        catch (_: Exception) { failLocked("Audio resources could not be opened. Check audio focus and device support.") }
    }
    private fun update(phase: VoicePhase, detail: String) { mutableState.value=mutableState.value.copy(phase=phase,detail=detail,attempt=retries) }
    private fun connectLocked(reconnecting: Boolean) {
        ready=false;generation++;val g=generation;gate.reset();preRoll.clear();input="";suppress=false;responseDone=true;awaitingInput=muted
        update(if(reconnecting) VoicePhase.RECONNECTING else VoicePhase.CONNECTING,if(reconnecting) "Reconnecting; microphone paused. Context resumes only if Gemini accepts the in-memory handle." else "Connecting to Gemini; microphone has not started.")
        val eventOverflow=AtomicBoolean(false)
        connection=connector.connect(key,config,handle) { event ->
            if(!events.trySend(g to event).isSuccess && eventOverflow.compareAndSet(false,true))scope.launch { lock.withLock { if(active && g==generation)failLocked("Incoming event buffer overflow. Session stopped rather than accumulating audio.") } }
        }
        setupJob?.cancel();setupJob=scope.launch { delay(15_000);lock.withLock { if(active && g==generation && !ready) reconnectLocked("Gemini setup timed out.") } }
    }
    private suspend fun responseDeadlineLocked() {
        cancelOther(responseJob)
        val g=generation
        responseJob=scope.launch { delay(45_000);lock.withLock { if(active && g==generation && !responseDone && !gate.active)failLocked("Gemini response timed out. Session stopped; restart explicitly after checking provider availability.") } }
    }
    private suspend fun eventLocked(event: LiveEvent, g: Long) {
        when(event) {
            LiveEvent.Ready -> {
                if(ready)return
                ready=true;setupJob?.cancel()
                if(!muted) startMicLocked(g)
                update(if(muted) VoicePhase.MUTED else VoicePhase.LISTENING,"Connected. Speak naturally; model/voice/language support depends on Gemini.")
            }
            is LiveEvent.Audio -> if(ready && !muted && !awaitingInput && !suppress && !gate.active) {
                responseDone=false;responseDeadlineLocked()
                if(!audio.play(event.pcm)) failLocked("Playback buffer overflow or audio output failure. Session stopped; no response was silently dropped.")
                else update(VoicePhase.SPEAKING,"Jerry is speaking; speak over the response or press Interrupt.")
            }
            is LiveEvent.InputText -> {
                input=(input+event.text).takeLast(4000)
                mutableState.value=mutableState.value.copy(inputCaption=input)
                if(event.finished) inputControlLocked()
            }
            is LiveEvent.OutputText -> {mutableState.value=mutableState.value.copy(outputCaption=(mutableState.value.outputCaption+event.text).takeLast(4000));if(!responseDone)responseDeadlineLocked()}
            LiveEvent.Interrupted -> { audio.interrupt();suppress=false;responseDone=true;update(if(muted) VoicePhase.MUTED else VoicePhase.LISTENING,"Response interrupted; stale playback discarded.") }
            LiveEvent.TurnComplete -> { responseDone=true;suppress=false;cancelOther(responseJob);if(!gate.active && inputControlLocked())return;if(!muted && !gate.active && !audio.busy()) update(VoicePhase.LISTENING,"Listening.") }
            is LiveEvent.ResumeHandle -> handle=event.handle
            LiveEvent.GoAway -> reconnectLocked("Gemini requested a connection handoff.")
            is LiveEvent.Failure -> if(event.retryable) reconnectLocked(event.detail) else failLocked(event.detail)
        }
    }
    private suspend fun inputControlLocked():Boolean {
        if(VoiceControls.stop(input)){stopLocked();return true}
        if(VoiceControls.mute(input)){input="";muteLocked(true);return true}
        val persona=Persona.fromCommand(input)
        if(persona!=null && persona!=config.persona){
            config=config.copy(persona=persona);input="";onPersona(persona);handle=null
            reconnectLocked("Persona changed; a new conversation context will start.",immediate=true)
            return true
        }
        return false
    }
    private suspend fun startMicLocked(g: Long) {
        val overflow=AtomicBoolean(false)
        val capture=++micGeneration
        audio.startMic({ pcm ->
            if(!micFrames.trySend(MicFrame(g,capture,pcm)).isSuccess && overflow.compareAndSet(false,true))scope.launch { lock.withLock { if(active && g==generation)failLocked("Microphone processing buffer overflow. Session stopped.") } }
        },
            { scope.launch { lock.withLock { if(active && g==generation && capture==micGeneration) failLocked("Microphone failed or permission was revoked. Session stopped.") } } })
    }
    private suspend fun microphoneLocked(pcm: ShortArray) {
        preRoll.addLast(pcm);while(preRoll.size>5)preRoll.removeFirst()
        when(gate.accept(pcm)) {
            true -> {
                awaitingInput=false
                cancelOther(responseJob)
                suppress=!responseDone
                audio.interrupt();input=""
                mutableState.value=mutableState.value.copy(inputCaption="",outputCaption="")
                if(connection?.activity(true)!=true) { reconnectLocked("Could not send speech-start signal.");return }
                update(VoicePhase.LISTENING,"Your speech interrupted playback; listening.")
                for(frame in preRoll)if(connection?.audio(frame)!=true) { reconnectLocked("Audio send buffer is full or connection lost.");return }
                preRoll.clear()
            }
            false -> {
                preRoll.clear()
                if(connection?.activity(false)!=true)reconnectLocked("Could not finish the input turn.")
                else {responseDone=false;responseDeadlineLocked();update(VoicePhase.THINKING,"Waiting for Gemini's response.")}
            }
            null -> if(gate.active) {
                preRoll.clear()
                if(connection?.audio(pcm)!=true)reconnectLocked("Audio send buffer is full or connection lost.")
            }
        }
    }
    suspend fun interrupt()=lock.withLock { safely { interruptLocked() } }
    private suspend fun interruptLocked() {
        if(!active || !ready || muted)return
        val wasActive=gate.active
        cancelOther(responseJob)
        audio.interrupt();awaitingInput=false;suppress=!responseDone;gate.forceStart();preRoll.clear();input=""
        if(!wasActive && connection?.activity(true)!=true)reconnectLocked("Interruption could not be sent.")
        else update(VoicePhase.LISTENING,"Playback cleared. Speak now; response cancellation is coordinated with Gemini.")
    }
    suspend fun mute(value: Boolean)=lock.withLock { safely { muteLocked(value) } }
    private suspend fun muteLocked(value:Boolean) {
        if(!active || !ready || value==muted)return
        muted=value;audio.interrupt()
        if(value) { awaitingInput=true;micGeneration++;audio.stopMic();if(gate.active)connection?.activity(false);connection?.audioEnd();gate.reset();preRoll.clear();update(VoicePhase.MUTED,"Microphone stopped; incoming playback suppressed.") }
        else { startMicLocked(generation);update(VoicePhase.LISTENING,"Microphone resumed.") }
    }
    private suspend fun reconnectLocked(reason: String, immediate: Boolean=false) {
        if(!active)return
        if(!immediate && retries>=3) { failLocked("$reason Retry limit reached; restart explicitly.");return }
        generation++;ready=false;connection?.close();connection=null;cancelOther(setupJob);cancelOther(retryJob);cancelOther(responseJob)
        withContext(NonCancellable) { audio.stopMic();audio.interrupt() }
        gate.reset();preRoll.clear()
        if(!immediate)retries++
        update(VoicePhase.RECONNECTING,reason+" Microphone paused; no recorded audio is replayed.")
        val g=generation
        retryJob=scope.launch { if(!immediate)delay(1000L shl (retries-1));lock.withLock { if(active && g==generation) {
            try { connectLocked(true) }catch(_:Exception){ failLocked("Reconnection could not start. Check network/model settings.") }
        } } }
    }
    private suspend fun failLocked(detail: String) { stopLocked();mutableState.value=mutableState.value.copy(phase=VoicePhase.ERROR,detail=detail) }
    private suspend fun stopLocked() {
        active=false;ready=false;generation++;lifetime++;cancelOther(retryJob);cancelOther(setupJob);cancelOther(responseJob);retryJob=null;setupJob=null;responseJob=null
        connection?.close();connection=null;key="";handle=null;gate.reset();preRoll.clear()
        withContext(NonCancellable) { audio.close() }
        mutableState.value=VoiceState()
    }
    suspend fun stop()=lock.withLock { stopLocked() }
}
