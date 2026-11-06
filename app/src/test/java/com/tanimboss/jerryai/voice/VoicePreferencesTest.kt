// Copyright (c) 2026 Tanim Boss. See LICENSE.
package com.tanimboss.jerryai.voice

import androidx.datastore.preferences.core.*
import com.tanimboss.jerryai.settings.SettingsRepository
import com.tanimboss.jerryai.settings.ThemeMode
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.rules.TemporaryFolder
import java.io.File

class VoicePreferencesTest {
    @get:Rule val temp=TemporaryFolder()
    private fun store(file:File,scope:CoroutineScope)=PreferenceDataStoreFactory.create(scope=scope,produceFile={file})
    @Test fun voicePreferencesPersistAcrossReopen()=runBlocking {
        val file=File(temp.root,"voice.preferences_pb");val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
        val config=VoiceConfig("test-model","Puck","bn-BD",Persona.FRIENDLY)
        try{VoiceSettings(store(file,scope)).save(config)}finally{scope.coroutineContext[Job]!!.cancelAndJoin()}
        val reopened=CoroutineScope(SupervisorJob()+Dispatchers.IO)
        try{Assert.assertEquals(config,VoiceSettings(store(file,reopened)).flow.first())}finally{reopened.coroutineContext[Job]!!.cancelAndJoin()}
    }
    @Test fun unknownChoicesFallBackWithoutDeletingUnrelatedData()=runBlocking {
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
        try{
            val store=store(File(temp.root,"voice.preferences_pb"),scope)
            store.edit{it[stringPreferencesKey("persona")]="FUTURE";it[stringPreferencesKey("voice")]="future";it[stringPreferencesKey("unrelated")]="keep"}
            val config=VoiceSettings(store).flow.first();Assert.assertEquals(Persona.ASSISTANT,config.persona);Assert.assertEquals("Kore",config.voice)
            Assert.assertEquals("keep",store.data.first()[stringPreferencesKey("unrelated")])
        }finally{scope.coroutineContext[Job]!!.cancelAndJoin()}
    }
    @Test fun savingVoiceDoesNotChangeExistingThemeData()=runBlocking {
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
        try{
            val original=SettingsRepository(store(File(temp.root,"settings.preferences_pb"),scope));original.setTheme(ThemeMode.DARK)
            VoiceSettings(store(File(temp.root,"voice.preferences_pb"),scope)).save(VoiceConfig("test-model"))
            Assert.assertEquals(ThemeMode.DARK,original.settings.first().theme)
        }finally{scope.coroutineContext[Job]!!.cancelAndJoin()}
    }
}
