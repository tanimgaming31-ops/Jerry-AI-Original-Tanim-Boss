// Copyright (c) 2026 Tanim Boss. See LICENSE.
package com.tanimboss.jerryai.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tanimboss.jerryai.voice.*

@Composable private fun Choice(label:String,selected:String,items:List<String>,onSelect:(String)->Unit) {
    var open by remember{mutableStateOf(false)}
    Box {OutlinedButton(onClick={open=true}){Text("$label: $selected")};DropdownMenu(expanded=open,onDismissRequest={open=false}){items.forEach { item -> DropdownMenuItem(text={Text(item)},onClick={onSelect(item);open=false}) }}}
}
@Composable fun VoiceScreen(model:VoiceViewModel) {
    val preferences by model.preferences.collectAsStateWithLifecycle()
    val state by model.state.collectAsStateWithLifecycle()
    val context=LocalContext.current
    var key by remember{mutableStateOf("")}
    var modelId by remember(preferences.config.model){mutableStateOf(preferences.config.model)}
    var clear by remember{mutableStateOf(false)}
    val microphone=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){ granted -> if(granted)model.start()else model.message("Microphone permission denied. No capture started; grant it only when you want a voice session.") }
    Text("Gemini Live voice",style=MaterialTheme.typography.headlineSmall)
    Text("Foreground-only Phase 1. No wake word or background listening. Audio is sent to Google only after you press Start and grant microphone permission. Provider processing, quota and pricing apply; free service is not guaranteed.")
    Text("Session: ${state.phase.name.lowercase()}\n${state.detail}")
    Text("${preferences.message}\nAPI key: ${if(preferences.keyPresent)"saved securely" else "not configured"}")
    OutlinedTextField(value=key,onValueChange={key=it.take(512)},label={Text("Gemini API key")},visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password),modifier=Modifier.fillMaxWidth(),singleLine=true)
    Button(enabled=key.isNotBlank()&&!preferences.busy,onClick={model.saveKey(key);key=""}){Text("Save encrypted key")}
    OutlinedButton(enabled=preferences.keyPresent&&!preferences.busy,onClick={clear=true}){Text("Remove saved key")}
    OutlinedTextField(value=modelId,onValueChange={modelId=it.take(127)},label={Text("Live model ID")},modifier=Modifier.fillMaxWidth(),singleLine=true)
    Button(onClick={model.save(preferences.config.copy(model=modelId.trim()))},enabled=modelId.isNotBlank()){Text("Save model")}
    OutlinedButton(onClick=model::discover,enabled=preferences.keyPresent&&!preferences.busy){Text("Discover available Live models")}
    if(preferences.models.isNotEmpty())Choice("Provider model",preferences.config.model,preferences.models){model.save(preferences.config.copy(model=it))}
    Choice("Voice",preferences.config.voice,VoiceConfig.voices){model.save(preferences.config.copy(voice=it))}
    Choice("Response language",VoiceConfig.languages[preferences.config.language].orEmpty(),VoiceConfig.languages.values.toList()){ name -> model.save(preferences.config.copy(language=VoiceConfig.languages.entries.first{it.value==name}.key)) }
    Choice("Personality",preferences.config.persona.label,Persona.entries.map{it.label}){name ->model.save(preferences.config.copy(persona=Persona.entries.first{it.label==name}))}
    Text("Language is a model-dependent request, not a guarantee of all listed languages. Try ‘Jerry, switch to friendly mode’ or ‘জেরি, বন্ধুত্বপূর্ণ মোড চালু করো’. A recognized final input transcript can change personality and restart the Live context; no device action is executed.")
    Button(enabled=!preferences.busy&&preferences.keyPresent&&preferences.config.valid()&&state.phase in listOf(VoicePhase.IDLE,VoicePhase.ERROR),onClick={
        if(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)model.start()else microphone.launch(Manifest.permission.RECORD_AUDIO)
    }){Text("Start foreground voice")}
    OutlinedButton(onClick=model::interrupt,enabled=state.phase in listOf(VoicePhase.SPEAKING,VoicePhase.THINKING,VoicePhase.LISTENING)){Text("Interrupt / listen")}
    OutlinedButton(onClick={model.mute(state.phase!=VoicePhase.MUTED)},enabled=state.phase in listOf(VoicePhase.MUTED,VoicePhase.LISTENING,VoicePhase.THINKING,VoicePhase.SPEAKING)){Text(if(state.phase==VoicePhase.MUTED)"Unmute microphone" else "Mute microphone")}
    OutlinedButton(onClick=model::stop){Text("Stop voice")}
    Text("Voice Stop: ‘Jerry, stop listening’ / ‘জেরি শোনা বন্ধ করো’ works only when Gemini returns a recognized final input transcript. If offline, always use Stop. Use headphones if speaker echo causes false interruptions. Echo cancellation is enabled only when hardware supports it. Leaving the app stops this session. Audio and captions are not recorded or included in backups.")
    Text("You: ${state.inputCaption.ifEmpty{"—"}}")
    Text("Jerry: ${state.outputCaption.ifEmpty{"—"}}")
    if(clear)AlertDialog(onDismissRequest={clear=false},title={Text("Remove Gemini key?")},text={Text("This stops voice and removes only the saved provider key. Existing settings and other app data are preserved.")},confirmButton={TextButton(onClick={clear=false;model.clearKey()}){Text("Remove key")}},dismissButton={TextButton(onClick={clear=false}){Text("Cancel")}})
}
