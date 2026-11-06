// Copyright (c) 2026 Tanim Boss. See LICENSE.
package com.tanimboss.jerryai

import android.app.Application
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tanimboss.jerryai.settings.AppSettings
import com.tanimboss.jerryai.settings.SettingsRepository
import com.tanimboss.jerryai.settings.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val Application.settingsStore by preferencesDataStore(
    name = "settings",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

data class SettingsUiState(val settings: AppSettings = AppSettings(), val loading: Boolean = true, val error: String? = null)

class JerryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application.settingsStore)
    private val mutableState = MutableStateFlow(SettingsUiState())
    val state = mutableState.asStateFlow()
    init {
        viewModelScope.launch {
            repository.settings.catch {
                mutableState.update { it.copy(loading = false, error = "Settings could not be read. Restart the app to retry.") }
            }.collect { settings ->
                mutableState.update { it.copy(settings = settings, loading = false, error = null) }
            }
        }
    }
    fun setTheme(theme: ThemeMode) {
        viewModelScope.launch {
            try { repository.setTheme(theme) }
            catch (_: java.io.IOException) {
                mutableState.update { it.copy(error = "The theme could not be saved. Check device storage and try again.") }
            }
        }
    }
}
