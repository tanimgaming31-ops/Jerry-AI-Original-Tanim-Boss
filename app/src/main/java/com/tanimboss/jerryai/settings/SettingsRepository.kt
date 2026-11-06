// Copyright (c) 2026 Tanim Boss. See LICENSE.
package com.tanimboss.jerryai.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode(val label: String) {
    SYSTEM("System"), LIGHT("Light"), DARK("Dark");
    companion object {
        fun decode(value: String?): ThemeMode = entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

data class AppSettings(val theme: ThemeMode = ThemeMode.SYSTEM)

class SettingsRepository(private val store: DataStore<Preferences>) {
    private val themeKey = stringPreferencesKey("theme")
    val settings: Flow<AppSettings> = store.data.map { AppSettings(ThemeMode.decode(it[themeKey])) }
    suspend fun setTheme(theme: ThemeMode) { store.edit { it[themeKey] = theme.name } }
}
