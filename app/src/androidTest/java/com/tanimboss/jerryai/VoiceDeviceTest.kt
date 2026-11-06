// Copyright (c) 2026 Tanim Boss. See LICENSE.
package com.tanimboss.jerryai

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tanimboss.jerryai.voice.VoiceKeyVault
import org.junit.*
import org.junit.runner.RunWith
import java.io.File
import java.security.KeyStore
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class VoiceDeviceTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun voiceScreenOpensAndStopsWithoutGrantingMicrophone() {
        compose.onNodeWithText("⚙", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Voice & Wake Word", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Gemini Live voice").assertIsDisplayed()
        // Test never enters a real key, requests microphone, or contacts Gemini.
        compose.onNodeWithText("Stop voice").performScrollTo().performClick()
    }
    @Test fun keystoreCredentialRoundTripAndTamperDetectionAreIsolated() {
        val app=compose.activity.applicationContext
        val id=UUID.randomUUID().toString()
        val dir=File(app.cacheDir,"voice-vault-test-$id").apply{check(mkdirs())}
        val alias="jerry.test.$id"
        val isolated=object:ContextWrapper(app){override fun getNoBackupFilesDir():File=dir}
        val vault=VoiceKeyVault(isolated,alias)
        val dummy="test-only-not-a-provider-key"
        Assert.assertFalse(KeyStore.getInstance("AndroidKeyStore").apply{load(null)}.containsAlias(alias))
        try {
            Assert.assertNull(vault.load());vault.save(dummy);Assert.assertEquals(dummy,vault.load())
            val file=File(dir,"gemini-live-key.v1")
            Assert.assertFalse(file.readBytes().toString(Charsets.ISO_8859_1).contains(dummy))
            file.writeBytes(file.readBytes().also{it[it.lastIndex]=(it.last().toInt() xor 1).toByte()})
            Assert.assertThrows(Exception::class.java){vault.load()}
            Assert.assertTrue(file.exists()) // Failed read must not erase stored data.
            vault.clear();Assert.assertFalse(vault.present())
        }finally{
            dir.deleteRecursively() // Only this newly created isolated test directory.
            KeyStore.getInstance("AndroidKeyStore").apply{load(null);deleteEntry(alias)}
        }
    }
}
