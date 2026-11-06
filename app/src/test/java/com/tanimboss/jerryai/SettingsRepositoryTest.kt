// Copyright (c) 2026 Tanim Boss. See LICENSE.
package com.tanimboss.jerryai

import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.tanimboss.jerryai.settings.SettingsRepository
import com.tanimboss.jerryai.settings.ThemeMode
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsRepositoryTest {
    @get:Rule val temporary = TemporaryFolder()

    private fun store(file: File, scope: CoroutineScope) = PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        scope = scope,
        produceFile = { file },
    )

    @Test fun freshInstallUsesSystemTheme() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val repository = SettingsRepository(store(File(temporary.root, "fresh.preferences_pb"), scope))
            assertEquals(ThemeMode.SYSTEM, repository.settings.first().theme)
        } finally { scope.coroutineContext[Job]!!.cancelAndJoin() }
    }

    @Test fun selectedThemeSurvivesStorageReopen() = runBlocking {
        val file = File(temporary.root, "persist.preferences_pb")
        val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            SettingsRepository(store(file, firstScope)).setTheme(ThemeMode.DARK)
        } finally { firstScope.coroutineContext[Job]!!.cancelAndJoin() }
        val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            assertEquals(ThemeMode.DARK, SettingsRepository(store(file, secondScope)).settings.first().theme)
        } finally { secondScope.coroutineContext[Job]!!.cancelAndJoin() }
    }

    @Test fun unknownStoredThemeFallsBackWithoutDeletingOtherPreferences() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val store = store(File(temporary.root, "unknown.preferences_pb"), scope)
            store.edit { it[stringPreferencesKey("theme")] = "FUTURE_THEME"; it[stringPreferencesKey("other")] = "keep" }
            assertEquals(ThemeMode.SYSTEM, SettingsRepository(store).settings.first().theme)
            assertEquals("keep", store.data.first()[stringPreferencesKey("other")])
        } finally { scope.coroutineContext[Job]!!.cancelAndJoin() }
    }

    @Test fun corruptPreferencesRecoverAndAcceptNewSettings() = runBlocking {
        val file = File(temporary.root, "corrupt.preferences_pb").apply { writeBytes(byteArrayOf(-1, -1, -1)) }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val repository = SettingsRepository(store(file, scope))
            assertEquals(ThemeMode.SYSTEM, repository.settings.first().theme)
            repository.setTheme(ThemeMode.LIGHT)
            assertEquals(ThemeMode.LIGHT, repository.settings.first().theme)
        } finally { scope.coroutineContext[Job]!!.cancelAndJoin() }
    }
}
