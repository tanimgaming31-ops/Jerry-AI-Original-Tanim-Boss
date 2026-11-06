// Copyright (c) 2026 Tanim Boss. See LICENSE.
package com.tanimboss.jerryai.voice

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LiveSessionTest {
    private class FakeConnection: LiveConnection {
        val activities=mutableListOf<Boolean>();var frames=0;var closed=false;var sends=true
        override fun audio(pcm:ShortArray):Boolean {frames++;return sends}
        override fun activity(start:Boolean):Boolean {activities+=start;return sends}
        override fun audioEnd()=true
        override fun close(){closed=true}
    }
    private class FakeConnector:LiveConnector {
        val listeners=mutableListOf<(LiveEvent)->Unit>();val connections=mutableListOf<FakeConnection>();val handles=mutableListOf<String?>()
        override fun connect(key:String,config:VoiceConfig,resume:String?,onEvent:(LiveEvent)->Unit):LiveConnection {
            assertEquals("test-only-key",key);listeners+=onEvent;handles+=resume
            return FakeConnection().also{connections+=it}
        }
        fun emit(e:LiveEvent,index:Int=listeners.lastIndex){listeners[index](e)}
    }
    private class FakeAudio:VoiceAudio {
        var starts=0;var activeMic=0;var clears=0;var plays=0;var opened=false;var overflow=false;var pending=false;var brokenPlayback=false
        var pcm:((ShortArray)->Unit)?=null;var lost:(()->Unit)?=null
        override suspend fun open(onFocusLost:()->Unit,onDrained:()->Unit){opened=true;lost=onFocusLost}
        override suspend fun startMic(onPcm:(ShortArray)->Unit,onError:()->Unit){check(activeMic==0);activeMic++;starts++;pcm=onPcm}
        override suspend fun stopMic(){activeMic=0;pcm=null}
        override fun play(pcm:ByteArray):Boolean {if(brokenPlayback)throw IllegalStateException("test failure");plays++;pending=true;return !overflow}
        override fun interrupt(){clears++;pending=false}
        override fun busy()=pending
        override suspend fun close(){stopMic();opened=false}
        fun speech(){repeat(3){pcm?.invoke(ShortArray(320){10000})}}
    }
    @Test fun noMicrophoneBeforeSetupAndDuplicateReadyDoesNotDoubleStart()=runTest {
        val c=FakeConnector();val a=FakeAudio();val s=LiveSession(backgroundScope,c,a)
        s.start("test-only-key",VoiceConfig("test-model"));assertEquals(0,a.starts)
        c.emit(LiveEvent.Ready);runCurrent();c.emit(LiveEvent.Ready);runCurrent()
        assertEquals(1,a.starts);assertEquals(VoicePhase.LISTENING,s.state.value.phase);s.stop()
    }
    @Test fun invalidSettingsDoNotConnectOrCapture()=runTest {
        val c=FakeConnector();val a=FakeAudio();val s=LiveSession(backgroundScope,c,a)
        s.start("",VoiceConfig());assertTrue(c.connections.isEmpty());assertEquals(0,a.starts);assertEquals(VoicePhase.ERROR,s.state.value.phase)
    }
    @Test fun voiceBargeInFlushesAndSendsActivityBeforeAudio()=runTest {
        val c=FakeConnector();val a=FakeAudio();val s=LiveSession(backgroundScope,c,a)
        s.start("test-only-key",VoiceConfig("test-model"));c.emit(LiveEvent.Ready);runCurrent()
        c.emit(LiveEvent.Audio(byteArrayOf(0,0)));runCurrent();assertEquals(VoicePhase.SPEAKING,s.state.value.phase)
        a.speech();runCurrent();assertTrue(a.clears>0);assertEquals(listOf(true),c.connections.last().activities);assertTrue(c.connections.last().frames>0)
        c.emit(LiveEvent.Audio(byteArrayOf(1,0)));runCurrent();assertEquals(1,a.plays)
        c.emit(LiveEvent.Interrupted);runCurrent();assertEquals(VoicePhase.LISTENING,s.state.value.phase);s.stop()
    }
    @Test fun muteReleasesMicrophoneAndSuppressesPlayback()=runTest {
        val c=FakeConnector();val a=FakeAudio();val s=LiveSession(backgroundScope,c,a)
        s.start("test-only-key",VoiceConfig("test-model"));c.emit(LiveEvent.Ready);runCurrent()
        s.mute(true);assertEquals(0,a.activeMic);assertEquals(VoicePhase.MUTED,s.state.value.phase)
        c.emit(LiveEvent.Audio(byteArrayOf(0,0)));runCurrent();assertEquals(0,a.plays)
        s.mute(false);assertEquals(1,a.activeMic);s.stop()
    }
    @Test fun stopCancelsRetriesAndStaleSocketEvents()=runTest {
        val c=FakeConnector();val a=FakeAudio();val s=LiveSession(backgroundScope,c,a)
        s.start("test-only-key",VoiceConfig("test-model"));c.emit(LiveEvent.Ready);runCurrent()
        c.emit(LiveEvent.Failure(true,"network"));runCurrent();s.stop()
        advanceTimeBy(10000);runCurrent();c.emit(LiveEvent.Ready,0);c.emit(LiveEvent.Audio(byteArrayOf(0,0)),0);runCurrent()
        assertEquals(1,c.connections.size);assertEquals(0,a.activeMic);assertEquals(0,a.plays);assertEquals(VoicePhase.IDLE,s.state.value.phase)
    }
    @Test fun authenticationFailureNeverRetries()=runTest {
        val c=FakeConnector();val a=FakeAudio();val s=LiveSession(backgroundScope,c,a)
        s.start("test-only-key",VoiceConfig("test-model"));c.emit(LiveEvent.Failure(false,"denied"));runCurrent()
        advanceTimeBy(20000);runCurrent();assertEquals(1,c.connections.size);assertEquals(VoicePhase.ERROR,s.state.value.phase)
    }
    @Test fun reconnectIsBoundedAndUsesResumptionWithoutReplayingAudio()=runTest {
        val c=FakeConnector();val a=FakeAudio();val s=LiveSession(backgroundScope,c,a)
        s.start("test-only-key",VoiceConfig("test-model"));c.emit(LiveEvent.Ready);c.emit(LiveEvent.ResumeHandle("resume-only-memory"));runCurrent()
        repeat(3){i -> c.emit(LiveEvent.Failure(true,"network"));runCurrent();assertEquals(0,a.activeMic);advanceTimeBy(1000L shl i);runCurrent();c.emit(LiveEvent.Ready);runCurrent()}
        assertEquals(4,c.connections.size);assertEquals("resume-only-memory",c.handles.last());assertTrue(c.connections.all{it.frames==0})
        c.emit(LiveEvent.Failure(true,"network"));runCurrent();assertEquals(VoicePhase.ERROR,s.state.value.phase);assertEquals(0,a.activeMic)
    }
    @Test fun setupTimeoutStopsOrReconnectsInsteadOfLeavingMicRunning()=runTest {
        val c=FakeConnector();val a=FakeAudio();val s=LiveSession(backgroundScope,c,a)
        s.start("test-only-key",VoiceConfig("test-model"));advanceTimeBy(15000);runCurrent();assertEquals(0,a.starts)
        assertEquals(VoicePhase.RECONNECTING,s.state.value.phase);s.stop();advanceTimeBy(10000);runCurrent();assertEquals(1,c.connections.size)
    }
    @Test fun playbackOverflowFailsSafely()=runTest {
        val c=FakeConnector();val a=FakeAudio();val s=LiveSession(backgroundScope,c,a)
        s.start("test-only-key",VoiceConfig("test-model"));c.emit(LiveEvent.Ready);runCurrent();a.overflow=true
        c.emit(LiveEvent.Audio(byteArrayOf(0,0)));runCurrent();assertEquals(VoicePhase.ERROR,s.state.value.phase);assertEquals(0,a.activeMic)
    }
    @Test fun inputTranscriptCanSwitchPersonaWithoutDeviceActions()=runTest {
        val c=FakeConnector();val a=FakeAudio();val modes=mutableListOf<Persona>();val s=LiveSession(backgroundScope,c,a,{modes+=it})
        s.start("test-only-key",VoiceConfig("test-model"));c.emit(LiveEvent.Ready);runCurrent()
        c.emit(LiveEvent.InputText("Jerry, switch to friendly mode",true));runCurrent()
        assertEquals(listOf(Persona.FRIENDLY),modes);assertEquals(2,c.connections.size);assertNull(c.handles.last());s.stop()
    }
    @Test fun interruptAfterServerFinishedDoesNotDiscardTheNextReply()=runTest {
        val c=FakeConnector();val a=FakeAudio();val s=LiveSession(backgroundScope,c,a)
        s.start("test-only-key",VoiceConfig("test-model"));c.emit(LiveEvent.Ready);runCurrent()
        c.emit(LiveEvent.Audio(byteArrayOf(0,0)));c.emit(LiveEvent.TurnComplete);runCurrent()
        assertEquals(VoicePhase.SPEAKING,s.state.value.phase)
        a.speech();runCurrent()
        repeat(30){a.pcm?.invoke(ShortArray(320));runCurrent()}
        c.emit(LiveEvent.Audio(byteArrayOf(1,0)));runCurrent();assertEquals(2,a.plays);s.stop()
    }
    @Test fun oldFocusCallbackCannotStopANewSession()=runTest {
        val c=FakeConnector();val a=FakeAudio();val s=LiveSession(backgroundScope,c,a)
        s.start("test-only-key",VoiceConfig("test-model"));val old=a.lost!!
        s.stop();s.start("test-only-key",VoiceConfig("test-model"));c.emit(LiveEvent.Ready);runCurrent()
        old();runCurrent();assertEquals(VoicePhase.LISTENING,s.state.value.phase)
        a.lost!!();runCurrent();assertEquals(VoicePhase.ERROR,s.state.value.phase);assertEquals(0,a.activeMic)
    }
    @Test fun unmuteDoesNotPlayAudioFromTheMutedTurn()=runTest {
        val c=FakeConnector();val a=FakeAudio();val s=LiveSession(backgroundScope,c,a)
        s.start("test-only-key",VoiceConfig("test-model"));c.emit(LiveEvent.Ready);runCurrent()
        s.mute(true);s.mute(false);c.emit(LiveEvent.Audio(byteArrayOf(0,0)));runCurrent();assertEquals(0,a.plays);s.stop()
    }
    @Test fun bengaliVoiceStopReleasesMicrophoneAndConnection()=runTest {
        val c=FakeConnector();val a=FakeAudio();val s=LiveSession(backgroundScope,c,a)
        s.start("test-only-key",VoiceConfig("test-model"));c.emit(LiveEvent.Ready);runCurrent()
        c.emit(LiveEvent.InputText("জেরি শোনা বন্ধ করো",true));runCurrent()
        assertEquals(VoicePhase.IDLE,s.state.value.phase);assertEquals(0,a.activeMic);assertTrue(c.connections.last().closed)
    }
    @Test fun microphoneBackpressureStopsInsteadOfGrowingUnbounded()=runTest {
        val c=FakeConnector();val a=FakeAudio();val s=LiveSession(backgroundScope,c,a)
        s.start("test-only-key",VoiceConfig("test-model"));c.emit(LiveEvent.Ready);runCurrent()
        repeat(20){a.pcm?.invoke(ShortArray(320))};runCurrent()
        assertEquals(VoicePhase.ERROR,s.state.value.phase);assertEquals(0,a.activeMic)
    }

    @Test fun playbackExceptionsReleaseAudioInsteadOfKillingTheEventLoopSilently()=runTest {
        val c=FakeConnector();val a=FakeAudio();val s=LiveSession(backgroundScope,c,a)
        s.start("test-only-key",VoiceConfig("test-model"));c.emit(LiveEvent.Ready);runCurrent();a.brokenPlayback=true
        c.emit(LiveEvent.Audio(byteArrayOf(0,0)));runCurrent();assertEquals(VoicePhase.ERROR,s.state.value.phase);assertEquals(0,a.activeMic)
    }
    @Test fun interruptDoesNotSendDuplicateActivityStarts()=runTest {
        val c=FakeConnector();val a=FakeAudio();val s=LiveSession(backgroundScope,c,a)
        s.start("test-only-key",VoiceConfig("test-model"));c.emit(LiveEvent.Ready);runCurrent()
        s.interrupt();s.interrupt();assertEquals(listOf(true),c.connections.last().activities);s.stop()
    }
    @Test fun oldMicrophoneFramesAreIgnoredAfterMuteAndUnmute()=runTest {
        val c=FakeConnector();val a=FakeAudio();val s=LiveSession(backgroundScope,c,a)
        s.start("test-only-key",VoiceConfig("test-model"));c.emit(LiveEvent.Ready);runCurrent();val old=a.pcm!!
        s.mute(true);s.mute(false);repeat(3){old(ShortArray(320){10000})};runCurrent()
        assertTrue(c.connections.last().activities.isEmpty());assertEquals(0,c.connections.last().frames);s.stop()
    }

    @Test fun optionalTranscriptFinishedCanBeFinalizedAtTurnComplete()=runTest {
        val c=FakeConnector();val a=FakeAudio();val modes=mutableListOf<Persona>();val s=LiveSession(backgroundScope,c,a,{modes+=it})
        s.start("test-only-key",VoiceConfig("test-model"));c.emit(LiveEvent.Ready);runCurrent()
        c.emit(LiveEvent.InputText("Jerry, switch to friendly mode",false));runCurrent();assertTrue(modes.isEmpty())
        c.emit(LiveEvent.TurnComplete);runCurrent();assertEquals(listOf(Persona.FRIENDLY),modes);s.stop()
    }

    @Test fun incomingEventOverflowStopsAndReleasesCapture()=runTest {
        val c=FakeConnector();val a=FakeAudio();val s=LiveSession(backgroundScope,c,a)
        s.start("test-only-key",VoiceConfig("test-model"));c.emit(LiveEvent.Ready);runCurrent()
        repeat(40){c.emit(LiveEvent.OutputText("caption"))};runCurrent()
        assertEquals(VoicePhase.ERROR,s.state.value.phase);assertEquals(0,a.activeMic)
    }

    @Test fun providerThatNeverFinishesAResponseTimesOutAndReleasesAudio()=runTest {
        val c=FakeConnector();val a=FakeAudio();val s=LiveSession(backgroundScope,c,a)
        s.start("test-only-key",VoiceConfig("test-model"));c.emit(LiveEvent.Ready);runCurrent()
        a.speech();runCurrent();repeat(30){a.pcm?.invoke(ShortArray(320));runCurrent()}
        assertEquals(VoicePhase.THINKING,s.state.value.phase)
        advanceTimeBy(45_000);runCurrent();assertEquals(VoicePhase.ERROR,s.state.value.phase);assertEquals(0,a.activeMic)
    }

}
