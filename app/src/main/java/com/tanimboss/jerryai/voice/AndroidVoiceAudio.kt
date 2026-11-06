// Copyright (c) 2026 Tanim Boss. See LICENSE.
package com.tanimboss.jerryai.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.*
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

/** Foreground PCM16 mono audio. No audio is saved or replayed after interruption. */
class AndroidVoiceAudio(private val context: Context) : VoiceAudio {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private val hardware=Any();private val queue=PlaybackQueue()
    private var capture: Job?=null;private var player: Job?=null
    private var track: AudioTrack?=null
    private var focus: AudioFocusRequest?=null;private var previousMode: Int?=null
    private val manager=context.getSystemService(AudioManager::class.java)
    private val pending=AtomicBoolean(false)
    private var written=0L;private var headBase=0L
    override suspend fun open(onFocusLost: () -> Unit, onDrained: () -> Unit) {
        close()
        val attributes=AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
        val request=AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).setAudioAttributes(attributes)
            .setAcceptsDelayedFocusGain(false).setOnAudioFocusChangeListener { if(it<0)onFocusLost() }.build()
        focus=request
        check(manager.requestAudioFocus(request)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        previousMode=manager.mode;manager.mode=AudioManager.MODE_IN_COMMUNICATION
        val min=AudioTrack.getMinBufferSize(24000,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT)
        check(min>0)
        val output=AudioTrack.Builder().setAudioAttributes(attributes).setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(24000).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()).setBufferSizeInBytes(maxOf(min*2,9600)).setTransferMode(AudioTrack.MODE_STREAM).build()
        track=output;check(output.state==AudioTrack.STATE_INITIALIZED);output.play()
        written=0;headBase=output.playbackHeadPosition.toLong() and 0xffffffffL
        player=scope.launch {
            var chunk: PlaybackQueue.Chunk?=null;var offset=0
            try {
                while(isActive) {
                    if(chunk==null) { chunk=queue.poll();offset=0 }
                    val current=chunk
                    if(current!=null) {
                        synchronized(hardware) {
                            if(!queue.current(current.epoch))chunk=null
                            else {
                                val count=output.write(current.bytes,offset,minOf(960,current.bytes.size-offset),AudioTrack.WRITE_NON_BLOCKING)
                                check(count>=0)
                                offset+=count;written+=count/2
                                if(offset==current.bytes.size)chunk=null
                            }
                        }
                    } else {
                        val drained=synchronized(hardware) {
                            val played=((output.playbackHeadPosition.toLong() and 0xffffffffL)-headBase) and 0xffffffffL
                            written>0 && queue.empty() && played>=written && pending.compareAndSet(true,false)
                        }
                        if(drained)onDrained()
                    }
                    delay(5)
                }
            } catch(cancelled:CancellationException){throw cancelled}
            catch(_:RuntimeException){onFocusLost()}
        }
    }
    override suspend fun startMic(onPcm: (ShortArray) -> Unit, onError: () -> Unit) {
        stopMic()
        capture=scope.launch {
            var record: AudioRecord?=null;var echo: AcousticEchoCanceler?=null;var noise: NoiseSuppressor?=null
            try {
                var inputRate=16000
                for(rate in listOf(16000,48000,44100)) {
                    if(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)throw SecurityException()
                    val min=AudioRecord.getMinBufferSize(rate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT)
                    if(min<=0)continue
                    val candidate=try {
                        AudioRecord.Builder().setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                            .setAudioFormat(AudioFormat.Builder().setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_IN_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                            .setBufferSizeInBytes(maxOf(min*2,rate/5)).build()
                    } catch(_:IllegalArgumentException){continue}
                    if(candidate.state==AudioRecord.STATE_INITIALIZED){record=candidate;inputRate=rate;break}else candidate.release()
                }
                val input=checkNotNull(record)
                try { if(AcousticEchoCanceler.isAvailable())echo=AcousticEchoCanceler.create(input.audioSessionId)?.apply { enabled=true } }catch(_:RuntimeException){/* Optional hardware effect. */}
                try { if(NoiseSuppressor.isAvailable())noise=NoiseSuppressor.create(input.audioSessionId)?.apply { enabled=true } }catch(_:RuntimeException){/* Optional hardware effect. */}
                val resampler=PcmResampler(inputRate)
                val frame=ShortArray(inputRate/50)
                input.startRecording();check(input.recordingState==AudioRecord.RECORDSTATE_RECORDING)
                var position=0
                while(isActive) {
                    val count=input.read(frame,position,frame.size-position,AudioRecord.READ_NON_BLOCKING)
                    check(count>=0)
                    position+=count
                    if(position==frame.size) { val pcm=resampler.convert(frame);position=0;if(pcm.isNotEmpty())onPcm(pcm) }
                    delay(5)
                }
            }catch(cancelled:CancellationException){throw cancelled}
            catch(_:RuntimeException){onError()}
            finally {
                try{echo?.release()}catch(_:RuntimeException){/* Release all resources even after one effect fails. */}
                try{noise?.release()}catch(_:RuntimeException){/* Optional effect already released. */}
                record?.let { try { if(it.recordingState==AudioRecord.RECORDSTATE_RECORDING)it.stop() }catch(_:RuntimeException){/* Released by the platform. */}
                    finally{try{it.release()}catch(_:RuntimeException){/* Already released. */}} }
            }
        }
    }
    override suspend fun stopMic() { capture?.cancelAndJoin();capture=null }
    override fun play(pcm: ByteArray): Boolean {
        if(track==null)return false
        val accepted=queue.offer(pcm);if(accepted)pending.set(true);return accepted
    }
    override fun interrupt() {
        synchronized(hardware) {
            queue.clear();pending.set(false);written=0
            track?.let { it.pause();it.flush();it.play();headBase=it.playbackHeadPosition.toLong() and 0xffffffffL }
        }
    }
    override fun busy(): Boolean = pending.get()
    override suspend fun close() {
        stopMic();player?.cancelAndJoin();player=null
        synchronized(hardware) {
            queue.clear();pending.set(false);written=0
            track?.let { try { it.pause();it.flush() }catch(_:RuntimeException){/* Already stopped. */}
                finally{try{it.release()}catch(_:RuntimeException){/* Already released. */}} };track=null
        }
        try {previousMode?.let { manager.mode=it }}catch(_:RuntimeException){/* OS routing changed; do not retain focus. */}finally{previousMode=null}
        try {focus?.let { manager.abandonAudioFocusRequest(it) }}catch(_:RuntimeException){/* Already released by the platform. */}finally{focus=null}
    }
}
