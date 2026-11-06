// Copyright (c) 2026 Tanim Boss. See LICENSE.
package com.tanimboss.jerryai.voice

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class VoiceSettingsState(val config: VoiceConfig=VoiceConfig(), val keyPresent: Boolean=false, val models: List<String> = emptyList(), val message: String="Choose a Live model; availability and free quota are provider-controlled.", val busy: Boolean=false)
class VoiceViewModel(application: Application): AndroidViewModel(application) {
    private val settings=VoiceSettings(application)
    private val vault=VoiceKeyVault(application)
    private val ownerScope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private val mutableSettings=MutableStateFlow(VoiceSettingsState())
    val preferences=mutableSettings.asStateFlow()
    private val session=LiveSession(ownerScope,GeminiLiveClient(),AndroidVoiceAudio(application)) { persona ->
        viewModelScope.launch { save(mutableSettings.value.config.copy(persona=persona)) }
    }
    val state=session.state
    private var starting=false
    private var startRequest=0L
    init {
        viewModelScope.launch {
            settings.flow.catch { mutableSettings.update { it.copy(message="Voice settings could not be read; existing data was not deleted.") } }
                .collect { config -> mutableSettings.update { it.copy(config=config,keyPresent=vault.present()) } }
        }
    }
    fun message(value:String){mutableSettings.update { it.copy(message=value) }}
    fun save(config:VoiceConfig){viewModelScope.launch { try {settings.save(config);message("Voice preferences saved. Model, language and voice changes apply on the next Start.")}catch(_:java.io.IOException){message("Voice settings could not be saved. Existing preferences were retained.")} }}
    fun saveKey(value:String) {
        if(mutableSettings.value.busy)return
        startRequest++;starting=false
        mutableSettings.update { it.copy(busy=true) }
        viewModelScope.launch {
            try { session.stop();withContext(Dispatchers.IO){vault.save(value.trim())};mutableSettings.update { it.copy(keyPresent=true,message="Key encrypted in Android Keystore-backed local storage. It is not included in backups.") } }
            catch(cancelled:CancellationException){throw cancelled}
            catch(_:Exception){message("The key could not be saved securely. Check device Keystore and storage.")}
            finally{mutableSettings.update { it.copy(busy=false) }}
        }
    }
    fun clearKey(){
        if(mutableSettings.value.busy)return
        startRequest++;starting=false;mutableSettings.update{it.copy(busy=true)}
        ownerScope.launch {
            try{session.stop();withContext(Dispatchers.IO){vault.clear()};mutableSettings.update{it.copy(keyPresent=false,message="Saved API key removed.")}}
            catch(cancelled:CancellationException){throw cancelled}
            catch(_:Exception){message("Could not remove saved key.")}
            finally{mutableSettings.update{it.copy(busy=false)}}
        }
    }
    fun start(){
        if(starting || mutableSettings.value.busy || state.value.phase !in listOf(VoicePhase.IDLE,VoicePhase.ERROR))return
        starting=true
        val request=++startRequest
        ownerScope.launch {
            try {
                val key=withContext(Dispatchers.IO){vault.load()}
                if(request!=startRequest)return@launch
                if(key.isNullOrBlank())message("Save your own Gemini API key first.")else session.start(key,mutableSettings.value.config)
            }catch(cancelled:CancellationException){throw cancelled}
            catch(_:Exception){message("Saved key could not be opened. No microphone or connection was started.")}
            finally{if(request==startRequest)starting=false}
        }
    }
    fun stop(){startRequest++;starting=false;ownerScope.launch { session.stop() }}
    fun interrupt(){ownerScope.launch{session.interrupt()}}
    fun mute(value:Boolean){ownerScope.launch{session.mute(value)}}
    fun discover(){
        if(mutableSettings.value.busy)return
        mutableSettings.update{it.copy(busy=true)}
        viewModelScope.launch {
            try {
                val models=withContext(Dispatchers.IO){val key=vault.load();check(!key.isNullOrBlank());GeminiModels().list(key)}
                mutableSettings.update { it.copy(models=models,message=if(models.isEmpty())"No Live-capable models were listed. Check your provider documentation/account, or enter a supported model ID." else "Live-capable model IDs returned by your provider. Actual voice/language support still needs a connection test.") }
            }catch(cancelled:CancellationException){throw cancelled}
            catch(_:Exception){message("Model discovery failed. Check saved key, network, account access and quota; no model availability is assumed.")}
            finally{mutableSettings.update{it.copy(busy=false)}}
        }
    }
    override fun onCleared(){startRequest++;ownerScope.launch {session.stop();ownerScope.cancel()};super.onCleared()}
}
