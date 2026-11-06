// Copyright (c) 2026 Tanim Boss. See LICENSE.
package com.tanimboss.jerryai.voice

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import kotlinx.coroutines.flow.map
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.voiceStore by preferencesDataStore(name="voice_preferences", corruptionHandler=ReplaceFileCorruptionHandler { emptyPreferences() })
class VoiceSettings internal constructor(private val store: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>) {
    constructor(context:Context):this(context.applicationContext.voiceStore)
    private val model=stringPreferencesKey("model"); private val voice=stringPreferencesKey("voice")
    private val language=stringPreferencesKey("language"); private val persona=stringPreferencesKey("persona")
    val flow = store.data.map { p -> VoiceConfig(p[model].orEmpty(), p[voice]?.takeIf { it in VoiceConfig.voices } ?: "Kore",
        p[language]?.takeIf { it in VoiceConfig.languages } ?: "en-US", Persona.entries.firstOrNull { it.name == p[persona] } ?: Persona.ASSISTANT) }
    suspend fun save(config: VoiceConfig) { store.edit { p -> p[model]=config.model.trim(); p[voice]=config.voice; p[language]=config.language; p[persona]=config.persona.name } }
}

/** AES-GCM key material stays in Android Keystore; encrypted credential is excluded from backups. */
class VoiceKeyVault(context: Context, private val alias: String = "jerry.gemini.live.key.v1") {
    private val file = AtomicFile(File(context.noBackupFilesDir, "gemini-live-key.v1"))
    private fun key(create: Boolean = true): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias,null) as? SecretKey)?.let { return it }
        check(create) { "Credential encryption key is unavailable; encrypted data was retained." }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build())
        }.generateKey()
    }
    @Synchronized fun present(): Boolean = file.baseFile.exists()
    @Synchronized fun save(value: String) {
        require(value.isNotBlank() && value.length <= 512 && value.none { it.isISOControl() })
        val cipher=Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE,key()) }
        val encrypted=cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val out=file.startWrite()
        try { out.write(byteArrayOf(1,cipher.iv.size.toByte()));out.write(cipher.iv);out.write(encrypted);file.finishWrite(out) }
        catch (failure: Exception) { file.failWrite(out);throw failure }
    }
    @Synchronized fun load(): String? {
        if (!present()) return null
        require(file.baseFile.length()<=2048)
        val bytes=file.openRead().use { it.readBytes() }
        require(bytes.size in 30..2048 && bytes[0].toInt()==1 && bytes[1].toInt()==12)
        val cipher=Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE,key(false),GCMParameterSpec(128,bytes.copyOfRange(2,14))) }
        return cipher.doFinal(bytes.copyOfRange(14,bytes.size)).toString(Charsets.UTF_8)
    }
    @Synchronized fun clear() { file.delete() }
}
