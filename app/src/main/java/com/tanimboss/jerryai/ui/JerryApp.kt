// Copyright (c) 2026 Tanim Boss. See LICENSE.
package com.tanimboss.jerryai.ui

import android.content.ActivityNotFoundException
import android.Manifest
import android.content.pm.PackageManager
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.tanimboss.jerryai.voice.VoicePhase
import android.content.Intent
import androidx.core.net.toUri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tanimboss.jerryai.actions.ActionsViewModel
import com.tanimboss.jerryai.automation.AutomationConnection
import com.tanimboss.jerryai.voice.VoiceViewModel
import com.tanimboss.jerryai.BuildConfig
import com.tanimboss.jerryai.JerryViewModel
import com.tanimboss.jerryai.catalog.FeatureCatalog
import com.tanimboss.jerryai.catalog.ImplementationState
import com.tanimboss.jerryai.diagnostics.readPermissionDiagnostics
import com.tanimboss.jerryai.settings.ThemeMode

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFCE77),
    onPrimary = Color(0xFF23091D),
    secondary = Color(0xFFFF3499),
    background = Color(0xFF0E0413),
    onBackground = Color.White,
    surface = Color(0xFF2B1029),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF451C38),
    onSurfaceVariant = Color(0xFFEFD9E8),
    outline = Color(0xFFBD5787),
)

private enum class Page(val title: String) { HOME("Home"), VOICE("Voice"), CHAT("Chat"), MEMORY("Memory"), ACTIONS("Actions"), FEATURES("Features"), SETTINGS("Settings"), DIAGNOSTICS("Diagnostics"), ABOUT("About") }

@Composable
fun JerryApp(viewModel: JerryViewModel) {
    val actionsViewModel = viewModel<ActionsViewModel>()
    val voiceViewModel = viewModel<VoiceViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val voiceState by voiceViewModel.state.collectAsStateWithLifecycle()
    val voicePreferences by voiceViewModel.preferences.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selected by rememberSaveable { mutableStateOf(Page.HOME.name) }
    var navigationError by remember { mutableStateOf<String?>(null) }
    val page = Page.entries.firstOrNull { it.name == selected } ?: Page.HOME

    val microphone = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) voiceViewModel.start()
        else {
            voiceViewModel.message("Microphone permission denied. No audio was captured.")
            selected = Page.VOICE.name
        }
    }
    val documents = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
                context.startActivity(Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, mime)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                navigationError = null
            } catch (_: ActivityNotFoundException) {
                navigationError = "No suitable document viewer is installed."
                selected = Page.FEATURES.name
            } catch (_: SecurityException) {
                navigationError = "Android blocked opening the selected document."
                selected = Page.FEATURES.name
            }
        }
    }
    val navigate: (String) -> Unit = { destination ->
        when (destination) {
            "DOCS" -> documents.launch(arrayOf("application/pdf", "text/plain", "image/*"))
            "CAMERA" -> {
                try {
                    context.startActivity(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
                    navigationError = null
                } catch (_: ActivityNotFoundException) {
                    navigationError = "No camera application is available."
                    selected = Page.DIAGNOSTICS.name
                } catch (_: SecurityException) {
                    navigationError = "Android blocked opening the camera."
                    selected = Page.DIAGNOSTICS.name
                }
            }
            else -> Page.entries.firstOrNull { it.name == destination }?.let { selected = it.name }
        }
    }
    val openNotifications: () -> Unit = {
        try {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            )
            navigationError = null
        } catch (_: ActivityNotFoundException) {
            navigationError = "This Android device has no supported notification settings screen."
            selected = Page.DIAGNOSTICS.name
        } catch (_: SecurityException) {
            navigationError = "Android denied notification settings."
            selected = Page.DIAGNOSTICS.name
        }
    }
    val startFromMic: () -> Unit = {
        if (voiceState.phase !in listOf(VoicePhase.IDLE, VoicePhase.ERROR)) {
            selected = Page.VOICE.name
        } else if (!voicePreferences.keyPresent || !voicePreferences.config.valid()) {
            selected = Page.VOICE.name
        } else if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            voiceViewModel.start()
        } else {
            microphone.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    MaterialTheme(colorScheme = DarkColors) {
        when (page) {
            Page.HOME -> JerryNeonHomeScreen(
                voicePhase = voiceState.phase, keyConfigured = voicePreferences.keyPresent,
                onOpen = navigate, onMic = startFromMic,
            )
            Page.SETTINGS -> JerryNeonSettingsScreen(
                theme = state.settings.theme,
                voicePhase = voiceState.phase,
                keyConfigured = voicePreferences.keyPresent,
                language = com.tanimboss.jerryai.voice.VoiceConfig.languages[voicePreferences.config.language] ?: "English",
                onThemeChange = viewModel::setTheme,
                onOpen = navigate,
                onNotifications = openNotifications,
            )
            else -> JerryNeonPageShell(title = page.title, current = page.name, onOpen = navigate) {
                navigationError?.let { JerryNeonInfoCard("Android action", it) }
                state.error?.let { JerryNeonInfoCard("Settings error", it) }
                when (page) {
                    Page.CHAT -> JerryNeonInfoCard(
                        "AI Chat is not ready yet",
                        "The Chat and conversation history implementation belongs to Phase 3. Voice chat is available on the separate Voice screen, subject to your key and provider access.",
                        onClick = { selected = Page.VOICE.name },
                    )
                    Page.MEMORY -> {
                        JerryNeonInfoCard("Memory Bank • planned",
                            "Room memory and backup/restore are not implemented. No existing personal memory or settings were deleted. Do not mistake this design for a functioning backup.")
                        JerryNeonInfoCard("Privacy", "Your Gemini key stays encrypted in Android Keystore, outside backups. Gemini voice audio goes to the selected provider only after you start a session.")
                    }
                    Page.VOICE -> VoiceScreen(voiceViewModel)
                    Page.ACTIONS -> ActionsScreen(actionsViewModel)
                    Page.FEATURES -> Features()
                    Page.DIAGNOSTICS -> Diagnostics()
                    Page.ABOUT -> About()
                    Page.HOME, Page.SETTINGS -> Unit
                }
            }
        }
    }
}

@Composable
private fun InformationCard(title: String, body: String) {
    JerryNeonInfoCard(title, body)
}

@Composable
private fun Home() {
    Text("Your assistant, built independently.", style = MaterialTheme.typography.headlineMedium)
    InformationCard("Foundation build", "Explore settings, diagnostics and Actions. Supported commands use Android intents first and optional, confirmed accessibility automation. Android device verification is still pending.")
    InformationCard("Foreground voice", "Configure your own key and a supported model on Voice. Gemini audio requires real provider access and phone testing. Wake word and background listening remain unavailable. Device actions still require the separate confirmation flow.")
    val features = FeatureCatalog.features
    val partial = features.count { it.implementation == ImplementationState.PARTIAL }
    val planned = features.count { it.implementation == ImplementationState.PLANNED }
    val implemented = features.count { it.implementation == ImplementationState.IMPLEMENTED_DEVICE_UNVERIFIED }
    Text("${features.size} initial feature groups · $implemented implemented/device-unverified · $partial partial · $planned planned", style = MaterialTheme.typography.bodyMedium)
    Text("Android device verification is pending. Automated build evidence is recorded in the repository validation report.")
}

@Composable
private fun Features() {
    Text("Feature inventory", style = MaterialTheme.typography.headlineSmall)
    Text("Initial owner-approved baseline. Historical completeness is unverified. Partial implementation does not mean the full feature works.")
    FeatureCatalog.features.forEach { feature ->
        InformationCard(
            "${feature.id}. ${feature.title}",
            "${feature.implementation.label} · Phase ${feature.phase}\n${feature.limitation}\nDevice evidence: ${feature.deviceEvidence ?: "Not tested"}",
        )
    }
}

@Composable
private fun Diagnostics() {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var revision by remember { mutableIntStateOf(0) }
    var settingsError by remember { mutableStateOf<String?>(null) }
    // Refresh after returning from Android settings, not just after navigation.
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) revision++ }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val permissions = remember(revision) { readPermissionDiagnostics(context) }
    fun openSettings(intent: Intent) {
        try { context.startActivity(intent); settingsError = null }
        catch (_: ActivityNotFoundException) { settingsError = "This device does not provide that settings screen." }
        catch (_: SecurityException) { settingsError = "Android blocked access to that settings screen." }
    }
    Text("Permission diagnostics", style = MaterialTheme.typography.headlineSmall)
    Text("Android ${Build.VERSION.RELEASE} · API ${Build.VERSION.SDK_INT}\nApp ${BuildConfig.VERSION_NAME}")
    permissions.forEach { InformationCard(it.title, "${it.state.label}\n${it.reason}") }
    val serviceConnected by AutomationConnection.connected.collectAsStateWithLifecycle()
    val automationAllowed by AutomationConnection.allowed.collectAsStateWithLifecycle()
    InformationCard("Accessibility service", "Declared, user-enabled only. Connected: $serviceConnected. Session automation allowed: $automationAllowed. Configure it on the Actions screen.")
    InformationCard("Screen capture", "Unavailable. Future capture requires Android consent; no screenshots are taken in this build.")
    InformationCard("Background listening", "Unavailable. No microphone service is registered or running in this build.")
    Button(onClick = { revision++ }) { Text("Refresh diagnostics") }
    OutlinedButton(onClick = { openSettings(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())) }) {
        Text("Open app settings")
    }
    OutlinedButton(onClick = { openSettings(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) { Text("Open accessibility settings") }
    settingsError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
}

@Composable
private fun About() {
    Text("Original contributions", style = MaterialTheme.typography.headlineSmall)
    Text("App name: Jerry AI Original\nOriginal developer: Tanim Boss\nOwner: Tanim Boss\nPublisher: Tanim Boss")
    InformationCard("Privacy in this build", "Theme and voice preferences are saved locally. Your own Gemini API key is encrypted using Android Keystore and excluded from backups. Commands and accessible screen text are used for confirmed actions, not logged as chat history. Android speech input and third-party apps have their own policies. The foreground Gemini Live session sends audio to Google only after Start and microphone permission. No chat or Memory Bank is implemented yet. Jerry has no contacts or direct-call permission. Android app backups and device transfer are disabled.")
    InformationCard("TANIM BOSS EXCLUSIVE OWNERSHIP LICENSE", "Original materials: Copyright © 2026 Tanim Boss. All rights reserved. Copying, modification, redistribution, or commercial use requires separate authorization from Tanim Boss. Third-party components remain under their own licenses. See LICENSE and THIRD_PARTY_NOTICES.md in the source distribution.")
    InformationCard("Third-party software", "AndroidX and Material components: Apache License 2.0. Kotlin: Apache License 2.0. kotlinx.coroutines: Apache License 2.0. Full notices and license texts are included in the source distribution; no third-party ownership is claimed.")
    var showLicenses by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    OutlinedButton(onClick = { showLicenses = !showLicenses }) {
        Text(if (showLicenses) "Hide licenses" else "Read full licenses")
    }
    if (showLicenses) {
        val licenses = remember {
            (listOf("LICENSE", "THIRD_PARTY_NOTICES.md") + context.assets.list("licenses").orEmpty().sorted().map { "licenses/$it" })
                .joinToString("\n\n") { path -> context.assets.open(path).bufferedReader().use { it.readText() } }
        }
        Text(licenses, style = MaterialTheme.typography.bodySmall)
    }
    Text("Version ${BuildConfig.VERSION_NAME} · Personal sideload development build")
}
